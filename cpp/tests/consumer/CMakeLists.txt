# Smoke test: use the library as a subproject, the way FetchContent users do.
cmake_minimum_required(VERSION 3.18)
project(consumer LANGUAGES CXX)
add_subdirectory(../.. cienciaid)
add_executable(app main.cpp)
target_link_libraries(app PRIVATE cienciaid::cienciaid)
enable_testing()
add_test(NAME smoke COMMAND app)
