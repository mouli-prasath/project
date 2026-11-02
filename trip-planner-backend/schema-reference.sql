-- AI Trip Planner — Reference Schema (MySQL 8)
-- Note: with `spring.jpa.hibernate.ddl-auto=update` (see application.yml),
-- Hibernate creates/updates these tables automatically from the JPA entities
-- on startup. This file is a human-readable reference of that schema, and a
-- starting point if you later migrate to Flyway/Liquibase for production.

CREATE DATABASE IF NOT EXISTS trip_planner;
USE trip_planner;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    source VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    travelers INT NOT NULL,
    budget DECIMAL(12,2),
    travel_style VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    preferred_transport VARCHAR(30),
    accommodation_preference VARCHAR(30),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_trip_dates CHECK (end_date >= start_date)
);

CREATE TABLE trip_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL UNIQUE,
    interests VARCHAR(500),
    additional_preferences TEXT,
    CONSTRAINT fk_preferences_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE TABLE places (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    address VARCHAR(500),
    opening_time TIME,
    closing_time TIME
);

CREATE TABLE itinerary_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    date DATE NOT NULL,
    theme VARCHAR(255),
    CONSTRAINT fk_days_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    UNIQUE KEY uq_trip_day (trip_id, day_number)
);

CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_day_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    start_time TIME,
    end_time TIME,
    duration_minutes INT,
    estimated_cost DECIMAL(10,2),
    description TEXT,
    category VARCHAR(50),
    activity_order INT NOT NULL,
    CONSTRAINT fk_activities_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE,
    CONSTRAINT fk_activities_place FOREIGN KEY (place_id) REFERENCES places(id)
);

CREATE TABLE trip_budget (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL UNIQUE,
    accommodation_cost DECIMAL(12,2),
    food_cost DECIMAL(12,2),
    transport_cost DECIMAL(12,2),
    activity_cost DECIMAL(12,2),
    total_estimated_cost DECIMAL(12,2),
    CONSTRAINT fk_budget_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_place (user_id, place_id)
);
