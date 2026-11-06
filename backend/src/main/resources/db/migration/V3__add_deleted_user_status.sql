-- Allow logically deleted users
ALTER TABLE
    app_user DROP
        CONSTRAINT chk_app_user_status;

ALTER TABLE
    app_user ADD CONSTRAINT chk_app_user_status CHECK(
        status IN(
            'ACTIVE',
            'PENDING_APPROVAL',
            'DISABLED',
            'DELETED'
        )
    );
