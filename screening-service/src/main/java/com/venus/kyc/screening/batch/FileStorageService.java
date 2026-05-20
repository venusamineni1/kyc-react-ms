package com.venus.kyc.screening.batch;

import java.io.File;
import java.util.List;

/**
 * Storage abstraction for file operations used by the mass screening pipeline.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link LocalFileStorageService} — resolved against a local base directory
 *       (active when {@code storage.mode=local}, the default for dev)</li>
 *   <li>{@link SftpFileStorageService} — delegates to the real SFTP server
 *       (active when {@code storage.mode=sftp})</li>
 * </ul>
 *
 * <p>All path arguments are relative to the storage root (local base dir or SFTP home).
 * Implementations wrap any {@link java.io.IOException} in unchecked exceptions.
 */
public interface FileStorageService {

    /** Lists filenames (not full paths) in the given directory. */
    List<String> listFiles(String dir);

    /** Downloads the file at {@code remotePath} into {@code localFile}. */
    void downloadFile(String remotePath, File localFile);

    /** Uploads {@code localFile} into the given remote directory. */
    void uploadFile(File localFile, String remoteDir);

    /** Atomically renames/moves a file between two paths. */
    void renameFile(String fromPath, String toPath);

    /** Returns true if the path exists. */
    boolean exists(String path);

    /** Creates the directory (and any parents) if it does not already exist. */
    void mkdirs(String dir);

    /** Writes a UTF-8 string to the given path. */
    void writeString(String content, String path);

    /**
     * Reads the file at {@code path} as a UTF-8 string.
     * Returns {@code null} if the file does not exist.
     */
    String readString(String path);

    /** Deletes the file at the given path. */
    void deleteFile(String path);
}
