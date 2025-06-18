# Mail

you can find our repository in the link: https://github.com/AlmogMeirov/Mail.git

**This part implements the Node.js web server (Exercise 3), responsible for user authentication, mail handling, and integrating with the blacklist TCP server from Exercise 2.**

## Purpose

In this part of the project, we developed a *Node.js web server* that exposes a RESTful API for handling a simple mail system (similar to Gmail). The server interacts with a C++ *blacklist server* from exercise 2 to verify links within mail content using a *TCP socket*.

The system supports:

* User registration and login with JWT authentication
* Sending and receiving mails
* Label creation and management
* Integration with the blacklist server (via TCP) to block mails with blacklisted URLs


## Build and Run Instructions

### Prerequisites:

* Docker
* Docker Compose

  
The following curl commands are written for bash on Linux-based systems.
They can also be executed on Windows using CMD or PowerShell, with appropriate syntax adjustments (e.g., quoting and escaping).

#### Step 1: Build & Start the Services
### Open fitst terminal:

  In the root project directory, run:

 ```
  docker-compose up --build
 ```

  This will build and start two services:

* mail-server: A Node.js web server on port 3000
* blacklist-server: A C++ TCP server on port 5555, connected to a persistent urls.txt file for blacklist storage

---

### Open a second terminal:
## API Usage
  ###  Register a user

    ```
    curl -i -X POST http://localhost:3000/api/users -H "Content-Type: application/json" -d '{"email":"bob@example.com","password":"bobspassword","firstName":"Bob","lastName":"Builder"}'
    ```

  ### Login to get JWT token

    ```
    BOB_TOKEN=$(curl -s -X POST http://localhost:3000/api/tokens -H "Content-Type: application/json" -d '{"email":"bob@example.com","password":"bobspassword"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    ```
    Save the token from the response to use in future authenticated requests.

## Mail

  ### Send a new mail

    ```
    curl -i -X POST http://localhost:3000/api/mails -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d '{"sender":"alice@example.com","recipients":["bob@example.com"],"subject":"Hello","content":"Visit http://tomer.com"}'

    If any link in the content is blacklisted, you’ll get:

    ```
    HTTP/1.1 400 Bad Request
    { "error": "Failed to validate message links" }
    ```

 ###  Send mail to multiple recipients
    ```
    curl -i -X POST http://localhost:3000/api/mails -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d '{"sender":"alice@example.com","recipients":     ["bob@example.com","meir@example.com"],"subject":"Team Update","content":"Meeting at 3PM today."}'
    ```

  ### Get the 50 most recent mails

    ```
    curl -i -X GET http://localhost:3000/api/mails -H "Authorization: Bearer <TOKEN>"
    ```

  ### Search mails by query

    ```
    curl -i -X GET http://localhost:3000/api/mails/search/<QUERY> -H "Authorization: Bearer <TOKEN>"
    Replace <QUERY> with the search string you want.
    ```

  ### Search mails by ID
     ```
    curl -i -X GET http://localhost:3000/api/mails/<MAIL_ID> -H "Authorization: Bearer <TOKEN>"
    Replace <MAIL_ID> with the ID of the mail you want.
    ```

  ### Assigning a label to a mail
  
  ```
  curl -i -X POST http://localhost:3000/api/mails/<MAIL_ID>/labels -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d '{"labelId":"<LABEL_ID>"}'
  ```


## Labels

  ### Create a label

    ```bash
    curl -i -X POST http://localhost:3000/api/labels \
    -H "Content-Type: application/json" \
    -d '{"name": "Work"}'
    ```

  ### View all labels

    ```bash
    curl -i http://localhost:3000/api/labels
    ```

  #### Get a label by ID for the current user
  ```
  ```
  
  ### Update a label by ID for the current user
  ```
  curl -i -X PATCH http://localhost:3000/api/labels/<LABEL_ID> -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d '{"name": "UpdatedLabelName"}'
  ```
  
  ### Delete a label by ID for the current user
  ```
  curl -i -X DELETE http://localhost:3000/api/labels/<LABEL_ID> -H "Authorization: Bearer <TOKEN>"
  ```
  ### Search labels by substring in name
  ```
  curl -i -X GET http://localhost:3000/api/labels/search/<SUBSTRING> -H "Authorization: Bearer <TOKEN>"
  ```

## Blacklist

  ### Add a malicious URL

    ```bash
    curl -i -X POST http://localhost:3000/api/blacklist \
    -H "Content-Type: application/json" \
    -d '{"url": "http://tomer.com"}'
    ```

  ### Remove a malicious URL


##  Implementation Notes

  * The C++ server uses a Bloom Filter to quickly check for URL membership
  * All communication between the web server and C++ blacklist server happens over TCP
  * Data is kept *in memory* in the web server (no database is used)

  ---


## Docker Notes

  * *Blacklist server* expects the file data/urls.txt to exist or will create it
  * Ports used:
    * Node server: 3000
    * TCP server: 5555 (internal communication only)
  * Containers communicate using Docker’s internal network (via container name)

  To stop the services:

  ```bash
  docker-compose down
  ```


##  Project Structure

MAIL/
├── .vscode/                         → VSCode workspace settings

├── data/                            → Shared data (e.g., urls.txt used by the C++ server)

├── server/

│   └── controllers/

│       └── mailController.js        → Legacy controller (can be migrated under src/)

├── src/

│   ├── cpp_server/                  → C++ TCP blacklist server

│   │   ├── BloomFilter.cpp/.h       → Bloom filter implementation

│   │   ├── Hash*.h                  → Hash function variants

│   │   ├── InfiniteCommandLoop.cpp  → Main server loop

│   │   ├── URL*.cpp/.h              → URL checker, storage, and utility logic

│   │   ├── tcp_client.py            → Python client for testing

│   │   ├── CMakeLists.txt           → Build configuration for C++ components

│   │   └── dockerfile               → Docker config for C++ server

│   └── node_server/                → Node.js web server

│       ├── controllers/            → Logic for routes (e.g., auth, mails)

│       ├── middlewares/           → Token authentication (JWT)

│       ├── models/                 → In-memory storage for users and mails

│       ├── routes/                 → Express route definitions

│       ├── uploads/                → Uploaded user profile images

│       ├── utils/                  → Utility functions (e.g., crypto)

│       ├── app.js                  → Express app setup

│       ├── curl/                   → CURL test scripts (optional)

│       ├── dockerfile              → Docker config for Node.js server

│       ├── index.js                → Main entry point (runs the server)

│       ├── package.json            → Project metadata and dependencies

│       └── package-lock.json

├── docker-compose.yml             → Compose file to orchestrate both services

├── dockerfile                     → (Possibly legacy) Dockerfile

├── CMakeLists.txt                 → Top-level CMake (links into src/cpp_server)

├── package.json                   → (Legacy or root Node metadata)

├── package-lock.json

└── README.md                      → Project documentation


## Team

  * Almog Meirov
  * Tomer Grady
  * Meir Crown



## Technologies Used

  * *Node.js + Express*
  * *C++17 (multi-threaded)*
  * *Docker + Docker Compose*
  * *JWT Authentication*
  * *Bloom Filter* for blacklist
  * *TCP Socket Communication*
#Enjoy!!
