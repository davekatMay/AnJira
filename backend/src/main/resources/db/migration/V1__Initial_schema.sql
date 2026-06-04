CREATE TABLE IF NOT EXISTS users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email       VARCHAR(100) NOT NULL,
    created_at  VARCHAR(50)  NOT NULL,
    updated_at  VARCHAR(50)  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_idx ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS users_email_idx ON users(email);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER     NOT NULL REFERENCES users(id),
    token       VARCHAR(512) NOT NULL,
    expires_at  VARCHAR(50)  NOT NULL,
    created_at  VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS groups (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    avatar      VARCHAR(500),
    invite_code VARCHAR(20)  NOT NULL,
    created_by  INTEGER     NOT NULL REFERENCES users(id),
    created_at  VARCHAR(50)  NOT NULL,
    updated_at  VARCHAR(50)  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS groups_invite_code_idx ON groups(invite_code);

CREATE TABLE IF NOT EXISTS group_members (
    id          SERIAL PRIMARY KEY,
    group_id    INTEGER     NOT NULL REFERENCES groups(id),
    user_id     INTEGER     NOT NULL REFERENCES users(id),
    role        VARCHAR(20) NOT NULL,
    created_at  VARCHAR(50) NOT NULL,
    updated_at  VARCHAR(50) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS group_members_unique_idx ON group_members(group_id, user_id);

CREATE TABLE IF NOT EXISTS tasks (
    id          SERIAL PRIMARY KEY,
    group_id    INTEGER     NOT NULL REFERENCES groups(id),
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    deadline    VARCHAR(50),
    status      VARCHAR(50) NOT NULL,
    created_by  INTEGER     NOT NULL REFERENCES users(id),
    assigned_to INTEGER     REFERENCES users(id),
    created_at  VARCHAR(50) NOT NULL,
    updated_at  VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS subtasks (
    id           SERIAL PRIMARY KEY,
    task_id      INTEGER  NOT NULL REFERENCES tasks(id),
    title        VARCHAR(200) NOT NULL,
    is_completed BOOLEAN  NOT NULL,
    created_at   VARCHAR(50) NOT NULL,
    updated_at   VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS meetings (
    id            SERIAL PRIMARY KEY,
    group_id      INTEGER     NOT NULL REFERENCES groups(id),
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(1000),
    date_time     VARCHAR(50) NOT NULL,
    end_date_time VARCHAR(50),
    location      VARCHAR(200),
    created_by    INTEGER     NOT NULL REFERENCES users(id),
    created_at    VARCHAR(50) NOT NULL,
    updated_at    VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS meeting_participants (
    id         SERIAL PRIMARY KEY,
    meeting_id INTEGER    NOT NULL REFERENCES meetings(id),
    user_id    INTEGER    NOT NULL REFERENCES users(id),
    status     VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at VARCHAR(50) NOT NULL,
    updated_at VARCHAR(50) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS meeting_participants_unique_idx ON meeting_participants(meeting_id, user_id);

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
