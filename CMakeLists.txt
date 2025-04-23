cmake_minimum_required(VERSION 3.10)
project(BloomFilter)

set(CMAKE_CXX_STANDARD 11)

# הוספת ספריית GoogleTest
find_package(GTest REQUIRED)
include_directories(${GTEST_INCLUDE_DIRS})

# הוספת קוד המקור שלך
add_library(bloom 
    BloomFilter.cpp
    bloom_setup.cpp
    HashStd.cpp    # אם זה קיים כס .cpp
    HashDouble.cpp
)

# קובץ ה-main
add_executable(bloom_app main.cpp)
target_link_libraries(bloom_app bloom)

# בניית הבדיקות
add_executable(test_config_parser test_config_parser.cpp)
target_link_libraries(test_config_parser bloom ${GTEST_LIBRARIES} pthread)

add_executable(PersistenceTest PersistenceTest.cpp)
target_link_libraries(PersistenceTest bloom ${GTEST_LIBRARIES} pthread)

enable_testing()
add_test(NAME ConfigTests COMMAND test_config_parser)
add_test(NAME PersistenceTests COMMAND PersistenceTest)
