#GCC compiler with g++ and make pre-installed
FROM gcc:12

#Install Cmake
RUN apt-get update && apt-get install -y cmake

#Set the working directory inside the container
WORKDIR /app

#Copy all project files from the host into the container's working directory
COPY . .

#Generate the build system using Cmake
RUN cmake -S . -B build

#Compile the project
RUN cmake --build build

# Run main
CMD ["./build/main_exec"]
