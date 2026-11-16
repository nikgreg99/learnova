package com.learnova.handler;

import com.learnova.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentStorageTransactionHandler {

    private final FileStorageService fileStorageService;

    public void registerUploadRollback(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            fileStorageService.delete(storageKey);
                        } catch (Exception exception) {
                            log.error(
                                "Failed to remove orphaned file from MinIO: {}",
                                storageKey,
                                exception
                            );
                        }
                    }
                }
            }
        );
    }

    public void registerDeleteAfterCommit(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorageService.delete(storageKey);
                    } catch (Exception exception) {
                        log.error(
                            "Failed to delete file from MinIO: {}",
                            storageKey,
                            exception
                        );
                    }
                }
            }
        );
    }
}

