-- MySQL 8 / InnoDB. Apply to an empty `daln` database.
-- Timestamps are UTC; venue timezone is stored separately for weekly pricing.
CREATE TABLE roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(100) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB;

CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email VARCHAR(190) NOT NULL,
  phone VARCHAR(30) NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  status ENUM('PENDING','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_users_email (email), UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
  user_id BIGINT UNSIGNED NOT NULL, role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE venues (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(180) NOT NULL,
  description TEXT NULL,
  address VARCHAR(500) NOT NULL,
  city VARCHAR(100) NOT NULL,
  timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  location POINT NOT NULL SRID 4326,
  opening_time TIME NOT NULL,
  closing_time TIME NOT NULL,
  status ENUM('PENDING','ACTIVE','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), KEY idx_venues_owner_status (owner_id,status),
  SPATIAL INDEX sidx_venues_location (location), KEY idx_venues_city_status (city,status),
  CONSTRAINT fk_venues_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE sports_categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL, name VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id), UNIQUE KEY uk_sports_code (code), KEY idx_sports_active_name (active,name)
) ENGINE=InnoDB;

CREATE TABLE venue_sports (
  venue_id BIGINT UNSIGNED NOT NULL, sport_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (venue_id,sport_id),
  CONSTRAINT fk_venue_sports_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT fk_venue_sports_sport FOREIGN KEY (sport_id) REFERENCES sports_categories(id)
) ENGINE=InnoDB;

CREATE TABLE courts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  venue_id BIGINT UNSIGNED NOT NULL, sport_id BIGINT UNSIGNED NOT NULL,
  parent_id BIGINT UNSIGNED NULL,
  name VARCHAR(120) NOT NULL,
  court_type VARCHAR(80) NULL,
  capacity SMALLINT UNSIGNED NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  -- A bookable leaf has bookable=TRUE. Parent courts can also be bookable
  -- when configured as a whole-court product.
  bookable BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), KEY idx_courts_venue_sport_active (venue_id,sport_id,active),
  KEY idx_courts_parent (parent_id),
  CONSTRAINT fk_courts_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT fk_courts_sport FOREIGN KEY (sport_id) REFERENCES sports_categories(id),
  CONSTRAINT fk_courts_parent FOREIGN KEY (parent_id) REFERENCES courts(id)
) ENGINE=InnoDB;

-- Materialized ancestry makes it possible to find and lock every conflicting
-- parent/child court in stable ID order. Maintain this table when court trees change.
CREATE TABLE court_closure (
  ancestor_id BIGINT UNSIGNED NOT NULL, descendant_id BIGINT UNSIGNED NOT NULL,
  depth TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (ancestor_id,descendant_id), KEY idx_closure_descendant (descendant_id,ancestor_id),
  CONSTRAINT fk_closure_ancestor FOREIGN KEY (ancestor_id) REFERENCES courts(id),
  CONSTRAINT fk_closure_descendant FOREIGN KEY (descendant_id) REFERENCES courts(id)
) ENGINE=InnoDB;

CREATE TABLE pricing_rules (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  venue_id BIGINT UNSIGNED NOT NULL, sport_id BIGINT UNSIGNED NOT NULL,
  court_id BIGINT UNSIGNED NULL,
  name VARCHAR(120) NOT NULL,
  day_of_week TINYINT UNSIGNED NULL COMMENT '1=Monday..7=Sunday; NULL=all days',
  start_time TIME NOT NULL, end_time TIME NOT NULL,
  valid_from DATE NULL, valid_until DATE NULL,
  price_per_slot DECIMAL(12,2) NOT NULL,
  priority SMALLINT NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id), KEY idx_pricing_lookup (venue_id,sport_id,active,day_of_week,start_time,end_time),
  KEY idx_pricing_court (court_id,active),
  CONSTRAINT fk_pricing_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT fk_pricing_sport FOREIGN KEY (sport_id) REFERENCES sports_categories(id),
  CONSTRAINT fk_pricing_court FOREIGN KEY (court_id) REFERENCES courts(id),
  CONSTRAINT chk_pricing_time CHECK (start_time < end_time),
  CONSTRAINT chk_pricing_dates CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from)
) ENGINE=InnoDB;

