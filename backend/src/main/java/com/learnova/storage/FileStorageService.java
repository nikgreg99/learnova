package com.learnova.storage;

import java.io.InputStream;

public interface FileStorageService {

        void upload(
            String storageKey,
            InputStream inputStream,
            long size,
            String contentType
        );

        InputStream download(String storageKey);

        void delete(String storageKey);

}
