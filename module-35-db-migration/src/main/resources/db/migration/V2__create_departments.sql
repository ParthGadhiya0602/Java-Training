-- V2: Create the departments table (new table, no existing data affected)
CREATE TABLE departments (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
