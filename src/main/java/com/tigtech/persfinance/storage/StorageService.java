package com.tigtech.persfinance.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Uploads a file and returns a public URL where it can be accessed.
     */
    String uploadUserPhoto(MultipartFile file, String userId) throws Exception;

    /**
     * Delete a stored file by its path or URL.
     */
    void delete(String pathOrUrl) throws Exception;
}

