-- ═══════════════════════════════════════════════════════════════════════════
-- James Property Holdings — SQL Server Management Studio Schema
-- Run this in SSMS to create all tables required by the Java application.
-- ═══════════════════════════════════════════════════════════════════════════

-- 1. Create & use database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'JamesPropertyDB')
    CREATE DATABASE JamesPropertyDB;
GO
USE JamesPropertyDB;
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- USERS TABLE (required by LoginPanel → TenantDAO.authenticate())
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
CREATE TABLE Users (
    UserID   INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(100) NOT NULL UNIQUE,
    Password NVARCHAR(255) NOT NULL,        -- store hashed in production
    FullName NVARCHAR(200),
    Role     NVARCHAR(50) DEFAULT 'Manager'
);
GO

-- Default admin account — change password before deployment
IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'admin')
    INSERT INTO Users (Username, Password, FullName, Role)
    VALUES ('admin', 'admin123', 'James Holding', 'Property Manager');
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- PROPERTY TYPE (lookup)
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PropertyType')
CREATE TABLE PropertyType (
    PropertyTypeID INT IDENTITY(1,1) PRIMARY KEY,
    TypeName       NVARCHAR(50) NOT NULL   -- 'House', 'Townhouse', 'Flat'
);
GO
IF NOT EXISTS (SELECT 1 FROM PropertyType WHERE TypeName = 'House')
BEGIN
    INSERT INTO PropertyType (TypeName) VALUES ('House');      -- ID = 1
    INSERT INTO PropertyType (TypeName) VALUES ('Townhouse');  -- ID = 2
    INSERT INTO PropertyType (TypeName) VALUES ('Flat');       -- ID = 3
END
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- PROPERTY (base table)
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Property')
CREATE TABLE Property (
    PropertyID     INT IDENTITY(1,1) PRIMARY KEY,
    PropertyTypeID INT NOT NULL,
    BuildingSize   DECIMAL(10,2),
    Address        NVARCHAR(255) NOT NULL,
    Location       NVARCHAR(100),
    MarketValue    DECIMAL(12,2),
    MonthlyRental  DECIMAL(10,2),
    Status         NVARCHAR(20) DEFAULT 'Available',    -- 'Available' | 'Occupied'
    FOREIGN KEY (PropertyTypeID) REFERENCES PropertyType(PropertyTypeID)
);
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- PROPERTY SUBTYPES
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'House')
CREATE TABLE House (
    PropertyID INT PRIMARY KEY,
    PlotSize   DECIMAL(10,2),
    FOREIGN KEY (PropertyID) REFERENCES Property(PropertyID) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Townhouse')
CREATE TABLE Townhouse (
    PropertyID  INT PRIMARY KEY,
    UnitNumber  NVARCHAR(20),
    Backyard    NVARCHAR(5),    -- 'yes' | 'no'
    FOREIGN KEY (PropertyID) REFERENCES Property(PropertyID) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Flat')
CREATE TABLE Flat (
    PropertyID     INT PRIMARY KEY,
    UnitNumber     NVARCHAR(20),
    FloorLevel     INT,
    ElevatorAccess NVARCHAR(5),    -- 'yes' | 'no'
    Backyard       NVARCHAR(5),    -- 'yes' | 'no'
    FOREIGN KEY (PropertyID) REFERENCES Property(PropertyID) ON DELETE CASCADE
);
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- TENANT
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Tenant')
CREATE TABLE Tenant (
    TenantID     INT IDENTITY(1,1) PRIMARY KEY,
    FirstName    NVARCHAR(100) NOT NULL,
    LastName     NVARCHAR(100) NOT NULL,
    IDNumber     NVARCHAR(50),
    PhoneNumber  NVARCHAR(30),
    EmailAddress NVARCHAR(150),
    Status       NVARCHAR(20) DEFAULT 'Active'    -- 'Active' | 'Blacklisted'
);
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- LEASE
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Lease')
CREATE TABLE Lease (
    LeaseID          INT IDENTITY(1,1) PRIMARY KEY,
    PropertyID       INT NOT NULL,
    TenantID         INT NOT NULL,
    StartDate        DATE NOT NULL,
    EndDate          DATE NOT NULL,
    MonthlyRent      DECIMAL(10,2) NOT NULL,
    SecurityDeposit  DECIMAL(10,2),
    PaymentDueDate   INT,              -- day of month (1-28)
    GracePeriod      INT DEFAULT 7,    -- days
    LatePenalty      DECIMAL(10,2),    -- amount per day
    Status           NVARCHAR(20) DEFAULT 'Active',   -- 'Active'|'Expired'|'Terminated'
    FOREIGN KEY (PropertyID) REFERENCES Property(PropertyID),
    FOREIGN KEY (TenantID)   REFERENCES Tenant(TenantID)
);
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- PAYMENT
-- ─────────────────────────────────────────────────────────────────────────────
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Payment')
CREATE TABLE Payment (
    PaymentID   INT IDENTITY(1,1) PRIMARY KEY,
    LeaseID     INT NOT NULL,
    AmountPaid  DECIMAL(12,2) NOT NULL,
    PaymentDate DATE NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (LeaseID) REFERENCES Lease(LeaseID)
);
GO

-- ─────────────────────────────────────────────────────────────────────────────
-- SAMPLE DATA (optional — remove before production)
-- ─────────────────────────────────────────────────────────────────────────────

-- Sample properties
IF NOT EXISTS (SELECT 1 FROM Property)
BEGIN
    DECLARE @PropID INT;

    -- House 1
    INSERT INTO Property (PropertyTypeID, BuildingSize, Address, Location, MarketValue, MonthlyRental, Status)
    VALUES (1, 180.0, '123 Main Street', 'Klein Windhoek', 1200000, 8500, 'Available');
    SET @PropID = SCOPE_IDENTITY();
    INSERT INTO House VALUES (@PropID, 650.0);

    -- Flat 1
    INSERT INTO Property (PropertyTypeID, BuildingSize, Address, Location, MarketValue, MonthlyRental, Status)
    VALUES (3, 75.0, '45 Highrise Avenue', 'Katutura', 650000, 4200, 'Available');
    SET @PropID = SCOPE_IDENTITY();
    INSERT INTO Flat VALUES (@PropID, 'F-12', 3, 'yes', 'no');

    -- Townhouse 1
    INSERT INTO Property (PropertyTypeID, BuildingSize, Address, Location, MarketValue, MonthlyRental, Status)
    VALUES (2, 120.0, '88 Valley Road', 'Eros', 980000, 6800, 'Available');
    SET @PropID = SCOPE_IDENTITY();
    INSERT INTO Townhouse VALUES (@PropID, 'T-04', 'yes');
END
GO

-- Sample tenants
IF NOT EXISTS (SELECT 1 FROM Tenant)
BEGIN
    INSERT INTO Tenant (FirstName, LastName, IDNumber, PhoneNumber, EmailAddress, Status)
    VALUES ('Alice', 'Johnson', 'ID-987654', '081-123-4567', 'alice@email.na', 'Active');

    INSERT INTO Tenant (FirstName, LastName, IDNumber, PhoneNumber, EmailAddress, Status)
    VALUES ('Samuel', 'Ndawana', 'ID-123987', '081-765-4321', 'samuel@email.na', 'Active');

    INSERT INTO Tenant (FirstName, LastName, IDNumber, PhoneNumber, EmailAddress, Status)
    VALUES ('Maria', 'van Wyk', 'PP-554421', '081-999-0000', 'maria@email.na', 'Blacklisted');
END
GO

PRINT 'JamesPropertyDB schema created successfully.';
GO
