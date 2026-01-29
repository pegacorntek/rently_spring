-- Add DELETED status to users table status enum
ALTER TABLE users MODIFY COLUMN status ENUM('ACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE';
