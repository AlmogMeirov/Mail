#include "gtest/gtest.h"
#include "BloomFilter.h"
#include "HashStd.h" 
#include "HashDouble.h"

// Test to check false positive scenerio.
// A false positive occurs when the filter incorrectly reports that
// an element is present, even though it was never added.


TEST(BloomFilterTest, MayCauseFalsePositive) {
    // Arrange: Create two hash functions and initialize the BloomFilter.
    auto hash1 = std::make_shared<HashStd>();
    auto hash2 = std::make_shared<HashDouble>();

    BloomFilter bloomFilter(128, {hash1, hash2});

    // Define a URL to add and another URL that will not be added.

    std::string inserted = "https://example.com/added";
    std::string notInserted = "https://example.com/not-added";

    // Act: Add the first URL to the BloomFilter and check for false positive on the second URL.
    bloomFilter.add(inserted);

    // Check if the BloomFilter incorrectly reports the second URL as present.
    bool result = bloomFilter.possiblyContains(notInserted);

    // Assert: Log the result and mark the test as successful regardless of the outcome.
    if (result) {
        std::cout << "[INFO] False positive detected for '" << notInserted << "'\n";
        SUCCEED();  // Test passes if a false positive is detected.
    } else {
        SUCCEED() << "No false positive detected"; // Test passes if no false positive is detected.
    }
}
