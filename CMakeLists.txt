cmake_minimum_required(VERSION 3.10)

project(MyExample)

set(CMAKE_CXX_STANDARD 11)

include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/03597a01ee50ed33e9dfd640b249b4be3799d395.zip
)

# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

# source files
set(SOURCES
    src/BloomFilter.cpp
    src/UrlStorage.cpp
)

# test files
set(TESTS
    tests/FalsePositiveTest.cpp
)

add_executable(runTests ${SOURCES} ${TESTS})

target_link_libraries(runTests gtest_main)