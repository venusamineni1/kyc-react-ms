package com.venus.kyc.screening.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "sftp")
public class SftpService {

    private final SftpRemoteFileTemplate sftpTemplate;

    public SftpService(SftpRemoteFileTemplate sftpTemplate) {
        this.sftpTemplate = sftpTemplate;
    }

    public void uploadFile(File localFile, String remoteDir) {
        sftpTemplate.execute(session -> {
            if (!session.exists(remoteDir)) {
                session.mkdir(remoteDir);
            }
            try (FileInputStream fis = new FileInputStream(localFile)) {
                session.write(fis, remoteDir + "/" + localFile.getName());
            }
            return null;
        });
    }

    public void downloadFile(String remoteFilePath, File localFile) {
        sftpTemplate.execute(session -> {
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                session.read(remoteFilePath, fos);
            }
            return null;
        });
    }

    public List<String> listFiles(String remoteDir) {
        return Arrays.stream(sftpTemplate.list(remoteDir))
                .map(f -> f.getFilename())
                .collect(Collectors.toList());
    }

    public void deleteFile(String remoteFilePath) {
        sftpTemplate.remove(remoteFilePath);
    }

    /** Atomically move/rename a remote file (used to transition CSV files between state dirs). */
    public void renameFile(String fromPath, String toPath) {
        sftpTemplate.rename(fromPath, toPath);
    }

    /** Returns true if the remote path exists. */
    public boolean exists(String remotePath) {
        return sftpTemplate.exists(remotePath);
    }

    /** Ensure a remote directory exists; creates it recursively if needed. */
    public void mkdirs(String remoteDir) {
        sftpTemplate.execute(session -> {
            if (!session.exists(remoteDir)) {
                session.mkdir(remoteDir);
            }
            return null;
        });
    }

    /**
     * Write a UTF-8 string as a remote file (used for .ack.json and .meta.json read-back).
     * Parent directory must already exist.
     */
    public void writeString(String content, String remoteFilePath) {
        sftpTemplate.execute(session -> {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                session.write(is, remoteFilePath);
            }
            return null;
        });
    }

    /**
     * Read a remote file as a UTF-8 string (used for reading .meta.json companion files).
     * Returns null if the file does not exist.
     */
    public String readString(String remoteFilePath) {
        return sftpTemplate.execute(session -> {
            if (!session.exists(remoteFilePath)) return null;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                session.read(remoteFilePath, baos);
                return baos.toString(StandardCharsets.UTF_8);
            }
        });
    }
}
