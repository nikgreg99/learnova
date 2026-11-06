ALTER TABLE
    app_user ADD COLUMN username VARCHAR(100);

ALTER TABLE
    app_user ADD CONSTRAINT uk_app_user_username UNIQUE(username);
