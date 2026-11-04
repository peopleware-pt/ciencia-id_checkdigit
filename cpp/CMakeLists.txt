cmake_minimum_required(VERSION 3.18)
project(cienciaid VERSION 1.0.0 LANGUAGES CXX)

add_library(cienciaid INTERFACE)
add_library(cienciaid::cienciaid ALIAS cienciaid)
target_include_directories(cienciaid INTERFACE
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>)
target_compile_features(cienciaid INTERFACE cxx_std_17)

# Tests (and the nlohmann/json download) only when built as the top-level project.
if(CMAKE_CURRENT_SOURCE_DIR STREQUAL CMAKE_SOURCE_DIR)
  enable_testing()
  include(FetchContent)
  if(POLICY CMP0135)
    cmake_policy(SET CMP0135 NEW)
  endif()
  FetchContent_Declare(json
    URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz
    URL_HASH SHA256=d6c65aca6b1ed68e7a182f4757257b107ae403032760ed6ef121c9d55e81757d)
  FetchContent_MakeAvailable(json)

  add_executable(vectors_test tests/vectors_test.cpp)
  target_link_libraries(vectors_test PRIVATE cienciaid::cienciaid nlohmann_json::nlohmann_json)
  target_compile_definitions(vectors_test PRIVATE
    CIENCIAID_VECTORS="${CMAKE_CURRENT_SOURCE_DIR}/../test-vectors.json")
  if(MSVC)
    target_compile_options(vectors_test PRIVATE /utf-8)
  endif()
  add_test(NAME vectors COMMAND vectors_test)
endif()
