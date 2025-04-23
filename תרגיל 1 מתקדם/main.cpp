#include "bloom_setup.h"
#include <iostream>
#include <string>
#include "BloomFilter.h"
#include "HashStd.h"
#include "HashDouble.h"

int main() {
    // Prompt user to enter Bloom‑filter configuration
    std::cout << "Enter config line (e.g. 256 std double): ";
    std::string line;
    std::getline(std::cin, line);

    // Parse the config line, create the filter
    BloomFilter bf = createFromConfigLine(line);
    std::cout << "success!\n";

    // Interactive command loop
    // Supported commands:
    //   add <url>    — insert URL into filter
    //   check <url>  — test membership
    //   exit         — quit program
    while (true) {
        std::cout << "> ";

        std::string cmd, url;
        if (!(std::cin >> cmd)) {
            // EOF or input error: exit loop
            break;
        }
        if (cmd == "exit") {
            // User requested exit
            break;
        }

        // Read the URL argument
        if (!(std::cin >> url)) {
            std::cout << "missing URL\n";
            continue;
        }

        if (cmd == "add") {
            // Mark bits for this URL
            bf.add(url);
            std::cout << "added\n";
        } else if (cmd == "check") {
            // Test membership
            bool ok = bf.possiblyContains(url);
            if (ok) {
                // Might be present (could be false positive)
                std::cout << "maybe in filter\n";
            } else {
                // Definitely not present
                std::cout << "definitely not in filter\n";
            }
        } else {
            // Unknown command
            std::cout << "unknown command\n";
        }
    }

    return 0;
}
