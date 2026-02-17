package com.venus.kyc.risk.batch;

import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SftpService {

    private final SftpRemoteFileTemplate sftpTemplate;

    public SftpService(SftpRemoteFileTemplate sftpTemplate) {
        this.sftpTemplate = sftpTemplate;
    }

    @org.springframework.beans.factory.annotation.Value("${sftp.enabled:true}")
    private boolean sftpEnabled;

    public void uploadFile(File localFile, String remoteDir) {
        if (!sftpEnabled) {
            System.out.println("SFTP is disabled. Mocking upload for: " + localFile.getName());
            return;
        }
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
}
