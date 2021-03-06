cmake_minimum_required(VERSION 3.4.1)

#SET(CMAKE_C_COMPILER /path/to/c/compiler)
#SET(CMAKE_CXX_COMPILER /path/to/cpp/compiler)
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -std=c++11 -F/Library/Frameworks -pthread")

project(MoneroJavaJni)

#############
# System
#############

set(MONERO_CPP "${CMAKE_SOURCE_DIR}/external/monero-cpp")
message(STATUS MONERO_CPP : ${MONERO_CPP} : ${MONERO_CPP})

set(MONERO_CPP_SRC "${MONERO_CPP}/src")
set(MONERO_CORE ${MONERO_CPP}/external/monero-core)
set(MONERO_CORE_SRC "${MONERO_CORE}/src")
set(BOOST ${MONERO_CPP}/external/boost-sdk)

# java header includes
include_directories("$ENV{JAVA_HOME}")
include_directories("$ENV{JAVA_HOME}/include")
if (APPLE)
  include_directories("$ENV{JAVA_HOME}/include/darwin")
elseif (WIN32)
  include_directories("$ENV{JAVA_HOME}/include/win32")
else()
  include_directories("$ENV{JAVA_HOME}/include/linux")
endif()

# monero core header includes
include_directories("${MONERO_CPP}/external/libsodium/include/sodium")
include_directories("${MONERO_CPP}/external/openssl-sdk/include")
include_directories("${MONERO_CPP_SRC}/")
include_directories("${MONERO_CORE}/contrib/epee/include")
include_directories("${MONERO_CORE}/external/")
include_directories("${MONERO_CORE}/external/easylogging++")
include_directories("${MONERO_CORE}/external/rapidjson/include")
include_directories("${MONERO_CORE_SRC}/")
include_directories("${MONERO_CORE_SRC}/crypto")
include_directories("${MONERO_CORE_SRC}/crypto/crypto_ops_builder/include/")
include_directories("${MONERO_CORE_SRC}/wallet")
include_directories("${MONERO_CORE_SRC}/wallet/api")
include_directories(${BOOST})

# TODO: remove TRUEs, how are APPLE, DEPENDS, etc initialized?
if (TRUE OR HIDAPI_FOUND OR LibUSB_COMPILE_TEST_PASSED)
  if (TRUE OR APPLE)
    if(TRUE OR DEPENDS)
      list(APPEND EXTRA_LIBRARIES "-framework Foundation -framework IOKit")
    else()
      find_library(COREFOUNDATION CoreFoundation)
      find_library(IOKIT IOKit)
      list(APPEND EXTRA_LIBRARIES ${IOKIT})
      list(APPEND EXTRA_LIBRARIES ${COREFOUNDATION})
    endif()
  endif()
  if (WIN32)
    list(APPEND EXTRA_LIBRARIES setupapi)
  endif()
endif()

#list(APPEND EXTRA_LIBRARIES "-framework Foundation -framework IOKit")
message(STATUS EXTRA_LIBRARIES: ${EXTRA_LIBRARIES})

######################
# monero-cpp
######################

#include_directories(${BOOST})
add_library(monero-cpp SHARED IMPORTED)

# import shared c++ library
if (APPLE)
  set_target_properties(monero-cpp PROPERTIES IMPORTED_LOCATION ./libmonero-cpp.dylib)
elseif (WIN32)
  set_target_properties(monero-cpp PROPERTIES IMPORTED_LOCATION ./libmonero-cpp.dll)
else()
  set_target_properties(monero-cpp PROPERTIES IMPORTED_LOCATION ./libmonero-cpp.so)
endif()

###############################################
# Build Java dynamic library (.dylib) for JNI
###############################################

set(
    MONERO_JNI_SRC_FILES
    src/main/cpp/monero_wallet_jni_bridge.cpp
    src/main/cpp/monero_utils_jni_bridge.cpp
)
add_library(monero-java SHARED ${MONERO_JNI_SRC_FILES})

target_link_libraries(monero-java
	monero-cpp
	${EXTRA_LIBRARIES}
)

# search for libmonero-cpp.dylib in same directory as libmonero-java.dylib on mac for portability
# command: install_name_tool -add_rpath @loader_path/ ./libmonero-java.dylib 
if (APPLE)
	add_custom_command(TARGET monero-java 
	    POST_BUILD COMMAND 
	    ${CMAKE_INSTALL_NAME_TOOL} -add_rpath "@loader_path/"
	    $<TARGET_FILE:monero-java>)
endif()