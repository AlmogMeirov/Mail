#include "BloomFilter.h"
#include <fstream>
#include <iostream> // Includes the standard C++ I/O library

// Adds a string (URL) to the Bloom Filter by marking bits based on hash functions
void BloomFilter::add(const std::string& url) {
    for (const auto& hash : hash_functions) {
        size_t idx = (*hash)(url) % size;// Apply the hash function, get index within bounds
        bit_array[idx] = true; // Set the bit at that index to true
    }
}


// Checks if a string (URL) might be in the Bloom Filterand returns true only if all bits for all hash functions are set
// False means definitely not present, true means "maybe" 
bool BloomFilter::possiblyContains(const std::string& url) const {
    for (const auto& hash : hash_functions) {
        size_t idx = (*hash)(url) % size; // Apply the hash and get index
        if (!bit_array[idx]) return false; // If even one bit is 0, URL is definitely not in the filter
    }
    return true; // URL might be in the filter
}

// Loads the Bloom Filter from a file. If the file is missing or damaged,
// it starts with an empty filter and sets the hash functions.
bool BloomFilter::load_from_file(const std::string& filename,
                                 const std::vector<std::shared_ptr<HashFunction>>& hash_funcs) {
    // Try to open the file in binary mode for reading
    std::ifstream infile(filename, std::ios::binary);
    // If the file doesn't exist, start with an empty filter
    if (!infile) {
        std::cerr << "File not found: " << filename << ". Initializing empty filter." << std::endl;
        initialize_empty_filter(); // Create empty Bloom filter
        hash_functions = hash_funcs; // Set the provided hash functions
        return false; // Loading fail indicator
    }


    try {
        // Read the first part of the file
        infile.read(reinterpret_cast<char*>(&this->size), sizeof(size_t));
        if (!infile) throw std::runtime_error("Failed to read size");
        // Resize the bit array to match the size read from fil
        bit_array.resize(size);
        // Read the bit values one by one
        for (size_t i = 0; i < size; ++i) {
            char bit;
            infile.read(&bit, 1);  // Read a single byte
            if (!infile) throw std::runtime_error("Failed to read bit array");
            bit_array[i] = (bit != 0); // Convert byte to bool
        }

        hash_functions = hash_funcs;  // Set the hash functions
        return true; // Successfully loaded
        // If any reading error occurs, catch and handle it
    }
    catch (const std::exception& e) {
        std::cerr << "[Error] Corrupted file: " << e.what() << ". Reinitializing." << std::endl;
        initialize_empty_filter();  // Start with an empty filter
        hash_functions = hash_funcs;  // Set the hash functions again
        return false; // Indicate that loading failed
    }
}

// Saves the current Bloom Filter to a binary file and stores the size followed by the bit values.
// This allows the filter to be restored on future runs.
void BloomFilter::save_to_file(const std::string& filename) const {
    // Open the file in binary mode for writing
    std::ofstream outfile(filename, std::ios::binary);
    // If the file cannot be opened, report an error and exit the function
    if (!outfile) {
        std::cerr << "[Error] Failed to open file for writing: " << filename << std::endl;
        return;
    }

    // Write the size of the bit array
    outfile.write(reinterpret_cast<const char*>(&this->size), sizeof(size_t));

    // Write each bit value (true/false) as a single byte
    for (bool bit : bit_array) {
        char b = bit ? 1 : 0;
        outfile.write(&b, 1);
    }
}

// Initializes the Bloom Filter with a default size and clears the bit array.
void BloomFilter::initialize_empty_filter() {
    size = 256;  // Default size, can be adjusted
    bit_array.assign(size, false);
}