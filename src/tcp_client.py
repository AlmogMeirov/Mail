import socket
import time
import sys

#HOST = "server"  # "server" = Docker service name (not 127.0.0.1!)
#PORT = 1234 # Port to connect to
RETRY_ATTEMPTS = 5 # Number of connection retry attempts
RETRY_DELAY = 2  # seconds


class TCPClient:
    def __init__(self, host, port):
        # Initialize a TCP socket
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connected = False # Connection status
        self.host = host
        self.port = port

    def connect(self):
        # Attempt to connect to the server with retries
        for attempt in range(1, RETRY_ATTEMPTS + 1):
            try:
                # Try to connect to the server
                self.socket.connect((HOST, PORT))
                
                self.connected = True # Mark as connected
                return True # Connection successful

            except (ConnectionRefusedError, socket.timeout) as e:
                print(f"[Error] {e}")
                time.sleep(RETRY_DELAY)
        print(" Failed to connect after retries.")
        return False

    def send_command(self, command):
        # Send a command to the server and receive the response
        self.socket.sendall((command + "\n").encode()) # Send command
        return self.socket.recv(1024).decode() # Receive response

    def close(self):
        self.socket.close() # Close the socket connection
