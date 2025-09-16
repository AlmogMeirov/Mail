> **Note:** This README provides a high-level overview of the project.  
> For complete documentation, step-by-step guides, and full API references, please see the `/wiki` directory.

# Gmail Application – Full-Stack Email Management System

A Gmail-like email application with a **Node.js/Express backend**, **Android (Java) frontend**, and **MongoDB database**.  

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Android Emulator / Device (set to English language)
- Web browser

### Run the Application
```bash
# From the** src folder**
docker-compose up -d
```

📱 Android Client: All mobile app files are in the /myapplication directory,
please ensure your emulator/device is set to English language before running the app

## Features

 * User registration & JWT authentication

 * Gmail-style labels (system & custom)

 * Email sending, receiving, drafts, and multi-recipient support (To, CC, BCC)

 * Spam detection with automated blacklist filtering

 * Delete, Star, and Search functionality

 * Dark mode with full UI adaptation

**Architecture**: RESTful API with JWT auth, MVVM Android client, automated blacklist filtering.

**Project** Management: Development progress tracked in JIRA

**Documentation**: See /wiki directory for detailed guides and API reference.

MAIL/
├── client/            → Android frontend (Java)
├── node_server/       → Node.js backend
├── cpp_server/        → C++ blacklist server
├── data/              → Shared data (e.g., urls.txt)
├── docker-compose.yml
└── README.md


## Team

  * Almog Meirov
  * Tomer Grady
  * Meir Crown



## Technologies Used
  * *Android (Java) - Mobile client with MVVM pattern* 
  * *Node.js + Express - REST API server*
  * *MongoDB - Document database*
  * *C++17 (multi-threaded) - Blacklist server*
  * *Docker + Docker Compose - Containerization*
  * *C++17 (multi-threaded)*
  * *Docker + Docker Compose*
  * *JWT Authentication - Secure token-based auth*
  * *Bloom Filter* for blacklist
  * *TCP Socket Communication*


