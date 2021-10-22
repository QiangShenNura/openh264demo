# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("openh264demo")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -g")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g")
#SET( CMAKE_EXE_LINKER FLAGS "-Wl,--build-id ${CMAKE_EXE_LINKER_FLAGS}")

set(CMAKE_CXX_FLAGS  "$ENV{CXXFLAGS}  -fms-extensions")
# 支持gnu11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
set(openh264_lib_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(openh264_head_dir ${CMAKE_SOURCE_DIR}/openh264)
include_directories(${openh264_head_dir}/include)


add_library(libopenh264 SHARED IMPORTED)
set_target_properties(libopenh264 PROPERTIES IMPORTED_LOCATION
        ${openh264_lib_DIR}/libopenh264.so)

#添加子目录，将自动找到子目录中的CMakeLists.txt
ADD_SUBDIRECTORY(${CMAKE_SOURCE_DIR}/libyuv)

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp
             VideoEncoder.cpp
             utils/logger.h
             utils/timer.c
             #exports.cpp
             #aac-enc.c
        )


include_directories(${openh264_head_dir}/include)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

target_include_directories(native-lib PRIVATE
        ${openh264_DIR}/include
        libyuv/include
        #android_aac_enc
        )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                        libopenh264
                        #aac_enc
                        libyuv
                       # Links the target library to the log library
                       # included in the NDK.
                        #使用安卓的头文件,如ANativeWindow
                        -landroid
                       ${log-lib} )