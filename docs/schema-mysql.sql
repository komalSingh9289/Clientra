-- =====================================================================
-- Clientra physical schema (MySQL 8) - synopsis section 7.6
-- The application creates/updates these tables itself (spring.jpa.hibernate.ddl-auto=update),
-- so running this file is OPTIONAL. Use it to review the schema, or to pre-create the database
-- with real FOREIGN KEY constraints.
-- Differences from the synopsis tables (all additive or a keyword-safe rename):
--   * tbl_pipeline_stages.stage_type, tbl_leads.ai_score / flagged / stage_changed_at, tbl_users.active,
--     tbl_reminders.priority / due_notified / overdue_notified, tbl_deals.closed_at   (features the UI needs)
--   * tbl_ai_logs.logged_at and tbl_audit_logs.logged_at  (the synopsis column "timestamp" is a SQL keyword)
--   * tbl_audit_logs, tbl_notification_rules, tbl_pipeline_stages are the extra tables for Module 8 / UC-12 / UC-14
-- =====================================================================
CREATE DATABASE IF NOT EXISTS clientra CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clientra;

CREATE TABLE tbl_companies (
  company_id   INT AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(150) NOT NULL,
  email        VARCHAR(120) UNIQUE,
  phone        VARCHAR(120),
  website      VARCHAR(150),
  address      TEXT,
  industry     VARCHAR(100)
);

CREATE TABLE tbl_roles (
  role_id     INT AUTO_INCREMENT PRIMARY KEY,
  role_name   VARCHAR(50) NOT NULL UNIQUE,      -- Administrator | Sales Manager | Sales Representative
  description TEXT
);

CREATE TABLE tbl_users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  company_id    INT NOT NULL,
  role_id       INT NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  email         VARCHAR(120) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,           -- bcrypt, work factor 12
  active        BIT NOT NULL DEFAULT 1,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_login    DATETIME(6),
  CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES tbl_companies (company_id),
  CONSTRAINT fk_users_role    FOREIGN KEY (role_id)    REFERENCES tbl_roles (role_id)
);

CREATE TABLE tbl_pipeline_stages (
  stage_id       INT AUTO_INCREMENT PRIMARY KEY,
  stage_name     VARCHAR(80) NOT NULL,
  sequence_order INT NOT NULL,
  stage_type     ENUM('OPEN','WON','LOST') NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE tbl_leads (
  lead_id          INT AUTO_INCREMENT PRIMARY KEY,
  assigned_to      INT,
  full_name        VARCHAR(120) NOT NULL,
  email            VARCHAR(120),
  phone            VARCHAR(30),
  company          VARCHAR(120),
  source           VARCHAR(100),
  stage_id         INT NOT NULL,
  priority_label   VARCHAR(50) DEFAULT 'Low',
  ai_score         INT NOT NULL DEFAULT 0,
  flagged          BIT NOT NULL DEFAULT 0,
  created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  stage_changed_at DATETIME(6),
  CONSTRAINT fk_leads_user  FOREIGN KEY (assigned_to) REFERENCES tbl_users (user_id),
  CONSTRAINT fk_leads_stage FOREIGN KEY (stage_id)    REFERENCES tbl_pipeline_stages (stage_id),
  INDEX idx_leads_assigned (assigned_to), INDEX idx_leads_stage (stage_id), INDEX idx_leads_email (email)
);

CREATE TABLE tbl_activities (
  activity_id     INT AUTO_INCREMENT PRIMARY KEY,
  lead_id         INT NOT NULL,
  user_id         INT NOT NULL,
  type            ENUM('Call','Email','Meeting','Note') NOT NULL,
  description     TEXT,
  sentiment_label VARCHAR(50),
  activity_date   DATETIME(6) NOT NULL,
  CONSTRAINT fk_act_lead FOREIGN KEY (lead_id) REFERENCES tbl_leads (lead_id),
  CONSTRAINT fk_act_user FOREIGN KEY (user_id) REFERENCES tbl_users (user_id),
  INDEX idx_activities_lead (lead_id)
);

CREATE TABLE tbl_deals (
  deal_id      INT AUTO_INCREMENT PRIMARY KEY,
  lead_id      INT NOT NULL,
  amount       DECIMAL(12,2) NOT NULL,
  status       ENUM('Open','Won','Lost') NOT NULL DEFAULT 'Open',
  closing_date DATE,
  notes        TEXT,
  closed_at    DATETIME(6),
  CONSTRAINT fk_deals_lead FOREIGN KEY (lead_id) REFERENCES tbl_leads (lead_id),
  INDEX idx_deals_lead (lead_id)
);

CREATE TABLE tbl_reminders (
  reminder_id      INT AUTO_INCREMENT PRIMARY KEY,
  lead_id          INT NOT NULL,
  user_id          INT NOT NULL,
  reminder_time    DATETIME(6) NOT NULL,
  message          TEXT,
  status           ENUM('Pending','Completed','Cancelled') NOT NULL DEFAULT 'Pending',
  priority         VARCHAR(10) DEFAULT 'Medium',
  due_notified     BIT NOT NULL DEFAULT 0,
  overdue_notified BIT NOT NULL DEFAULT 0,
  CONSTRAINT fk_rem_lead FOREIGN KEY (lead_id) REFERENCES tbl_leads (lead_id),
  CONSTRAINT fk_rem_user FOREIGN KEY (user_id) REFERENCES tbl_users (user_id),
  INDEX idx_reminders_user (user_id)
);

CREATE TABLE tbl_ai_logs (
  log_id        INT AUTO_INCREMENT PRIMARY KEY,
  lead_id       INT,
  request_type  VARCHAR(100),
  input_summary TEXT,
  ai_response   TEXT,
  logged_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ailog_lead FOREIGN KEY (lead_id) REFERENCES tbl_leads (lead_id)
);

CREATE TABLE tbl_ai_responses (
  response_id     INT AUTO_INCREMENT PRIMARY KEY,
  log_id          INT NOT NULL,
  insights_score  DOUBLE,
  summary         TEXT,
  recommendations TEXT,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_airesp_log FOREIGN KEY (log_id) REFERENCES tbl_ai_logs (log_id)
);

CREATE TABLE tbl_audit_logs (          -- append-only: the application never updates or deletes rows
  id        INT AUTO_INCREMENT PRIMARY KEY,
  user_id   INT,
  user_name VARCHAR(120),
  action    VARCHAR(40) NOT NULL,
  entity    VARCHAR(120),
  detail    TEXT,
  logged_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_audit_time (logged_at)
);

CREATE TABLE tbl_notification_rules (  -- single row, id = 1
  id                INT PRIMARY KEY,
  email_on_due      BIT NOT NULL DEFAULT 1,
  email_on_overdue  BIT NOT NULL DEFAULT 1,
  email_on_negative BIT NOT NULL DEFAULT 1,
  weekly_digest     BIT NOT NULL DEFAULT 0
);
