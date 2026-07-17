cmake_minimum_required(VERSION 3.22.1)
project("nekodaemon")

add_library(nekodaemon SHARED daemon.cpp)

target_compile_options(nekodaemon PRIVATE -std=c++17)
target_link_libraries(nekodaemon PRIVATE log)
