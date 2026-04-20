-- V3: Insert seed data for employees and departments
INSERT INTO departments (name) VALUES ('Engineering'), ('Marketing'), ('Finance');

INSERT INTO employees (name, email, salary, active) VALUES
    ('Alice',   'alice@example.com',   95000.00, TRUE),
    ('Bob',     'bob@example.com',     72000.00, TRUE),
    ('Charlie', 'charlie@example.com', 68000.00, FALSE),
    ('Diana',   'diana@example.com',   88000.00, TRUE);
