package com.cesi.datalogscheduler.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;

@Slf4j
@SuppressWarnings("unused")
public class SftpUtil {
    private final ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<ChannelSftp> channelThreadLocal = new ThreadLocal<>();

    public ChannelSftp getChannel() {
        return channelThreadLocal.get();
    }

    /**
     * 连接服务器
     *
     * @param username 用户名
     * @param host     服务器IP
     * @param port     端口，默认22
     * @param password 密码
     * @return 连接结果
     */
    public boolean connect(String username, String host, int port, String password) {
        JSch jsch = new JSch();
        boolean successed = false;
        if (channelThreadLocal.get() != null) {
            log.info("通道不为空");
            return false;
        }
        try {
            // 根据用户名，主机ip和端口获取一个Session对象
            Session session = jsch.getSession(username, host, port);
            sessionThreadLocal.set(session);
            // 设置密码
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", "password");
            session.setConfig("StrictHostKeyChecking", "no");
            // 设置超时
            session.setTimeout(60000);
            // 通过Session建立连接
            session.connect();
            log.info("JSch Session 连接成功");
            // 打开SFTP通道
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channelThreadLocal.set(channel);
            // 建立SFTP通道的连接
            channel.connect();
            log.info("SFTP 通道连接成功");
            successed = true;
        } catch (JSchException e) {
            log.error("连接服务器异常", e);
            disconnect();
        }
        return successed;
    }

    /**
     * 关闭通道并退出本次会话
     */
    public void disconnect() {
        ChannelSftp channel = channelThreadLocal.get();
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        Session session = sessionThreadLocal.get();
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 文件上传
     * 采用默认的传输模式：OVERWRITE
     *
     * @param src 输入流
     * @param dst 上传路径
     */
    public boolean upload(String src, String dst) {
        boolean successed = false;
        try (FileInputStream fis = new FileInputStream(src)) {
            Path parent = Path.of(dst).getParent();
            if (createDir(parent.toString())) {
                channelThreadLocal.get().put(fis, dst);
                successed = true;
            }
        } catch (SftpException | IOException e) {
            log.error("文件上传异常，文件路径：" + src + " -> " + dst, e);
        }
        return successed;
    }

    /**
     * 创建一个文件目录
     *
     * @param createPath 路径
     * @return 创建路径是否成功
     */
    public boolean createDir(String createPath) {
        boolean successed = false;
        try {
            ChannelSftp channel = channelThreadLocal.get();
            if (isDirExist(createPath)) {
                channel.cd(createPath);
                return true;
            }
            String[] paths = createPath.split("/");
            StringBuilder filePath = new StringBuilder("/");
            for (String path : paths) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path).append("/");
                if (!isDirExist(filePath.toString())) {
                    // 建立目录
                    channel.mkdir(filePath.toString());
                    // 进入并设置为当前目录
                }
                channel.cd(filePath.toString());
            }
            channel.cd(createPath);
            successed = true;
        } catch (SftpException e) {
            log.error("目录创建异常: " + createPath, e);
        }
        return successed;
    }

    /**
     * 判断目录是否存在
     *
     * @param directory 路径
     * @return 是否存在路径
     */
    public boolean isDirExist(String directory) {
        try {
            SftpATTRS sftpATTRS = channelThreadLocal.get().lstat(directory);
            return sftpATTRS.isDir();
        } catch (SftpException e) {
            return false;
        }
    }

    /**
     * 重命名指定文件或目录
     */
    public boolean rename(String oldPath, String newPath) {
        boolean successed = false;
        try {
            channelThreadLocal.get().rename(oldPath, newPath);
            successed = true;
        } catch (SftpException e) {
            log.error("重命名指定文件或目录异常", e);
        }
        return successed;
    }

    /**
     * 列出指定目录下的所有文件和子目录。
     */
    @SuppressWarnings("rawtypes")
    public Vector ls(String path) {
        try {
            return channelThreadLocal.get().ls(path);
        } catch (SftpException e) {
            log.error("列出指定目录下的所有文件和子目录失败: " + path, e);
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param directory  linux服务器文件地址
     * @param deleteFile 文件名称
     */
    public boolean deleteFile(String directory, String deleteFile) {
        boolean successed = false;
        try {
            ChannelSftp channel = channelThreadLocal.get();
            channel.cd(directory);
            channel.rm(deleteFile);
            successed = true;
        } catch (SftpException e) {
            log.error("删除文件失败", e);
        }
        return successed;
    }

    /**
     * 下载文件
     *
     * @param src 下载源
     * @param dst 下载目标
     */
    public boolean download(String src, String dst) {
        boolean successed = false;
        FileOutputStream fos = null;
        try {
            Path dir = Path.of(dst).getParent();
            if (!Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    log.error("创建路径失败: " + dir, e);
                }
            }
            fos = new FileOutputStream(dst);
            channelThreadLocal.get().get(src, fos);
            successed = true;
        } catch (SftpException | IOException e) {
            log.error("下载文件失败，文件路径：" + src + " -> " + dst, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("文件关闭失败，文件路径：" + dst, e);
                }
            }
        }
        return successed;
    }

    public List<String> lsRecursively(String path) {
        return lsRecursively(path, new ArrayList<>());
    }

    public void rmdirRecursively(String remoteDir) {
        rmdirRecursively(remoteDir, null);
    }

    public void rmdirRecursively(String remoteDir, BiFunction<String, ChannelSftp, Boolean> filter) {
        remoteDir = remoteDir.replace("\\", "/");
        try {
            ChannelSftp channel = channelThreadLocal.get();
            if (filter != null && !filter.apply(remoteDir, channel)) {
                return;
            }
            if (!channel.stat(remoteDir).isDir()) {
                channel.rm(remoteDir);
            }
            if (channel.stat(remoteDir).isDir()) {
                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> dirList = channel.ls(remoteDir);
                for (ChannelSftp.LsEntry entry : dirList) {
                    if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                        continue;
                    }
                    rmdirRecursively(Path.of(remoteDir, entry.getFilename()).toString().replace("\\", "/"), filter);
                }
                if (channel.ls(remoteDir).size() <= 2) {
                    channel.rmdir(remoteDir);
                }
            }
        } catch (SftpException e) {
            log.error("删除目录失败: " + remoteDir, e);
        }
    }

    private List<String> lsRecursively(String path, List<String> list) {
        try {
            ChannelSftp channel = channelThreadLocal.get();
            SftpATTRS attrs = channel.stat(path);
            if (!attrs.isDir()) {
                list.add(path);
            } else {
                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> parent = channel.ls(path);
                // 只遍历非空文件夹
                if (parent.size() > 2) {
                    for (ChannelSftp.LsEntry child : parent) {
                        if (".".equals(child.getFilename()) || "..".equals(child.getFilename())) {
                            continue;
                        }
                        lsRecursively(Path.of(path, child.getFilename()).toString().replace("\\", "/"), list);
                    }
                }
            }
            return list;
        } catch (SftpException e) {
            System.out.println(path);
            System.out.println(list);
            e.printStackTrace();
            return null;
        }
    }
}
