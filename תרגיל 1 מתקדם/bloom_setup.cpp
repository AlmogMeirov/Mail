#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"

#include <sstream>

/**
 *
 * If the size is invalid, a default of 128 is used.
 * If no valid hash function names are provided, std::hash is used by default.
 *
 * @param line A space‐separated string, e.g. "256 std double".
 * @return A BloomFilter initialized with the parsed size and hash functions.
 */
BloomFilter createFromConfigLine(const std::string& line) {
    // Wrap the input line in a stringstream for easy token extraction
    std::stringstream ss(line);

    // Default to 128 bits if parsing fails or value out of bounds
    size_t bit_array_size = 128;
    bool valid_size = static_cast<bool>(ss >> bit_array_size);

    // Validate parsed size: must be nonzero and ≤ 1,000,000
    if (!valid_size || bit_array_size == 0 || bit_array_size > 1000000) {
        bit_array_size = 128;  // fallback to default
    }

    // Container for pointers to chosen hash functions
    std::vector<std::shared_ptr<HashFunction>> hash_funcs;
    std::string funcName;

    // Read each remaining word and map it to a hash function
    while (ss >> funcName) {
        if (funcName == "std") {
            // Use the standard library hash<string>
            hash_funcs.push_back(std::make_shared<HashStd>());
        } else if (funcName == "double") {
            // Use our doubled‐salt hash
            hash_funcs.push_back(std::make_shared<HashDouble>());
        }
        // Unknown names are ignored silently
    }

    // If no valid hash names were provided, default to std::hash
    if (hash_funcs.empty()) {
        hash_funcs.push_back(std::make_shared<HashStd>());
    }

    // Construct and return the BloomFilter with the chosen size and hash functions
    return BloomFilter(bit_array_size, hash_funcs);
}
