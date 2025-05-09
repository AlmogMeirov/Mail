from tcp_client import TCPClient
import socket
import sys


def main():
    client = TCPClient()
    if client.connect():
        print("Connected successfully. Ready to send commands.")

        try:
            while True:
                try:
                    command = input("> ").strip()

                    if command.lower() == "quit":
                        print("User requested graceful shutdown.")
                        break

                    if command.lower() == "exit":
                        print("System-level shutdown requested.")
                        break

                    if not command:
                        continue  # Skip empty input

                    # Try to send the command
                    try:
                        client.sock.sendall(command.encode("utf-8"))
                    except (BrokenPipeError, ConnectionResetError, OSError) as e:
                        print(f"[Error] Failed to send command: {e}")
                        break

                    # Try to receive the response
                    try:
                        client.sock.settimeout(5.0)
                        response = client.sock.recv(1024).decode("utf-8").strip()

                        if not response:
                            print("[Warning] Empty response from server.")
                        elif response.lower() in {
                            "true",
                            "false",
                            "true true",
                            "true false",
                        }:
                            print(f"[Server] {response}")
                        else:
                            print(f"[Warning] Unexpected server response: {response}")

                    except socket.timeout:
                        print("[Error] Timeout while waiting for server response.")
                        break
                    except (ConnectionResetError, OSError) as e:
                        print(f"[Error] Failed to receive response: {e}")
                        break
                    finally:
                        client.sock.settimeout(None)

                except Exception as e:
                    # General safety net — catches anything not already handled
                    print(f"[Unhandled Error] {e}")
                    break

        except (KeyboardInterrupt, EOFError):
            # Ctrl+C or EOF (e.g. user closed terminal)
            print("\n[Info] Interrupted by user.")

        finally:
            if client.sock:
                try:
                    client.sock.shutdown(
                        socket.SHUT_RDWR
                    )  # Gracefully terminate connection in both directions
                except OSError:
                    pass  # Socket might already be closed
                finally:
                    client.sock.close()  # Fully close the socket and release resources
            print("Connection closed. Resources released.")

    else:
        print("Failed to connect. Exiting.")


if __name__ == "__main__":
    main()
