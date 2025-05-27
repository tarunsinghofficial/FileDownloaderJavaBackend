FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the compiled classes
COPY bin/ /app/bin/

# Copy the lib directory if there are any JAR dependencies
COPY lib/ /app/lib/

# Create downloads directory
RUN mkdir -p /app/downloads

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application with classpath including lib directory
CMD ["java", "-cp", "bin:lib/*", "FileDownloaderServer"]