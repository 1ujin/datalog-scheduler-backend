package com.cesi.datalogscheduler.component;

import com.cesi.datalogscheduler.entity.BackupInfo;
import com.cesi.datalogscheduler.enums.SystemEnum;
import com.cesi.datalogscheduler.mapper.BackupInfoMapper;
import com.cesi.datalogscheduler.util.SftpUtil;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.processing.SupportedAnnotationTypes;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@Qualifier("aaa")
@RequiredArgsConstructor
public class Backupper {
    private static final AtomicBoolean isBackup = new AtomicBoolean(true);

    /**
     * 连接池
     */
    private final Map<Integer, Object> connectionMap = new ConcurrentHashMap<>();

    private final Map<Integer, SystemEnum> systemMap = new ConcurrentHashMap<>();

    /**
     * 锁池
     */
    // private final Map<Integer, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final BackupInfoMapper backupInfoMapper;

    public static void openBackup() {
        isBackup.compareAndSet(false, true);
        log.info("开启定时备份");
    }

    public static void closeBackup() {
        isBackup.compareAndSet(true, false);
        log.info("关闭定时备份");
    }

    public static boolean backupStatus() {
        return isBackup.get();
    }

    public void backUp(List<Path> backupFileList, String computerName) {
        if (!isBackup.get()) {
            return;
        }
        log.info("开始备份");
        List<BackupInfo> infos = backupInfoMapper.findAll();
        if (infos == null || infos.size() < 1) {
            return;
        }
        for (BackupInfo info : infos) {
            if (!info.isSync()) {
                continue;
            }
            log.info(info.getHost());
            // Integer id = info.getId();
            // if (!lockMap.containsKey(id)) {
            //     lockMap.put(id, new ReentrantLock());
            // }
            // ReentrantLock lock = lockMap.get(id);
            if (SystemEnum.LINUX == SystemEnum.ofName(info.getSystem())) {
                // 远程连接
                SftpUtil sftp = new SftpUtil();
                sftp.connect(info.getUsername(), info.getHost(), info.getPort(), info.getPassword());
                if (!sftp.getChannel().isConnected()) {
                    continue;
                }
                // 是否为空路径
                var ls = sftp.ls(info.getPrefixBackupPath() + "/" + computerName);
                if (ls == null || ls.size() <= 0) {
                    if (!sftp.createDir(info.getPrefixBackupPath() + "/" + computerName)) {
                        sftp.disconnect();
                        continue;
                    }
                }
                for (Path backup : backupFileList) {
                    String src = backup.toString().replace("\\", "/");
                    String dst = Path.of(info.getPrefixBackupPath(), computerName, backup.getFileName().toString()).toString().replace("\\", "/");
                    if (!sftp.upload(src, dst)) {
                        log.error("上传备份失败: From " + src + " To " + dst);
                    }
                }
                // 断开连接
                sftp.disconnect();
            } else if (SystemEnum.WINDOWS == SystemEnum.ofName(info.getSystem())) {
                Path share = Path.of("/", info.getHost(), info.getPrefixBackupPath());
                if (Files.notExists(share)) {
                    log.warn("备份根目录不存在: " + share);
                    try {
                        Files.createDirectories(share);
                        log.info("备份根目录创建成功: " + share);
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("创建备份根目录失败", e);
                    }
                }
                if (Files.exists(share)) {
                    for (Path src : backupFileList) {
                        Path dst = Path.of(String.valueOf(share), computerName, String.valueOf(src.getFileName()));
                        Path parent = dst.getParent();
                        if (Files.notExists(parent)) {
                            try {
                                Files.createDirectories(parent);
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("创建目录失败: " + parent.toString().replace("\\", "/"), e);
                                continue;
                            }
                        }
                        try {
                            Files.copy(src, dst);
                        } catch (IOException e) {
                            e.printStackTrace();
                            log.error("上传备份失败: From " + src.toString().replace("\\", "/") + " To " + dst.toString().replace("\\", "/"), e);
                        }
                    }
                } else {
                    AuthenticationContext ac = new AuthenticationContext(info.getUsername(), info.getPassword().toCharArray(), "");
                    try (
                            SMBClient client = new SMBClient();
                            Connection connection = client.connect(info.getHost());
                            Session session = connection.authenticate(ac);
                            DiskShare diskShare = (DiskShare) session.connectShare(info.getPrefixBackupPath())) {
                        for (Path src : backupFileList) {
                            Path dst = Path.of(info.getPrefixBackupPath(), computerName, src.getFileName().toString());
                            Path parent = dst.getParent();
                            if (!diskShare.folderExists(parent.toString())) {
                                mkdirs(diskShare, parent);
                            }
                            try (
                                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src.toString()));
                                    BufferedOutputStream bos = new BufferedOutputStream(diskShare.openFile(dst.toString(), EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null).getOutputStream())) {
                                byte[] b = new byte[0x400];
                                while (bis.read(b) != -1) {
                                    bos.write(b);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                log.error("上传备份失败: From " + src.toString().replace("\\", "/") + " To " + dst.toString().replace("\\", "/"), e);
                            }
                        }
                    } catch (IOException | SMBApiException e) {
                        e.printStackTrace();
                        log.error("访问远程路径失败：", e);
                    }
                }
            } else {
                log.error("不能识别的备份系统名称: " + info.getSystem());
            }
        }
        log.info("结束备份");
        for (Path del : backupFileList) {
            try {
                Files.deleteIfExists(del);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("删除临时压缩文件失败", e);
            }
        }
    }

    public static void mkdirs(DiskShare diskShare, Path path) throws SMBApiException {
        if (!diskShare.folderExists(path.toString())) {
            mkdirs(diskShare, path.getParent());
            diskShare.mkdir(path.toString());
        }
    }

    public void shutdown() {
        for (Map.Entry<Integer, Object> entry : connectionMap.entrySet()) {
            Integer connectionId = entry.getKey();
            SystemEnum system = systemMap.get(connectionId);
            if (SystemEnum.LINUX == system) {
                if (entry.getValue() != null) {
                    ((SftpUtil) entry.getValue()).disconnect();
                }
            } else if (SystemEnum.WINDOWS == system) {
                if (entry.getValue() != null) {
                    Session session = (Session) entry.getValue();
                    try (SMBClient ignored = session.getConnection().getClient()) {
                        session.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("连接关闭失败，备份id: " + connectionId + "]");
                    }
                }
            }
        }
    }

    public void shutdown(long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("停止备份中断异常", e);
            }
        }).start();
    }
}
