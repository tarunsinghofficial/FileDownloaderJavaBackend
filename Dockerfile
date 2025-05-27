FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the compiled classes
COPY bin/ /app/bin/

# Create lib directory (instead of copying, since it might be empty)
RUN mkdir -p /app/lib

# Create downloads directory
RUN mkdir -p /app/downloads

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application with classpath including lib directory
# Use ":" as path separator for Linux environments
CMD ["java", "-cp", "bin:lib/*", "FileDownloaderServer"]