CREATE TABLE IF NOT EXISTS announcements (
    id          SERIAL PRIMARY KEY,
    group_id    INTEGER  NOT NULL REFERENCES groups(id),
    text        TEXT     NOT NULL,
    attachments TEXT     NOT NULL DEFAULT '[]',
    is_pinned   BOOLEAN  NOT NULL DEFAULT FALSE,
    created_by  INTEGER  NOT NULL REFERENCES users(id),
    created_at  VARCHAR(50) NOT NULL,
    updated_at  VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS playlists (
    id          SERIAL PRIMARY KEY,
    group_id    INTEGER     NOT NULL REFERENCES groups(id),
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'group',
    meeting_id  INTEGER     REFERENCES meetings(id),
    created_by  INTEGER     NOT NULL REFERENCES users(id),
    created_at  VARCHAR(50) NOT NULL,
    updated_at  VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist_tracks (
    id             SERIAL PRIMARY KEY,
    playlist_id    INTEGER     NOT NULL REFERENCES playlists(id),
    track_id       VARCHAR(50) NOT NULL,
    track_name     VARCHAR(300) NOT NULL,
    artist_name    VARCHAR(300) NOT NULL,
    track_view_url VARCHAR(500) NOT NULL,
    artwork_url    VARCHAR(500),
    preview_url    VARCHAR(500),
    sort_order     INTEGER     NOT NULL,
    created_at     VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id           SERIAL PRIMARY KEY,
    user_id      INTEGER     NOT NULL REFERENCES users(id),
    type         VARCHAR(50) NOT NULL,
    reference_id INTEGER     NOT NULL,
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS fcm_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER      NOT NULL REFERENCES users(id),
    token       VARCHAR(512) NOT NULL,
    device_name VARCHAR(100),
    created_at  VARCHAR(50)  NOT NULL,
    updated_at  VARCHAR(50)  NOT NULL,
    UNIQUE(user_id, token)
);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id                      SERIAL PRIMARY KEY,
    user_id                 INTEGER  NOT NULL REFERENCES users(id),
    group_id                INTEGER  NOT NULL REFERENCES groups(id),
    task_assigned           BOOLEAN  NOT NULL DEFAULT TRUE,
    task_status_changed     BOOLEAN  NOT NULL DEFAULT TRUE,
    meeting_created         BOOLEAN  NOT NULL DEFAULT TRUE,
    meeting_reminder        BOOLEAN  NOT NULL DEFAULT TRUE,
    announcement_posted     BOOLEAN  NOT NULL DEFAULT TRUE,
    group_invite            BOOLEAN  NOT NULL DEFAULT TRUE,
    meeting_reminder_minutes INTEGER NOT NULL DEFAULT 30,
    UNIQUE(user_id, group_id)
);
