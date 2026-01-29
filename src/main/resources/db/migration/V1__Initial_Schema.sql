-- Combined Migration: Initial Schema
-- Complete database schema for Rently - Rental Management System

-- Users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(15) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    id_number VARCHAR(20) UNIQUE,
    id_issue_date DATE NULL,
    id_issue_place VARCHAR(100) NULL,
    gender ENUM('male', 'female', 'other'),
    date_of_birth DATE,
    place_of_origin VARCHAR(255),
    place_of_residence VARCHAR(255),
    bank_name VARCHAR(50) DEFAULT NULL,
    bank_code VARCHAR(20) DEFAULT NULL,
    bank_account_number VARCHAR(30) DEFAULT NULL,
    bank_account_holder VARCHAR(100) DEFAULT NULL,
    status ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    share_full_name BOOLEAN NOT NULL DEFAULT TRUE,
    share_phone BOOLEAN NOT NULL DEFAULT FALSE,
    share_origin BOOLEAN NOT NULL DEFAULT FALSE,
    share_gender BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_phone (phone),
    INDEX idx_users_status (status),
    INDEX idx_users_id_number (id_number),
    INDEX idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User roles table
CREATE TABLE user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role ENUM('LANDLORD', 'TENANT', 'SYSTEM_ADMIN') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role),
    INDEX idx_user_roles_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- OTP verification table
