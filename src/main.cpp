#include "TCP_Server.h"

int main()
{
    // Create Server instance
    TCPServer server(1234); // Port 1234
    server.run();           // Create the socket
    return 0;
}