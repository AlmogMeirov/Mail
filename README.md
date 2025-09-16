Gmail Application - Email Management System
A full-stack Gmail-like email application with Node.js/Express backend, Android (Java) frontend, and MongoDB database.

🚀**Navigate to the **src** directory and run in bash:**

 ```
 docker-compose up -d
 ```
📱 Android Client: All mobile app files are in the /myapplication directory,
please ensure your emulator/device is set to English language before running the app

Features: User registration & authentication, Gmail-style labels, email search, spam detection, drafts, custom labels, archive & star functionality.

Architecture: RESTful API with JWT auth, MVVM Android client, automated blacklist filtering.

Project Management: Development progress tracked in JIRA

Documentation: See /wiki directory for detailed guides and API reference.

Team

Almog Meirov
Tomer Grady
Meir Crown

Technologies Used

Android (Java) - Mobile client with MVVM pattern
Node.js + Express - REST API server
MongoDB - Document database
C++17 (multi-threaded) - Blacklist server
Docker + Docker Compose - Containerization
JWT Authentication - Secure token-based auth
bcrypt - Password hashing
TCP Socket Communication - Inter-service communication


