# CMAKE generated file: DO NOT EDIT!
# Generated by "MinGW Makefiles" Generator, CMake Version 3.12

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

SHELL = cmd.exe

# The CMake executable.
CMAKE_COMMAND = "C:\Program Files\JetBrains\CLion 2018.2.3\bin\cmake\win\bin\cmake.exe"

# The command to remove a file.
RM = "C:\Program Files\JetBrains\CLion 2018.2.3\bin\cmake\win\bin\cmake.exe" -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug

# Include any dependencies generated for this target.
include CMakeFiles/udp-server.dir/depend.make

# Include the progress variables for this target.
include CMakeFiles/udp-server.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/udp-server.dir/flags.make

CMakeFiles/udp-server.dir/main.c.obj: CMakeFiles/udp-server.dir/flags.make
CMakeFiles/udp-server.dir/main.c.obj: ../main.c
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug\CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building C object CMakeFiles/udp-server.dir/main.c.obj"
	C:\PROGRA~1\MINGW-~1\X86_64~1.0-W\mingw64\bin\gcc.exe $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -o CMakeFiles\udp-server.dir\main.c.obj   -c C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\main.c

CMakeFiles/udp-server.dir/main.c.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing C source to CMakeFiles/udp-server.dir/main.c.i"
	C:\PROGRA~1\MINGW-~1\X86_64~1.0-W\mingw64\bin\gcc.exe $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -E C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\main.c > CMakeFiles\udp-server.dir\main.c.i

CMakeFiles/udp-server.dir/main.c.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling C source to assembly CMakeFiles/udp-server.dir/main.c.s"
	C:\PROGRA~1\MINGW-~1\X86_64~1.0-W\mingw64\bin\gcc.exe $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -S C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\main.c -o CMakeFiles\udp-server.dir\main.c.s

# Object files for target udp-server
udp__server_OBJECTS = \
"CMakeFiles/udp-server.dir/main.c.obj"

# External object files for target udp-server
udp__server_EXTERNAL_OBJECTS =

udp-server.exe: CMakeFiles/udp-server.dir/main.c.obj
udp-server.exe: CMakeFiles/udp-server.dir/build.make
udp-server.exe: CMakeFiles/udp-server.dir/linklibs.rsp
udp-server.exe: CMakeFiles/udp-server.dir/objects1.rsp
udp-server.exe: CMakeFiles/udp-server.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug\CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking C executable udp-server.exe"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles\udp-server.dir\link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/udp-server.dir/build: udp-server.exe

.PHONY : CMakeFiles/udp-server.dir/build

CMakeFiles/udp-server.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles\udp-server.dir\cmake_clean.cmake
.PHONY : CMakeFiles/udp-server.dir/clean

CMakeFiles/udp-server.dir/depend:
	$(CMAKE_COMMAND) -E cmake_depends "MinGW Makefiles" C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug C:\Users\allen\Desktop\Workspace\Auburn\FALL2018\COMP4320\lab1\udp-server\UDPServer\UDPServer\cmake-build-debug\CMakeFiles\udp-server.dir\DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/udp-server.dir/depend
