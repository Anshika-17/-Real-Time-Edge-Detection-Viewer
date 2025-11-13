cmake_minimum_required(VERSION 3.22.1)
project(native_lib)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(native-lib SHARED
    native-lib.cpp)

find_package(OpenCV REQUIRED core imgproc)

find_library(log-lib log)

include_directories(${OpenCV_INCLUDE_DIRS})

target_link_libraries(
    native-lib
    ${OpenCV_LIBS}
    ${log-lib}
)
