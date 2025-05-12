#include "TCP_Server.h"

int main(int argc, char* argv[])
{
    int port;
    if (argc > 1) {
        port = std::atoi(argv[1]);
    }
    // Create Server instance
    TCPServer server(port); // Port from command line argument
    server.run();           // Create the socket
    return 0;
}