# Campus Device Exit Management System

![Java](https://img.shields.io/badge/Java-17-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Maven](https://img.shields.io/badge/Maven-3.8.6-red)
![Swing](https://img.shields.io/badge/GUI-Swing-yellowgreen)

A Java application for tracking student devices leaving campus using QR code scanning.

## Features

- 📷 Webcam QR code scanning
- 🏷️ Student database management
- 🖨️ QR code generation for each device
- 🔍 Real-time validation system
- 📊 MySQL database integration
## Technologies Used
- **Frontend:** Java Swing
- **Backend:** Java (JDBC for database connectivity)
- **Build Tool:** Apache Maven
- **Database:** MySQL
- **QR Code Generation & Scanning:** ZXing Library (com.google.zxing)
- **Webcam Integration:** Webcam Capture Library (com.github.sarxos.webcam-capture)
## Prerequisites

- Java JDK 17+
- MySQL Server 8.0+
- Webcam
- Maven 3.8.6+

## Installation

### 1. Database Setup

```sql
CREATE DATABASE campus_db;
CREATE USER 'campus_user'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON campus_db.* TO 'campus_user'@'localhost';
FLUSH PRIVILEGES;

USE campus_db;

CREATE TABLE students (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50),
    pc_serial VARCHAR(50) UNIQUE
);
```
### 2. project structure
```
Campus-pc-exit-management/
├── src/
│   └── campusPc/
│       ├── MainApp.java                # Main application entry point
│       ├── DatabaseManager.java        # Handles all database interactions
│       ├── RegistrationPanel.java      # Student registration form (generates QR codes)
│       ├── ScannerPanel.java           # PC exit scanning interface
│       ├── PcExitReportPanel.java      # Displays PC exit reports
│       └── ValidationUtil.java         # Utility for input validation
├── lib/
│   └── mysql-connector-j-8.x.x.jar     # MySQL JDBC Driver
│   └── zxing-core-x.x.x.jar            # ZXing core library for QR code generation
│   └── zxing-javase-x.x.x.jar          # ZXing Java SE client for image writing
├── QRCodes/                            # Directory where generated QR codes are saved (created on first generation)
├── .gitignore                          # Specifies files to ignore in Git
└── README.md                           # This file
```