package com.venus.kyc.screening.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Local-disk implementation of {@link FileStorageService}.
 * Active when {@code storage.mode=local} (the default — no SFTP needed in dev).
 *
 * <p>All "remote" paths are resolved under {@code storage.local.base-dir}.
 * Drop CSV files into {@code <base-dir>/batch/input/} and the poller will detect them.
 */
@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Value("${storage.local.base-dir:/tmp/screening-local}")
    private String baseDir;

    @Override
    public List<String> listFiles(String dir) {
        File directory = resolve(dir);
        if (!directory.exists() || !directory.isDirectory()) {
            return Collections.emptyList();
        }
        String[] names = directory.list();
        if (names == null) return Collections.emptyList();
        return Arrays.stream(names)
                .filter(n -> new File(directory, n).isFile())
                .collect(Collectors.toList());
    }

    @Override
    public void downloadFile(String remotePath, File localFile) {
        File src = resolve(remotePath);
        try {
            Files.copy(src.toPath(), localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.debug("Local copy: {} → {}", src, localFile);
        } catch (IOException e) {
            throw new UncheckedIOException("downloadFile failed: " + remotePath, e);
        }
    }

    @Override
    public void uploadFile(File localFile, String remoteDir) {
        File destDir = resolve(remoteDir);
        destDir.mkdirs();
        File dest = new File(destDir, localFile.getName());
        try {
            Files.copy(localFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.debug("Local upload: {} → {}", localFile, dest);
        } catch (IOException e) {
            throw new UncheckedIOException("uploadFile failed: " + remoteDir, e);
        }
    }

    @Override
    public void renameFile(String fromPath, String toPath) {
        File src = resolve(fromPath);
        File dest = resolve(toPath);
        dest.getParentFile().mkdirs();
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.debug("Local rename: {} → {}", src, dest);
        } catch (IOException e) {
            throw new UncheckedIOException("renameFile failed: " + fromPath + " → " + toPath, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return resolve(path).exists();
    }

    @Override
    public void mkdirs(String dir) {
        resolve(dir).mkdirs();
    }

    @Override
    public void writeString(String content, String path) {
        File dest = resolve(path);
        dest.getParentFile().mkdirs();
        try {
            Files.writeString(dest.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("writeString failed: " + path, e);
        }
    }

    @Override
    public String readString(String path) {
        File file = resolve(path);
        if (!file.exists()) return null;
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("readString failed: " + path, e);
        }
    }

    @Override
    public void deleteFile(String path) {
        File file = resolve(path);
        if (file.exists()) {
            file.delete();
        }
    }

    private File resolve(String relativePath) {
        return new File(baseDir, relativePath);
    }
}
