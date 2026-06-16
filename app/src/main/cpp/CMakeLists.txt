cmake_minimum_required(VERSION 3.22.1)
project("native_tts_bridge")

add_library(
    native-tts-bridge
    SHARED
    native_tts_bridge.cpp)

find_library(log-lib log)

target_link_libraries(
    native-tts-bridge
    ${log-lib})
