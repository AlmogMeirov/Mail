cmake_minimum_required(VERSION 3.10)

project(BloomFilterProject)

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
    src/*.cpp
    src/*.h
)

# test files
set(TESTS
     tests/*.cpp
)
# Build the test executable
add_executable(runTests ${SOURCES} ${TESTS})
target_link_libraries(runTests gtest_main pthread)

# Build main executable
add_executable(main_exec
    src/main.cpp
    ${SOURCES}
)
target_link_libraries(main_exec pthread)
