cmake_minimum_required(VERSION 3.10)

# Project name
project(MailProject)

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# Include directories
include_directories(src)

# Source files
set(SOURCES
    src/bloom_setup.cpp
    src/BloomFilter.cpp
    src/main.cpp
    src/TCP_Server.cpp
    src/URLChecker.cpp
    src/UrlListUtils.cpp
    src/UrlStorage.cpp
    src/InfiniteCommandLoop.cpp
)

# Add executable
add_executable(MailProject ${SOURCES}) # Add the source files to the executable
target_link_libraries(MailProject pthread)# Link with pthread	

# Link libraries

#Gtest
include(FetchContent)  # Enables downloading external dependencies

# Download and make GoogleTest available
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/release-1.12.1.zip
)

# Make the content available
FetchContent_MakeAvailable(googletest)

# Force shared CRT on Windows (ignored on Linux)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

enable_testing()

# Macro to simplify test target creation
macro(add_gtest name file)
  add_executable(${name} ${file})
  target_link_libraries(${name} gtest gtest_main pthread)
  add_test(NAME ${name} COMMAND ${name})
endmacro()

# Add test executables
add_gtest(FalsePositiveTest src/tests/FalsePositiveTest.cpp)
add_gtest(OutputStringTest src/tests/OutputStringTest.cpp)
add_gtest(PersistenceTest src/tests/PersistenceTest.cpp)
add_gtest(TestBloomFilter src/tests/test_bloom_filter.cpp)
add_gtest(TestConfigParser src/tests/test_config_parser.cpp)
add_gtest(TestUrlList src/tests/test_url_list.cpp)
add_gtest(TestOperateBloomFilter src/tests/TestOperateBloomFilter.cpp)
add_gtest(TrueNegativeTest src/tests/TrueNegativeTest.cpp)
add_gtest(TrueNegativeTwoHashTest src/tests/TrueNegativeTwoHashTest.cpp)
add_gtest(TruePositiveTest src/tests/TruePositiveTest.cpp)
add_gtest(TruePositiveTwoHashTest src/tests/TruePositiveTwoHashTest.cpp)

# Add tests
#enable_testing()
#add_executable(FalsePositiveTest src/tests/FalsePositiveTest.cpp)
#add_executable(OutputStringTest src/tests/OutputStringTest.cpp)
#add_executable(PersistenceTest src/tests/PersistenceTest.cpp)
#add_executable(TestBloomFilter src/tests/test_bloom_filter.cpp)
#add_executable(TestConfigParser src/tests/test_config_parser.cpp)
#add_executable(TestUrlList src/tests/test_url_list.cpp)
#add_executable(TestOperateBloomFilter src/tests/TestOperateBloomFilter.cpp)
#add_executable(TrueNegativeTest src/tests/TrueNegativeTest.cpp)
#add_executable(TrueNegativeTwoHashTest src/tests/TrueNegativeTwoHashTest.cpp)
#add_executable(TruePositiveTest src/tests/TruePositiveTest.cpp)
#add_executable(TruePositiveTwoHashTest src/tests/TruePositiveTwoHashTest.cpp)