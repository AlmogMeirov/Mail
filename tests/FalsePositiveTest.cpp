#include "gtest/gtest.h"
#include "../src/BloomFilter.h"
#include "../src/HashStd.h"
#include "../src/HashDouble.h"
#include "../src/UrlStorage.h"

#include <memory>

TEST(BloomFilterTest, DetectsFalsePositiveUsingUrlStorage) {
    // Step 1: Create a Bloom Filter with 2 hash functions
    std::vector<std::shared_ptr<HashFunction>> hash_funcs = {
        std::make_shared<HashStd>(),
        std::make_shared<HashDouble>()
    };

    BloomFilter filter(128, hash_funcs);

    std::string insertedUrl = "https://example.com/added";
    std::string notInsertedUrl = "https://example.com/not-added";

    // Step 2: Add only the first URL
    filter.add(insertedUrl);

    // Step 3: Run Bloom Filter check on a different URL
    bool bloomResult = filter.possiblyContains(notInsertedUrl);

    if (bloomResult) {
        // Step 4: Check against real blacklist using UrlStorage
        UrlStorage storage("data/urls.txt");
        bool reallyExists = storage.contains(notInsertedUrl);

        EXPECT_FALSE(reallyExists)
            << "False positive detected: BloomFilter returned true but URL not found in real blacklist";
    } else {
        SUCCEED() << "Correct behavior: BloomFilter returned false for URL not added";
    }
}
