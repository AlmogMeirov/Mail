# Mail

## Purpose

In this part of the project, we developed a client-server system for managing URL data using a Bloom Filter. The code was structured with a modular design and adherence to SOLID principles, especially the Open/Closed Principle. We containerized the system with Docker, ensuring clean separation between services. Connection handling, error responses, and client-server communication were thoroughly tested and refined.

---

## Project Structure

```
/src        - Source files (C++.cpp, C++.h, Python)
/tests      - Test files 
/data       - Data files (e.g., urls.txt, bloom_state.bin )
/client -     client files - client.py, tcp_client.py
/Dockerfile - Docker setup, docker-compose...
/README.md  - Project documentation
```

---

## Build and Run Instructions

### Build the Docker image

```bash
docker-compose build --no-cache
```

### Start the Server

```bash
docker-compose up -d server
```

### Run the Client

```bash
docker-compose run --rm client
```
(The software will assign it a port and an IP address)

OR

```bash
docker-compose run --rm client {IP} {port number}
```

### To stop all running containers, remove the server container, and free the bound port:

```bash
docker-compose down
```

**optional**
### Activate the tests ()
```bash
docker-compose run --rm tests
```

> **Important:**  
 - client will automatically try to connect to the server container using the internal Docker network.

- On first connection, the server will prompt for Bloom Filter configuration (e.g. 1024 2 1).

- Input must follow the format: COMMAND URL (e.g. POST www.google.com).

- Invalid commands or malformed URLs will result in a 400 Bad Request response.

---

## Expected Input

1. **Initial Configuration**:  
   Enter a line specifying:
   
   ```
   <bit array size> <number of iterations of first hash function> [optional: number of iterations of second hash function]
   ```
   
   Example:
   ```
   256 2 1
   ```

2. **URL Commands**:  
   After configuration, you can add or query URLs interactively:
   <br>
   Adding URL:
    ```
   POST <URL>
     ```
   Checking if URL is in list:
    ```
   GET <URL>
    ```

   Delete the URL from list:
    ```
   DELETE <URL>
    ```
    Only valid URLs can be added.
---

## Example Run

  ```
256 2 2
200 OK
GET www.hhh.com
200 Ok

false
POST www.hhh.com
201 Created
GET www.hhh.com
200 Ok

true true
DELETE www.k.com
404 Not Found
DELETE www.hhh.com
204 No Content
GET www.hhh.com
200 Ok

true false
  ```

---
# Design Principles
This system was designed with SOLID principles in mind, particularly:

Open/Closed Principle (OCP):
The codebase is open for extension but closed for modification.
New command types (e.g., SHUTDOWN, STATS) can be added by introducing new handler functions without modifying existing logic in the command loop.

Modular Command Handling:
Commands are parsed and validated separately. Logic for POST, GET, and DELETE operations is encapsulated, allowing new operations to be introduced easily without breaking existing behavior.

Extensibility Strategy:
Input parsing and validation are designed with flexibility, allowing commands like "SHUTDOWN www.site.com" or "STATS bloom" to be recognized in the future.

This ensures long-term maintainability and adaptability for future use cases without requiring structural rewrites.


## Notes

- Data files are located under the `/data` directory.
- The application expects to find (or create) `urls.txt` at `../data/urls.txt` relative to the executable's runtime location.
- If the file does not exist, it will be created automatically.

---

## Authors

- Developed by Almog Meirov, Tomer Grady, Meir Crown

---

## Technologies Used

- **C++17** - Core server-side logic and Bloom Filter implementation.
- **Docker & Docker Compose** - Containerized setup for consistent cross-platform execution of both client and server.
- **Python 3.0** - Lightweight TCP client for sending requests and receiving responses.
- **Bloom Filter Data Structure** - Probabilistic data structure used to check for membership efficiently
- **Regex-based URL validation** - Ensures incoming URLs follow standard syntax before processing.
- **Clien-server interface** - Line-based text protocol over TCP sockets, supporting POST/GET/DELETE operations.
✅ This documentation has been tested by a peer to ensure clarity and completeness.

---

# Enjoy!
