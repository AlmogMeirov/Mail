#include "bloom_setup.h"
#include <iostream>
#include <string>
#include <cassert>
#include <memory>       
#include <vector>
#include <sstream>
#include "BloomFilter.h"
#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"

int main() {
    std::cout << "Enter config line (e.g. 256 std double): ";
    std::string line;
    std::getline(std::cin, line);
    BloomFilter bf = createFromConfigLine(line);
    std::cout << "success!\n";

    // לולאת פקודות אינטראקטיבית:
    while (true) {
        std::cout << "> ";
        std::string cmd, url;
        if (!(std::cin >> cmd)) break;          // EOF או שגיאה
        if (cmd == "exit") break;               // יציאה
        std::cin >> url;                        // קרא את ה‑URL
        if (cmd == "add") {
            bf.add(url);
            std::cout << "added\n";
        } else if (cmd == "check") {
            bool ok = bf.possiblyContains(url);
            std::cout << (ok
                ? "maybe in filter\n"
                : "definitely not in filter\n");
        } else {
            std::cout << "unknown command\n";
        }
    }
    return 0;
}
