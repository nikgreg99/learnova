package com.learnova.service.security;


import com.learnova.config.security.SecurityUtils;
import com.learnova.model.Attachment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AttachmentSecurityService {

    public void validateOwnership(Attachment attachment) {
        String currentUserId = SecurityUtils.getCurrentKeycloakId();

        boolean isOwner = attachment.getUploadedBy() != null
            && attachment.getUploadedBy().equals(currentUserId);

        boolean isAdmin = isAdmin();

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                "You are not allowed to access this attachment"
            );
        }
    }

    private boolean isAdmin() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
            && authentication.getAuthorities().stream()
            .anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
            );
    }
}
