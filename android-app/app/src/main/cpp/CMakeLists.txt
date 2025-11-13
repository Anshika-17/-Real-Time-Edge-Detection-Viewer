cmake_minimum_required(VERSION 3.22.1)
project(native-lib)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build the native library
add_library(native-lib SHARED
        native-lib.cpp)

# ----- OpenCV .so files from jniLibs -----
set(OPENCV_LIB_PATH ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(opencv_java4 SHARED IMPORTED)

set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION ${OPENCV_LIB_PATH}/libopencv_java4.so)

# ----- OpenCV header include path -----
set(OPENCV_INCLUDE_DIR
        E:/opencv-4.8.0-android-sdk/OpenCV-android-sdk/sdk/native/jni/include)

include_directories(${OPENCV_INCLUDE_DIR})

# Android log
find_library(log-lib log)

# Link everything
target_link_libraries(
        native-lib
        opencv_java4
        ${log-lib}
)