CREATE TABLE bookings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  booking_code VARCHAR(32) NOT NULL,
  customer_id BIGINT UNSIGNED NOT NULL,
  status ENUM('HELD','PENDING_PAYMENT','CONFIRMED','CANCELLED','EXPIRED','COMPLETED','REFUNDED') NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'VND',
  total_amount DECIMAL(12,2) NOT NULL,
  deposit_amount DECIMAL(12,2) NOT NULL,
  hold_expires_at DATETIME(6) NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_bookings_code (booking_code),
  UNIQUE KEY uk_bookings_customer_idempotency (customer_id,idempotency_key),
  KEY idx_bookings_expiry (status,hold_expires_at), KEY idx_bookings_customer_created (customer_id,created_at),
  CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT chk_booking_amounts CHECK (total_amount >= 0 AND deposit_amount >= 0 AND deposit_amount <= total_amount)
) ENGINE=InnoDB;

CREATE TABLE booking_details (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  booking_id BIGINT UNSIGNED NOT NULL, venue_id BIGINT UNSIGNED NOT NULL,
  court_id BIGINT UNSIGNED NOT NULL, sport_id BIGINT UNSIGNED NOT NULL,
  starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id), KEY idx_detail_court_time (court_id,starts_at,ends_at),
  KEY idx_detail_venue_start (venue_id,starts_at),
  CONSTRAINT fk_details_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT fk_details_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT fk_details_court FOREIGN KEY (court_id) REFERENCES courts(id),
  CONSTRAINT fk_details_sport FOREIGN KEY (sport_id) REFERENCES sports_categories(id),
  CONSTRAINT chk_detail_time CHECK (starts_at < ends_at)
) ENGINE=InnoDB;

CREATE TABLE payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, booking_id BIGINT UNSIGNED NOT NULL,
  provider VARCHAR(40) NOT NULL, provider_transaction_id VARCHAR(120) NULL,
  amount DECIMAL(12,2) NOT NULL, status ENUM('INITIATED','PENDING','SUCCEEDED','FAILED','REFUNDED') NOT NULL,
  idempotency_key VARCHAR(120) NOT NULL, paid_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_payment_idempotency (provider,idempotency_key),
  UNIQUE KEY uk_payment_provider_tx (provider,provider_transaction_id),
  KEY idx_payments_booking_status (booking_id,status),
  CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE payouts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, venue_id BIGINT UNSIGNED NOT NULL,
  period_start DATE NOT NULL, period_end DATE NOT NULL,
  gross_amount DECIMAL(14,2) NOT NULL, commission_amount DECIMAL(14,2) NOT NULL,
  net_amount DECIMAL(14,2) NOT NULL,
  status ENUM('PENDING','PROCESSING','PAID','FAILED') NOT NULL DEFAULT 'PENDING',
  provider_reference VARCHAR(120) NULL, paid_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_payout_venue_period (venue_id,period_start,period_end),
  KEY idx_payout_status_period (status,period_start),
  CONSTRAINT fk_payout_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT chk_payout_amounts CHECK (gross_amount >= 0 AND commission_amount >= 0 AND net_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE venue_commission_rates (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, venue_id BIGINT UNSIGNED NOT NULL,
  rate_percent DECIMAL(5,2) NOT NULL, valid_from DATETIME(6) NOT NULL, valid_until DATETIME(6) NULL,
  PRIMARY KEY (id), KEY idx_commission_venue_dates (venue_id,valid_from,valid_until),
  CONSTRAINT fk_commission_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT chk_commission_rate CHECK (rate_percent >= 0 AND rate_percent <= 100)
) ENGINE=InnoDB;

CREATE TABLE subscriptions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, venue_id BIGINT UNSIGNED NOT NULL,
  plan_code VARCHAR(40) NOT NULL, status ENUM('TRIAL','ACTIVE','PAST_DUE','CANCELLED','EXPIRED') NOT NULL,
  starts_at DATETIME(6) NOT NULL, ends_at DATETIME(6) NOT NULL,
  provider_reference VARCHAR(120) NULL,
  PRIMARY KEY (id), KEY idx_subscriptions_venue_status (venue_id,status,ends_at),
  CONSTRAINT fk_subscription_venue FOREIGN KEY (venue_id) REFERENCES venues(id)
) ENGINE=InnoDB;
