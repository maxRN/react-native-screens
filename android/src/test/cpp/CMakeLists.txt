cmake_minimum_required(VERSION 3.16)

project(rnscreens_native_tests LANGUAGES CXX)

enable_testing()
find_package(Threads REQUIRED)

add_executable(
  thread_safe_shared_object_test
  ThreadSafeSharedObjectTest.cpp
)
target_compile_features(thread_safe_shared_object_test PRIVATE cxx_std_20)
target_link_libraries(thread_safe_shared_object_test PRIVATE Threads::Threads)
target_include_directories(
  thread_safe_shared_object_test
  PRIVATE
  ../../main/cpp
)

add_test(
  NAME thread_safe_shared_object_test
  COMMAND thread_safe_shared_object_test
)
