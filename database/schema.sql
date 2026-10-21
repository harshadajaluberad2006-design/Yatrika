CREATE DATABASE IF NOT EXISTS yatrika_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE yatrika_db;

CREATE TABLE IF NOT EXISTS travel_enquiries (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    travelers INT NOT NULL,
    travel_date DATE DEFAULT NULL,
    budget VARCHAR(50) DEFAULT NULL,
    message TEXT,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
