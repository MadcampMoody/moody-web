-- Fix playlist reference issues
-- Step 1: Drop existing foreign key constraint from music table
ALTER TABLE music DROP FOREIGN KEY FKkkltgb4crajsq3lgi4ydgr023;

-- Step 2: Rename playlists table's primary key column to avoid confusion
ALTER TABLE playlists CHANGE COLUMN playlist_id playlists_id_trash BIGINT NOT NULL AUTO_INCREMENT;

-- Step 3: Add proper foreign key constraint to reference playlist table
ALTER TABLE music 
ADD CONSTRAINT FK_music_playlist 
FOREIGN KEY (playlist_id) REFERENCES playlist(playlist_id);

-- Optional: Show the current structure to verify
-- DESCRIBE music;
-- DESCRIBE playlist;
-- DESCRIBE playlists; 