CREATE TABLE otp_verifications (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    type ENUM('REGISTER', 'RESET_PASSWORD', 'TENANT_VERIFICATION') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_phone_type (phone, type),
    INDEX idx_otp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Houses table
CREATE TABLE houses (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description TEXT,
    invoice_due_day INT DEFAULT 10,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_houses_owner_id (owner_id),
    INDEX idx_houses_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rooms table
CREATE TABLE rooms (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    code VARCHAR(20) NOT NULL,
    floor INT NOT NULL DEFAULT 1,
    area_m2 DECIMAL(10,2) NOT NULL,
    base_rent DECIMAL(15,2) NOT NULL,
    max_tenants INT DEFAULT 2,
    status ENUM('EMPTY', 'RESERVED', 'RENTED', 'MAINTENANCE') NOT NULL DEFAULT 'EMPTY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_code_house (house_id, code),
    INDEX idx_rooms_house_id (house_id),
    INDEX idx_rooms_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Room tenants table
CREATE TABLE room_tenants (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_room_tenants_room_id (room_id),
    INDEX idx_room_tenants_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Service fees table
CREATE TABLE service_fees (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    fee_type ENUM('FIXED', 'PER_PERSON', 'SPLIT_EQUAL', 'SPLIT_BY_TENANT') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    unit VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_service_fee_name_house (house_id, name),
    INDEX idx_service_fees_house_id (house_id),
    INDEX idx_service_fees_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Contract templates table
CREATE TABLE contract_templates (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NULL,
    name VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_templates_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_templates_house FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    INDEX idx_contract_templates_owner (owner_id),
    INDEX idx_contract_templates_house (house_id),
    INDEX idx_contract_templates_default (owner_id, house_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Contracts table
CREATE TABLE contracts (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    duration INT NOT NULL DEFAULT 12,
    duration_unit ENUM('MONTH', 'YEAR') NOT NULL DEFAULT 'MONTH',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    payment_period ENUM('MONTHLY', 'QUARTERLY', 'BIANNUAL', 'ANNUAL') NOT NULL DEFAULT 'MONTHLY',
    payment_due_day INT NOT NULL DEFAULT 5,
    monthly_rent DECIMAL(15,2) NOT NULL,
    deposit_months INT NOT NULL DEFAULT 1,
    deposit_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    deposit_paid BOOLEAN NOT NULL DEFAULT FALSE,
    template_id VARCHAR(36) NULL,
    custom_terms TEXT NULL,
    content_snapshot TEXT,
    status ENUM('DRAFT', 'ACTIVE', 'ENDED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contracts_template FOREIGN KEY (template_id) REFERENCES contract_templates(id) ON DELETE SET NULL,
    INDEX idx_contracts_room_id (room_id),
    INDEX idx_contracts_landlord_id (landlord_id),
    INDEX idx_contracts_tenant_id (tenant_id),
    INDEX idx_contracts_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Contract service fees table
CREATE TABLE contract_service_fees (
    id VARCHAR(36) PRIMARY KEY,
    contract_id VARCHAR(36) NOT NULL,
    service_fee_id VARCHAR(36) NULL,
    name VARCHAR(100) NOT NULL,
    fee_type ENUM('FIXED', 'PER_PERSON', 'SPLIT_EQUAL', 'SPLIT_BY_TENANT') NOT NULL DEFAULT 'FIXED',
    amount DECIMAL(15, 2) NOT NULL,
    unit_rate DECIMAL(12, 2) NULL,
    unit VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_service_fees_contract FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_service_fees_service_fee FOREIGN KEY (service_fee_id) REFERENCES service_fees(id) ON DELETE SET NULL,
    INDEX idx_contract_service_fees_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Invoices table (with invoice_type and is_netting)
CREATE TABLE invoices (
    id VARCHAR(36) PRIMARY KEY,
    contract_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    due_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    late_fee_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    invoice_type VARCHAR(20) DEFAULT 'NORMAL',
    is_netting BOOLEAN DEFAULT FALSE,
    status ENUM('DRAFT', 'SENT', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_invoices_contract_id (contract_id),
    INDEX idx_invoices_tenant_id (tenant_id),
    INDEX idx_invoices_period_month (period_month),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_invoice_type (invoice_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Invoice items table
CREATE TABLE invoice_items (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    type ENUM('ROOM_RENT', 'ELECTRICITY', 'WATER', 'SERVICE', 'OTHER') NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_invoice_items_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meter readings table
CREATE TABLE meter_readings (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    electricity_old DECIMAL(10,2) NOT NULL DEFAULT 0,
    electricity_new DECIMAL(10,2) NOT NULL DEFAULT 0,
    electricity_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    water_old DECIMAL(10,2) NOT NULL DEFAULT 0,
    water_new DECIMAL(10,2) NOT NULL DEFAULT 0,
    water_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    UNIQUE KEY uk_meter_reading_room_period (room_id, period_month),
    INDEX idx_meter_readings_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payments table
CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    method ENUM('SEPAY', 'BANK_QR', 'CASH') NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    transaction_code VARCHAR(100),
    proof_image_url VARCHAR(500),
    note TEXT,
    paid_at TIMESTAMP,
    sepay_transaction_id VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_payments_invoice_id (invoice_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_sepay_tx (sepay_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SePay transactions table
CREATE TABLE sepay_transactions (
    id VARCHAR(36) PRIMARY KEY,
    sepay_transaction_id VARCHAR(50) NOT NULL UNIQUE,
    gateway VARCHAR(20) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    transfer_amount DECIMAL(15,2) NOT NULL,
    content TEXT,
    code VARCHAR(100),
    reference_code VARCHAR(100),
    transaction_date TIMESTAMP NOT NULL,
    transfer_type VARCHAR(10) NOT NULL,
    payment_id VARCHAR(36),
    invoice_id VARCHAR(36),
    status ENUM('RECEIVED', 'MATCHED', 'UNMATCHED', 'IGNORED') NOT NULL DEFAULT 'RECEIVED',
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sepay_tx_sepay_id (sepay_transaction_id),
    INDEX idx_sepay_tx_code (code),
    INDEX idx_sepay_tx_status (status),
    INDEX idx_sepay_tx_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tickets table (maintenance requests)
CREATE TABLE tickets (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tickets_house_id (house_id),
    INDEX idx_tickets_room_id (room_id),
    INDEX idx_tickets_tenant_id (tenant_id),
    INDEX idx_tickets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ticket attachments table
CREATE TABLE ticket_attachments (
    id VARCHAR(36) PRIMARY KEY,
    ticket_id VARCHAR(36) NOT NULL,
    type ENUM('IMAGE', 'VIDEO') NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    INDEX idx_ticket_attachments_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Amenities table (house-specific)
CREATE TABLE amenities (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category ENUM('FURNITURE', 'APPLIANCE', 'UTILITY', 'FACILITY', 'OTHER') NOT NULL,
    icon VARCHAR(50) NULL,
    price DECIMAL(15, 2) DEFAULT 0,
    is_custom BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_amenities_house FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    INDEX idx_amenities_house (house_id),
    INDEX idx_amenities_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Room amenities junction table (with condition column)
CREATE TABLE room_amenities (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    amenity_id VARCHAR(36) NOT NULL,
    quantity INT DEFAULT 1,
    `condition` VARCHAR(20),
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_amenities_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_amenity (room_id, amenity_id),
    INDEX idx_room_amenities_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- House shared amenities table
CREATE TABLE house_shared_amenities (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    amenity_id VARCHAR(36) NOT NULL,
    quantity INT DEFAULT 1,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE,
    UNIQUE KEY uk_house_shared_amenity (house_id, amenity_id),
    INDEX idx_house_shared_amenities_house (house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Push subscriptions table for FCM tokens
CREATE TABLE push_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    device_name VARCHAR(100),
    user_agent VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_token (user_id, fcm_token),
    INDEX idx_push_subscriptions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notifications table
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    data JSON,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity logs table
CREATE TABLE activity_logs (
    id VARCHAR(36) PRIMARY KEY,
    landlord_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36),
    entity_type VARCHAR(50),
    description VARCHAR(500) NOT NULL,
    metadata TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_activity_logs_landlord (landlord_id),
    INDEX idx_activity_logs_type (type),
    INDEX idx_activity_logs_created_at (created_at),
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Expense records (category_id uses fixed values from ExpenseCategoryType enum)
CREATE TABLE expenses (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amount DECIMAL(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    receipt_url VARCHAR(500),
    status ENUM('PENDING', 'PAID') NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP NULL,
    payment_method ENUM('CASH', 'BANK_TRANSFER', 'CARD', 'OTHER'),
    payment_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    INDEX idx_expenses_house (house_id),
    INDEX idx_expenses_category (category_id),
    INDEX idx_expenses_status (status),
    INDEX idx_expenses_date (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tasks table for user todo list
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tasks_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Contract snapshots for tracking history
CREATE TABLE contract_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    contract_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    change_note VARCHAR(255),
    created_by VARCHAR(36),
    created_at DATETIME NOT NULL,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_contract_snapshots_contract_id (contract_id),
    INDEX idx_contract_snapshots_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Utility shortfalls table for tracking electricity/water shortfall to be applied to next invoices
CREATE TABLE utility_shortfalls (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    electricity_shortfall DECIMAL(15,2) NOT NULL,
    water_shortfall DECIMAL(15,2) NOT NULL,
    total_shortfall DECIMAL(15,2) NOT NULL,
    per_room_amount DECIMAL(15,2) NOT NULL,
    active_room_count INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    applied_at DATETIME NULL,
    CONSTRAINT fk_utility_shortfalls_house FOREIGN KEY (house_id) REFERENCES houses(id),
    INDEX idx_utility_shortfalls_house_status (house_id, status),
    INDEX idx_utility_shortfalls_house_period (house_id, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create announcements table for system announcements
CREATE TABLE announcements (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type ENUM('INFO', 'WARNING', 'URGENT', 'MAINTENANCE') NOT NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    target_audience ENUM('ALL', 'LANDLORDS', 'TENANTS') NOT NULL DEFAULT 'ALL',
    publish_at DATETIME NULL,
    expire_at DATETIME NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_announcements_status (status),
    INDEX idx_announcements_target_audience (target_audience),
    INDEX idx_announcements_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
