-- Tạo bảng files
CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    display_name TEXT NOT NULL,     -- tên hiển thị
    file_path TEXT NOT NULL UNIQUE, -- tên file random (.dat)
    file_type TEXT NOT NULL,        -- IMAGE | VIDEO
    mime_type TEXT,                 -- image/png, video/mp4
    file_size INTEGER,              -- size (bytes)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Index tối ưu filter + sort (QUAN TRỌNG)
CREATE INDEX IF NOT EXISTS idx_files_type_created
    ON files(file_type, created_at DESC);

-- Index cho sort toàn bộ (optional nhưng nên có)
CREATE INDEX IF NOT EXISTS idx_files_created
    ON files(created_at DESC);