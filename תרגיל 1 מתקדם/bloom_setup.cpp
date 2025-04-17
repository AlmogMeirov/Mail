#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"

#include <sstream>

BloomFilter createFromConfigLine(const std::string& line) {
    std::stringstream ss(line);
    size_t bit_array_size = 128;
    bool valid_size = static_cast<bool>(ss >> bit_array_size);

    if (!valid_size || bit_array_size == 0 || bit_array_size > 1000000) {
        bit_array_size = 128;
    }

    std::vector<std::shared_ptr<HashFunction>> hash_funcs;
    std::string func;
    while (ss >> func) {
        if (func == "std") {
            hash_funcs.push_back(std::make_shared<HashStd>());
        } else if (func == "double") {
            hash_funcs.push_back(std::make_shared<HashDouble>());
        }
    }

    if (hash_funcs.empty()) {
        hash_funcs.push_back(std::make_shared<HashStd>());
    }

    return BloomFilter(bit_array_size, hash_funcs);
}
