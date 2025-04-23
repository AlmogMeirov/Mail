#ifndef BLOOM_SETUP_H
#define BLOOM_SETUP_H

#include "BloomFilter.h"
#include <memory>
#include <vector>
#include <string>

// return Configured BloomFilter instance.
BloomFilter createFromConfigLine(const std::string& line);

// Parses a user‐supplied configuration line (e.g. “256”) and
// constructs a BloomFilter with the specified bit size
BloomFilter createFromConfigLine(const std::string& line);

#endif
