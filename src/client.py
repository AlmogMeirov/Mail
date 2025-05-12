from tcp_client import TCPClient # Import the TCP client class


def main():
    client = TCPClient() # Create a TCP client instance
    if not client.connect(): # Attempt to connect to the server
        print("Failed to connect. Exiting.")
        return

    try:
        while True:
            command = input().strip()  # Read user input and strip whitespace
            if not command:# Skip empty commands
                continue
            response = client.send_command(command) # Send command to the server
            if not response: # Check if the server closed the connection
                print("Server closed the connection.")
                break
            print(f"{response}", end="") # Print the server's response
    except KeyboardInterrupt: # Handle Ctrl+C gracefully
        print("")
    finally:
        client.close() # Ensure the client connection is closed


if __name__ == "__main__":
    main() # Run the main function if the script is executed directly
