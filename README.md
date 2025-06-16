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

#### Step 1: Build & Start the Services

  In the root project directory, run:

  ```bash
  docker-compose up --build
  ```

  This will build and start two services:

* mail-server: A Node.js web server on port 3000
* blacklist-server: A C++ TCP server on port 5555, connected to a persistent urls.txt file for blacklist storage

---

## API Usage
  ###  Register a user

    ```bash
    curl -i -X POST http://localhost:3000/api/users \
    -H "Content-Type: application/json" \
    -d '{"email": "alice@example.com", "password": "1234", "firstName": "Alice", "lastName": "Wonderland"}'
    ```

  ### Login to get JWT token

    ```bash
    curl -i -X POST http://localhost:3000/api/tokens \
    -H "Content-Type: application/json" \
    -d '{"email": "alice@example.com", "password": "1234"}'
    ```

    Save the token from the response to use in future authenticated requests.

---

## Mail

  ### Send a new mail

    ```bash
    curl -i -X POST http://localhost:3000/api/mails \
    -H "Authorization: Bearer <TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{"sender":"alice@example.com","recipient":"bob@example.com","subject":"Hello","content":"Visit http://tomer.com"}'
    ```

    If any link in the content is blacklisted, you’ll get:

    ```
    HTTP/1.1 400 Bad Request
    { "error": "Failed to validate message links" }
    ```

  ### Get the 50 most recent mails

    ```bash
    curl -i -X GET http://localhost:3000/api/mails \
    -H "Authorization: Bearer <TOKEN>"
    ```

  ### Search mails by query

    ```bash
    curl -i http://localhost:3000/api/mails/search/hello/ \
    -H "Authorization: Bearer <TOKEN>"
    ```

##  Labels

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


## Blacklist

  ### Add a malicious URL

    ```bash
    curl -i -X POST http://localhost:3000/api/blacklist \
    -H "Content-Type: application/json" \
    -d '{"url": "http://tomer.com"}'
    ```


##  Project Structure

MAIL/
├── .vscode/ → VSCode workspace settings
├── data/ → Shared data (e.g., urls.txt used by the C++ server)
├── server/
│ └── controllers/
│ └── mailController.js → Legacy controller (can be migrated under src/)
├── src/
│ ├── cpp_server/ → C++ TCP blacklist server
│ │ ├── BloomFilter.cpp/.h → Bloom filter implementation
│ │ ├── Hash*.h → Hash function variants
│ │ ├── InfiniteCommandLoop.cpp → Main server loop
│ │ ├── URL*.cpp/.h → URL checker, storage, and utility logic
│ │ ├── tcp_client.py → Python client for testing
│ │ ├── CMakeLists.txt → Build configuration for C++ components
│ │ └── dockerfile → Docker config for C++ server
│ └── node_server/ → Node.js web server
│ ├── controllers/ → Logic for routes (e.g., auth, mails)
│ ├── middlewares/ → Token authentication (JWT)
│ ├── models/ → In-memory storage for users and mails
│ ├── routes/ → Express route definitions
│ ├── uploads/ → Uploaded user profile images
│ ├── utils/ → Utility functions (e.g., crypto)
│ ├── app.js → Express app setup
│ ├── curl/ → CURL test scripts (optional)
│ ├── dockerfile → Docker config for Node.js server
│ ├── index.js → Main entry point (runs the server)
│ ├── package.json → Project metadata and dependencies
│ └── package-lock.json
├── docker-compose.yml → Compose file to orchestrate both services
├── dockerfile → (Possibly legacy) Dockerfile
├── CMakeLists.txt → Top-level CMake (links into src/cpp_server)
├── package.json → (Legacy or root Node metadata)
├── package-lock.json
└── README.md → Project documentation


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
