package com.cesi.datalogscheduler.component;

import com.cesi.datalogscheduler.DatalogSchedulerApplication;
import com.cesi.datalogscheduler.entity.ConnectionInfo;
import com.cesi.datalogscheduler.entity.ResultInfo;
import com.cesi.datalogscheduler.enums.SystemEnum;
import com.cesi.datalogscheduler.mapper.ConnectionInfoMapper;
import com.cesi.datalogscheduler.mapper.ResultInfoMapper;
import com.cesi.datalogscheduler.util.SftpUtil;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import jcifs.smb1.smb1.NtlmPasswordAuthentication;
import jcifs.smb1.smb1.SmbException;
import jcifs.smb1.smb1.SmbFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {
    private static final AtomicBoolean isSync = new AtomicBoolean(false);

    /**
     * 文件冲突保存本地路径
     */
    @Value("${conflict.local-path}")
    private String conflictLocalPath;

    /**
     * 文件冲突保存挂载路径
     */
    @Value("${conflict.volume-path}")
    private String conflictVolumePath;

    /**
     * 任务超时时长
     */
    @Value("${schedule.task-timeout:60}")
    private long taskTimeout;

    /**
     * 压缩级别
     */
    @Value("${schedule.compression-level:-1}")
    private int compressionLevel;

    /**
     * 分卷大小默认1GB
     */
    @Value("${schedule.zip-split-size:0x40000000}")
    private long zipSplitSize;

    private final Statistics statistics;

    private final ConnectionInfoMapper connectionInfoMapper;

    private final ResultInfoMapper resultInfoMapper;

    private final Backupper backupper;

    private final String projectName = DatalogSchedulerApplication.class.getName();

    /**
     * 迁移并分析
     */
    public void sync() {
        if (!isSync.get()) {
            return;
        }
        log.info("开始同步");
        // 形如 /home/demo/2910_DATA/202301/laolianhou/20230322/20230323_25C_XXX/1234.txt
        List<ConnectionInfo> connections = connectionInfoMapper.findAll();
        if (connections == null) {
            return;
        }
        ExecutorService exec = Executors.newFixedThreadPool(8);
        for (ConnectionInfo connect : connections) {
            if (!connect.isSync()) {
                continue;
            }
            // 多线程同步文件
            exec.execute(() -> {
                log.info("机台: " + connect.getComputerName());
                if (connect.getPrefixVolumePath() == null) {
                    connect.setPrefixVolumePath(connect.getPrefixLocalPath());
                }
                long beginTime = System.currentTimeMillis();
                List<ResultInfo> dataToInsert = new ArrayList<>();
                String dateName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
                Path tmpdir = null;
                if (Backupper.backupStatus()) {
                    try {
                        tmpdir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), projectName));
                        // tmpdir = Files.createTempDirectory(projectName + "_");
                        tmpdir.toFile().deleteOnExit();
                        log.info("临时目录: " + tmpdir);
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("创建临时目录失败", e);
                    }
                }
                File zipFile = null;
                if (tmpdir != null) {
                    try {
                        zipFile = Files.createTempFile(tmpdir, connect.getComputerName() + "_" + dateName + "_", ".zip").toFile();
                        zipFile.deleteOnExit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (SystemEnum.ADV93000 == SystemEnum.ofName(connect.getSystem())) {
                    // 远程连接
                    SftpUtil sftp = new SftpUtil();
                    sftp.connect(connect.getUsername(), connect.getHost(), connect.getPort(), connect.getPassword());
                    if (!sftp.getChannel().isConnected()) {
                        return;
                    }
                    // 是否为空路径
                    var ls = sftp.ls(connect.getPrefixRemotePath());
                    if (ls == null || ls.size() <= 0) {
                        sftp.disconnect();
                        return;
                    }
                    int removedFailure = 0;
                    String duplicate = null;
                    String localPrefix = connect.getPrefixLocalPath() + "/" + connect.getComputerName();
                    String volumePrefix = connect.getPrefixVolumePath() + "/" + connect.getComputerName();
                    // 递归获取路径下所有文件
                    List<String> filenameList = sftp.lsRecursively(connect.getPrefixRemotePath());
                    log.info("本次同步文件数量: " + filenameList.size());
                    int count = 0, step = filenameList.size() > 1000 ? 1000 : 100;
                    // 缓冲数组
                    byte[] buff = new byte[0x400];
                    // 压缩输出流
                    ZipArchiveOutputStream zos = null;
                    if (zipFile != null) {
                        try {
                            // 1GB分卷
                            zos = new ZipArchiveOutputStream(zipFile, zipSplitSize);
                            // 压缩级别
                            zos.setLevel(compressionLevel);
                        } catch (IOException e) {
                            e.printStackTrace();
                            log.error("创建压缩输出流失败", e);
                        }
                    }
                    for (String filename : filenameList) {
                        filename = filename.replace("\\", "/");
                        Path filePath = Path.of(filename);
                        String filePathSuffix = filePath.subpath(Path.of(connect.getPrefixRemotePath()).getNameCount(), filePath.getNameCount()).toString();
                        Path tarPath = Path.of(localPrefix, filePathSuffix);
                        Path volPath = Path.of(volumePrefix, filePathSuffix);
                        // 复制
                        if (Files.exists(tarPath)) {
                            duplicate = tarPath.toString().replace("\\", "/");
                            log.warn("文件重复: From " + filename + " To " + duplicate);
                            localPrefix = conflictLocalPath + "/" + dateName + "/" + connect.getComputerName();
                            volumePrefix = conflictVolumePath + "/" + dateName + "/" + connect.getComputerName();
                            tarPath = Path.of(localPrefix, filePathSuffix);
                            volPath = Path.of(volumePrefix, filePathSuffix);
                        }
                        String tarFilename = tarPath.toString().replace("\\", "/");
                        String volFilename = volPath.toString().replace("\\", "/");
                        if (!sftp.download(filename, tarFilename)) {
                            removedFailure++;
                            log.error("下载失败: From " + filename + " To " + tarFilename);
                        } else {
                            // 分析
                            ResultInfo resultInfo = statistics.parse(tarFilename, duplicate, localPrefix, SystemEnum.ofName(connect.getSystem()), connect.getComputerName());
                            if (resultInfo != null) {
                                resultInfo.setPath(volFilename);
                                resultInfo.setDuplicatePath(duplicate);
                                dataToInsert.add(resultInfo);
                            }
                            // 添加到压缩文件
                            // tarFilename -> file -> FileInputStream -> byte[] -> ZipArchiveOutputStream -> ByteArrayOutputStream -> zipFile
                            if (zos != null) {
                                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tarFilename))) {
                                    // 创建压缩项目
                                    zos.putArchiveEntry(new ZipArchiveEntry(filePathSuffix));
                                    // 循环写入内容
                                    while (bis.read(buff) != -1) {
                                        zos.write(buff);
                                    }
                                    // 关闭项目
                                    zos.closeArchiveEntry();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    log.error("添加压缩项目失败", e);
                                }
                            }
                        }
                        count++;
                        if (count % step == 0) {
                            log.info("" + count + " / " + filenameList.size());
                        }
                    }
                    if (zos != null) {
                        try {
                            zos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            log.error("关闭压缩输出流失败", e);
                        }
                        try {
                            List<Path> backupFileList = Files.list(tmpdir).filter(p -> p.toFile().isFile() && String.valueOf(p.getFileName()).startsWith(connect.getComputerName() + "_" + dateName)).collect(Collectors.toList());
                            backupper.backUp(backupFileList, connect.getComputerName());
                        } catch (IOException e) {
                            e.printStackTrace();
                            log.error("遍历压缩文件失败", e);
                        }
                    }
                    // 删除
                    if (removedFailure == 0 && !filenameList.isEmpty()) {
                        log.info("删除路径下所有内容");
                        sftp.rmdirRecursively(connect.getPrefixRemotePath());
                        sftp.createDir(connect.getPrefixRemotePath());
                    }
                    // 断开连接
                    sftp.disconnect();
                    log.info("迁移失败数量: " + removedFailure + ", 耗时: " + (System.currentTimeMillis() - beginTime) + "(ms)");
                } else if (SystemEnum.J750 == SystemEnum.ofName(connect.getSystem())
                        || SystemEnum.ULTRA_FLEX == SystemEnum.ofName(connect.getSystem())) {
                    int removedFailure = 0;
                    String localPrefix = connect.getPrefixLocalPath() + "/" + connect.getComputerName();
                    String volumePrefix = connect.getPrefixVolumePath() + "/" + connect.getComputerName();
                    List<Path> filePathList;
                    Stream<Path> paths = null;
                    Path share = Path.of("/", connect.getHost(), connect.getPrefixRemotePath());
                    try {
                        paths = Files.walk(share);
                        filePathList = paths.filter(p -> !p.equals(share)).filter(Files::isRegularFile).collect(Collectors.toList());
                        log.info("本次同步文件数量: " + filePathList.size());
                        int count = 0, step = filePathList.size() > 1000 ? 1000 : 100;
                        // 缓冲数组
                        byte[] buff = new byte[0x400];
                        // 压缩输出流
                        ZipArchiveOutputStream zos = null;
                        if (zipFile != null) {
                            try {
                                // 1GB分卷
                                zos = new ZipArchiveOutputStream(zipFile, zipSplitSize);
                                // 压缩级别
                                zos.setLevel(compressionLevel);
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("创建压缩输出流失败", e);
                            }
                        }
                        for (Path srcPath : filePathList) {
                            String filePathSuffix = srcPath.subpath(share.getNameCount(), srcPath.getNameCount()).toString();
                            Path tarPath = Path.of(localPrefix, filePathSuffix);
                            Path volPath = Path.of(volumePrefix, filePathSuffix);
                            // 复制
                            String duplicate = null;
                            if (Files.exists(tarPath)) {
                                log.warn("文件重复: From " + srcPath.toString().replace("\\", "/") + " To " + tarPath.toString().replace("\\", "/"));
                                duplicate = volPath.toString().replace("\\", "/");
                                localPrefix = conflictLocalPath + "/" + dateName + "/" + connect.getComputerName();
                                volumePrefix = conflictVolumePath + "/" + dateName + "/" + connect.getComputerName();
                                tarPath = Path.of(localPrefix, filePathSuffix);
                                volPath = Path.of(volumePrefix, filePathSuffix);
                            }
                            try {
                                if (Files.notExists(tarPath.getParent())) {
                                    Files.createDirectories(tarPath.getParent());
                                }
                                Files.copy(srcPath, tarPath);
                                // 分析
                                ResultInfo resultInfo = statistics.parse(tarPath.toString().replace("\\", "/"), duplicate, localPrefix, SystemEnum.ofName(connect.getSystem()), connect.getComputerName());
                                if (resultInfo != null) {
                                    resultInfo.setPath(volPath.toString().replace("\\", "/"));
                                    resultInfo.setDuplicatePath(duplicate);
                                    dataToInsert.add(resultInfo);
                                }
                                // 添加到压缩文件
                                if (zos != null) {
                                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(String.valueOf(tarPath)))) {
                                        // 创建压缩项目
                                        zos.putArchiveEntry(new ZipArchiveEntry(filePathSuffix));
                                        // 循环写入内容
                                        while (bis.read(buff) != -1) {
                                            zos.write(buff);
                                        }
                                        // 关闭项目
                                        zos.closeArchiveEntry();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        log.error("添加压缩项目失败", e);
                                    }
                                }
                            } catch (IOException e) {
                                removedFailure++;
                                log.error("下载失败: From " + srcPath.toString().replace("\\", "/") + " To " + tarPath.toString().replace("\\", "/"), e);
                            }
                            count++;
                            if (count % step == 0) {
                                log.info("" + count + " / " + filePathList.size());
                            }
                        }
                        if (zos != null) {
                            try {
                                zos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("关闭压缩输出流失败", e);
                            }
                            List<Path> backupFileList = Files.list(tmpdir).filter(p -> p.toFile().isFile() && String.valueOf(p.getFileName()).startsWith(connect.getComputerName() + "_" + dateName)).collect(Collectors.toList());
                            backupper.backUp(backupFileList, connect.getComputerName());
                        }
                        if (removedFailure == 0 && !filePathList.isEmpty()) {
                            log.info("删除路径下所有内容: " + share);
                            paths = Files.walk(share);
                            paths.filter(p -> !p.equals(share)).sorted(Comparator.reverseOrder()).forEach(path -> {
                                try {
                                    if (!Files.deleteIfExists(path)) {
                                        log.error("删除路径失败: " + path.toString().replace("\\", "/"));
                                    }
                                } catch (IOException e) {
                                    log.error("删除路径失败: " + path.toString().replace("\\", "/"), e);
                                }
                            });
                            if (!Files.exists(share)) {
                                Files.createDirectories(share);
                            }
                        }
                    } catch (IOException e) {
                        log.error("遍历远程路径失败: " + connect.getPrefixRemotePath().replace("\\", "/"), e);
                    } finally {
                        if (paths != null) {
                            paths.close();
                        }
                    }
                    // SMB
                    if (paths == null) {
                        removedFailure = syncByHierynomus(connect, dataToInsert, dateName);
                    }
                    log.info("迁移失败数量: " + removedFailure + ", 耗时: " + (System.currentTimeMillis() - beginTime) + "(ms)");
                } else {
                    log.error("不能识别的测试系统名称: " + connect.getSystem());
                }
                resultBatchInsert(dataToInsert);
                dataToInsert.clear();
            });
        }
        // 任务完成后关闭线程池
        exec.shutdown();
        try {
            // 主线程中断60分钟
            if (exec.awaitTermination(taskTimeout, TimeUnit.MINUTES)) {
                log.info("结束同步");
            } else {
                log.error("超时，强制结束所有同步，未完成同步任务数量: " + exec.shutdownNow().size());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("主线程中断失败", e);
        } finally {
            exec.shutdownNow();
        }
    }

    @SuppressWarnings("unused")
    private int syncByHierynomus(ConnectionInfo info, List<ResultInfo> dataToInsert) {
        return syncByHierynomus(info, dataToInsert, null);
    }

    @SuppressWarnings("unused")
    private int syncByHierynomus(ConnectionInfo info, List<ResultInfo> dataToInsert, String dateName) {
        if (dateName == null) {
            dateName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        Path tmpdir = null;
        if (Backupper.backupStatus()) {
            try {
                tmpdir = Files.createTempDirectory(projectName + "_");
                tmpdir.toFile().deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("创建临时目录失败", e);
            }
        }
        File zipFile = null;
        if (tmpdir != null) {
            try {
                zipFile = Files.createTempFile(tmpdir, info.getComputerName() + "_" + dateName + "_", ".zip").toFile();
                zipFile.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int removedFailure = 0;
        String localPrefix = info.getPrefixLocalPath() + "/" + info.getComputerName();
        String volumePrefix = info.getPrefixVolumePath() + "/" + info.getComputerName();
        AuthenticationContext ac = new AuthenticationContext(info.getUsername(), info.getPassword().toCharArray(), "");
        try (
                SMBClient client = new SMBClient();
                Connection connection = client.connect(info.getHost());
                Session session = connection.authenticate(ac)) {
            String shareFolder = Path.of(info.getPrefixRemotePath()).getName(0).toString();
            String subFolder = ".";
            if (!shareFolder.equals(info.getPrefixRemotePath().replace("/", ""))) {
                subFolder = "/" + Path.of(info.getPrefixRemotePath()).subpath(1, Path.of(info.getPrefixRemotePath()).getNameCount());
            }
            try (DiskShare diskShare = (DiskShare) session.connectShare(shareFolder)) {
                List<String> filenameList = new ArrayList<>();
                if (diskShare.folderExists(subFolder)) {
                    for (FileIdBothDirectoryInformation f : diskShare.list(subFolder)) {
                        if (".".equals(f.getFileName()) || "..".equals(f.getFileName())) {
                            continue;
                        }
                        if (FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue() == (f.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue())) {
                            SyncScheduler.fillFilenameListRecursively(subFolder + "/" + f.getFileName(), filenameList, diskShare);
                        }
                    }
                }
                log.info("本次同步文件数量: " + filenameList.size());
                int count = 0, step = filenameList.size() > 1000 ? 1000 : 100;
                // 压缩输出流
                ZipArchiveOutputStream zos = null;
                if (zipFile != null) {
                    try {
                        // 1GB分卷
                        zos = new ZipArchiveOutputStream(zipFile, zipSplitSize);
                        // 压缩级别
                        zos.setLevel(compressionLevel);
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("创建压缩输出流失败", e);
                    }
                }
                for (String srcFilename : filenameList) {
                    Path srcPath = Path.of(srcFilename);
                    String filePathSuffix = srcPath.subpath(Path.of(subFolder).getNameCount(), srcPath.getNameCount()).toString();
                    Path tarPath = Path.of(localPrefix, filePathSuffix);
                    Path volPath = Path.of(volumePrefix, filePathSuffix);
                    // 复制
                    String duplicate = null;
                    if (Files.exists(tarPath)) {
                        log.warn("文件重复: From " + srcPath.toString().replace("\\", "/") + " To " + tarPath.toString().replace("\\", "/"));
                        duplicate = volPath.toString().replace("\\", "/");
                        localPrefix = conflictLocalPath + "/" + dateName + "/" + info.getComputerName();
                        volumePrefix = conflictVolumePath + "/" + dateName + "/" + info.getComputerName();
                        tarPath = Path.of(localPrefix, filePathSuffix);
                        volPath = Path.of(volumePrefix, filePathSuffix);
                    }
                    String tarFilename = tarPath.toString().replace("\\", "/");
                    String volFilename = volPath.toString().replace("\\", "/");
                    if (!Files.exists(tarPath.getParent())) {
                        Files.createDirectories(tarPath.getParent());
                    }
                    try (
                            BufferedInputStream bis1 = new BufferedInputStream(diskShare.openFile(srcFilename, EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null).getInputStream())) {
                        // 缓冲数组
                        byte[] b = new byte[0x400];
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tarFilename))) {
                            while (bis1.read(b) != -1) {
                                bos.write(b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            log.error("写入文件失败", e);
                        }
                        // 分析
                        ResultInfo resultInfo = statistics.parse(tarFilename, duplicate, localPrefix, SystemEnum.ofName(info.getSystem()), info.getComputerName());
                        if (resultInfo != null) {
                            resultInfo.setPath(volFilename);
                            resultInfo.setDuplicatePath(duplicate);
                            dataToInsert.add(resultInfo);
                        }
                        // 添加到压缩文件
                        if (zos != null) {
                            try (BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(tarFilename))) {
                                // 创建压缩项目
                                zos.putArchiveEntry(new ZipArchiveEntry(filePathSuffix));
                                // 循环写入内容
                                while (bis2.read(b) != -1) {
                                    zos.write(b);
                                }
                                // 关闭项目
                                zos.closeArchiveEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("添加压缩项目失败", e);
                            }
                        }
                    } catch (IOException e) {
                        removedFailure++;
                        log.error("下载失败: From " + srcFilename + " To " + tarFilename, e);
                    }
                    count++;
                    if (count % step == 0) {
                        log.info("" + count + " / " + filenameList.size());
                    }
                }
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("关闭压缩输出流失败", e);
                    }
                    String finalDateName = dateName;
                    List<Path> backupFileList = Files.list(tmpdir).filter(p -> p.toFile().isFile() && String.valueOf(p.getFileName()).startsWith(info.getComputerName() + "_" + finalDateName)).collect(Collectors.toList());
                    backupper.backUp(backupFileList, info.getComputerName());
                }
                if (removedFailure == 0 && !filenameList.isEmpty()) {
                    log.info("删除路径下所有内容: " + subFolder);
                    for (FileIdBothDirectoryInformation f : diskShare.list(subFolder)) {
                        if (".".equals(f.getFileName()) || "..".equals(f.getFileName())) {
                            continue;
                        }
                        removeFileRecursively(subFolder + "/" + f.getFileName(), diskShare);
                        try {
                            diskShare.rmdir(subFolder + "/" + f.getFileName() + "/", true);
                        } catch (SMBApiException e) {
                            log.error("删除路径失败: [" + subFolder + "/" + f.getFileName() + "]", e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("访问远程路径失败：", e);
        }
        return removedFailure;
    }

    @SuppressWarnings("unused")
    private int syncByJcifs(ConnectionInfo info, List<ResultInfo> dataToInsert) {
        return syncByJcifs(info, dataToInsert, null);
    }

    @SuppressWarnings("unused")
    private int syncByJcifs(ConnectionInfo info, List<ResultInfo> dataToInsert, String dateName) {
        if (dateName == null) {
            dateName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        Path tmpdir = null;
        if (Backupper.backupStatus()) {
            try {
                tmpdir = Files.createTempDirectory(projectName + "_");
                tmpdir.toFile().deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("创建临时目录失败", e);
            }
        }
        File zipFile = null;
        if (tmpdir != null) {
            try {
                zipFile = Files.createTempFile(tmpdir, info.getComputerName() + "_" + dateName + "_", ".zip").toFile();
                zipFile.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int removedFailure = 0;
        String localPrefix = info.getPrefixLocalPath() + "/" + info.getComputerName();
        String volumePrefix = info.getPrefixVolumePath() + "/" + info.getComputerName();
        Path share = Path.of("/", info.getHost(), info.getPrefixRemotePath());
        try {
            // 创建远程文件对象
            // smb://用户名:密码/共享的路径/...
            // smb://ip地址/共享的路径/...
            String remoteUrl = "smb://" + info.getHost() + "/" + info.getPrefixRemotePath() + "/";
            SmbFile remoteFile = new SmbFile(remoteUrl, new NtlmPasswordAuthentication(null, info.getUsername(), info.getPassword()));
            // 尝试连接
            remoteFile.connect();
            if (remoteFile.exists()) {
                // 递归获取共享文件夹中文件列表
                SmbFile[] smbFiles = remoteFile.listFiles();
                List<SmbFile> smbFileList = new ArrayList<>();
                for (SmbFile smbFile : smbFiles) {
                    fillSmbFileListRecursively(smbFile, smbFileList);
                }
                log.info("本次同步文件数量: " + smbFileList.size());
                int count = 0, step = smbFileList.size() > 1000 ? 1000 : 100;
                // 缓冲数组
                byte[] buff = new byte[0x400];
                // 压缩输出流
                ZipArchiveOutputStream zos = null;
                if (zipFile != null) {
                    try {
                        // 1GB分卷
                        zos = new ZipArchiveOutputStream(zipFile, zipSplitSize);
                        // 压缩级别
                        zos.setLevel(compressionLevel);
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("创建压缩输出流失败", e);
                    }
                }
                for (SmbFile smbFile : smbFileList) {
                    Path srcPath = Path.of(smbFile.getCanonicalPath());
                    String filePathSuffix = srcPath.subpath(share.getNameCount(), srcPath.getNameCount()).toString();
                    Path tarPath= Path.of(localPrefix, filePathSuffix);
                    Path volPath= Path.of(volumePrefix, filePathSuffix);
                    // 复制
                    String duplicate = null;
                    if (Files.exists(tarPath)) {
                        log.warn("文件重复: From " + srcPath.toString().replace("\\", "/") + " To " + tarPath.toString().replace("\\", "/"));
                        duplicate = volPath.toString().replace("\\", "/");
                        localPrefix = conflictLocalPath + "/" + dateName + "/" + info.getComputerName();
                        volumePrefix = conflictVolumePath + "/" + dateName + "/" + info.getComputerName();
                        tarPath = Path.of(localPrefix, filePathSuffix);
                        volPath = Path.of(volumePrefix, filePathSuffix);
                    }
                    String tarFilename = tarPath.toString().replace("\\", "/");
                    String volFilename = volPath.toString().replace("\\", "/");
                    try {
                        if (!Files.exists(tarPath.getParent())) {
                            Files.createDirectories(tarPath.getParent());
                        }
                        smbFile.copyTo(new SmbFile("file://" + tarFilename));
                        // 分析
                        ResultInfo resultInfo = statistics.parse(tarFilename, duplicate, localPrefix, SystemEnum.ofName(info.getSystem()), info.getComputerName());
                        if (resultInfo != null) {
                            resultInfo.setPath(volFilename);
                            resultInfo.setDuplicatePath(duplicate);
                            dataToInsert.add(resultInfo);
                        }
                        // 添加到压缩文件
                        if (zos != null) {
                            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tarFilename))) {
                                // 创建压缩项目
                                zos.putArchiveEntry(new ZipArchiveEntry(filePathSuffix));
                                // 循环写入内容
                                while (bis.read(buff) != -1) {
                                    zos.write(buff);
                                }
                                // 关闭项目
                                zos.closeArchiveEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("添加压缩项目失败", e);
                            }
                        }
                    } catch (SmbException e) {
                        removedFailure++;
                        log.error("下载失败: From " + srcPath.toString().replace("\\", "/") + " To " + tarPath.toString().replace("\\", "/"), e);
                    }
                    count++;
                    if (count % step == 0) {
                        log.info("" + count + " / " + smbFileList.size());
                    }
                }
                if (zos != null) {
                    try {
                        String finalDateName = dateName;
                        List<Path> backupFileList = Files.list(tmpdir).filter(p -> p.toFile().isFile() && String.valueOf(p.getFileName()).startsWith(info.getComputerName() + "_" + finalDateName)).collect(Collectors.toList());
                        backupper.backUp(backupFileList, info.getComputerName());
                        zos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("关闭压缩输出流失败", e);
                    }
                }
                if (removedFailure == 0 && !smbFileList.isEmpty()) {
                    log.info("删除路径下所有内容: " + remoteFile);
                    for (SmbFile delFile : remoteFile.listFiles()) {
                        try {
                            delFile.delete();
                        } catch (SmbException e) {
                            log.error("删除路径失败", e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("访问远程路径失败：", e);
        }
        return removedFailure;
    }

    public static void fillSmbFileListRecursively(SmbFile smbFile, List<SmbFile> list) {
        try {
            if (smbFile.isFile()) {
                System.out.println(list.size());
                list.add(smbFile);
            } else {
                for (SmbFile child : smbFile.listFiles()) {
                    fillSmbFileListRecursively(child, list);
                }
            }
        } catch (SmbException e) {
            log.error("访问远程路径失败: " + smbFile.getCanonicalPath(), e);
        }
    }

    public static void fillFilenameListRecursively(String path, List<String> list, DiskShare share) {
        for (FileIdBothDirectoryInformation f : share.list(path)) {
            if (".".equals(f.getFileName()) || "..".equals(f.getFileName())) {
                continue;
            }
            String nextPath = path + "/" + f.getFileName();
            if (FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue() == (f.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue())) {
                list.add(nextPath);
            } else if (FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue() == (f.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue())) {
                fillFilenameListRecursively(nextPath, list, share);
            }
        }
    }

    public static void removeFileRecursively(String path, DiskShare share) {
        for (FileIdBothDirectoryInformation f : share.list(path)) {
            if (".".equals(f.getFileName()) || "..".equals(f.getFileName())) {
                continue;
            }
            String nextPath = path + "/" + f.getFileName();
            if (FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue() == (f.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue())) {
                try {
                    share.rm(nextPath);
                } catch (SMBApiException e) {
                    log.error("删除路径失败: [" + nextPath + "]", e);
                }
            } else if (FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue() == (f.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue())) {
                removeFileRecursively(nextPath, share);
            }
        }
    }

    private void resultBatchInsert(List<ResultInfo> dataToInsert) {
        try {
            long beginTime = System.currentTimeMillis();
            // int rows = batchUtils.batchUpdateOrInsert(dataToInsert, ResultInfoMapper.class, (resultInfo, resultInfoMapper) -> resultInfoMapper.insert(resultInfo));
            int rows = 0;
            List<ResultInfo> tmp = new ArrayList<>();
            Map<ResultInfo, ResultInfo> map = new HashMap<>();
            for (ResultInfo resultInfo : dataToInsert) {
                // 每次遍历都与数据库中已有的数据对比检查有无重复
                // List<ResultInfo> duplicate = resultInfoMapper.findDuplicate(resultInfo);
                // if (!duplicate.isEmpty()) {
                //     resultInfo.setDuplicatePath(duplicate.get(0).getPath());
                // }

                // 本次批处理所有插入的数据相互之间有无重复
                if (map.containsKey(resultInfo)) {
                    resultInfo.setDuplicatePath(map.get(resultInfo).getPath());
                } else {
                    map.put(resultInfo, resultInfo);
                }
                tmp.add(resultInfo);

                if (tmp.size() >= 50) {
                    // 每遍历50个检查
                    for (ResultInfo duplicate : resultInfoMapper.multiFindDuplicate(tmp)) {
                        // 与数据库中已有的数据对比检查有无重复
                        for (ResultInfo tmpItem : tmp) {
                            if (Objects.equals(duplicate, tmpItem)) {
                                tmpItem.setDuplicatePath(duplicate.getPath());
                            }
                        }
                    }
                    // 插入数据库
                    rows += resultInfoMapper.multiInsert(tmp);
                    tmp.clear();
                    map.clear();
                }
            }
            if (!tmp.isEmpty()) {
                for (ResultInfo duplicate : resultInfoMapper.multiFindDuplicate(tmp)) {
                    // 与数据库中已有的数据对比检查有无重复
                    for (ResultInfo tmpItem : tmp) {
                        if (Objects.equals(duplicate, tmpItem)) {
                            tmpItem.setDuplicatePath(duplicate.getPath());
                        }
                    }
                }
                rows += resultInfoMapper.multiInsert(tmp);
            }
            // for (int i = 0, j; i < dataToInsert.size(); i += j) {
            //     tmp.clear();
            //     for (j = 0; i + j < dataToInsert.size() && j < 50; j++) {
            //         tmp.add(dataToInsert.get(i + j));
            //     }
            //     rows += resultInfoMapper.multiInsert(tmp);
            // }
            log.info("写入数据库数量: " + rows + ", 耗时: " + (System.currentTimeMillis() - beginTime) + "(ms)");
        } catch (Exception e) {
            log.error("写入数据库失败", e);
        }
    }

    public static void openSync() {
        isSync.compareAndSet(false, true);
        log.info("开启定时同步");
    }

    public static void closeSync() {
        isSync.compareAndSet(true, false);
        log.info("关闭定时同步");
    }

    public static boolean syncStatus() {
        return isSync.get();
    }

    @PostConstruct
    private void postConstructor() {
        log.info("文件冲突保存本地路径: " + conflictLocalPath);
        log.info("文件冲突保存挂载路径: " + conflictVolumePath);
        log.info("任务超时时长: " + taskTimeout + "(min)");
        log.info("压缩级别: " + compressionLevel);
        log.info("压缩文件分卷大小: " + zipSplitSize + "(B)");
    }
}
