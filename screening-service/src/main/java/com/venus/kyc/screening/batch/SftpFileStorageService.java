package com.venus.kyc.screening.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * SFTP implementation of {@link FileStorageService}.
 * Active when {@code storage.mode=sftp} (staging / production environments).
 * Delegates all operations to the underlying {@link SftpService}.
 */
@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "sftp")
public class SftpFileStorageService implements FileStorageService {

    private final SftpService sftpService;

    public SftpFileStorageService(SftpService sftpService) {
        this.sftpService = sftpService;
    }

    @Override
    public List<String> listFiles(String dir) {
        return sftpService.listFiles(dir);
    }

    @Override
    public void downloadFile(String remotePath, File localFile) {
        sftpService.downloadFile(remotePath, localFile);
    }

    @Override
    public void uploadFile(File localFile, String remoteDir) {
        sftpService.uploadFile(localFile, remoteDir);
    }

    @Override
    public void renameFile(String fromPath, String toPath) {
        sftpService.renameFile(fromPath, toPath);
    }

    @Override
    public boolean exists(String path) {
        return sftpService.exists(path);
    }

    @Override
    public void mkdirs(String dir) {
        sftpService.mkdirs(dir);
    }

    @Override
    public void writeString(String content, String path) {
        sftpService.writeString(content, path);
    }

    @Override
    public String readString(String path) {
        return sftpService.readString(path);
    }

    @Override
    public void deleteFile(String path) {
        sftpService.deleteFile(path);
    }
}
