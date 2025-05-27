FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the compiled classes
COPY bin/ /app/bin/

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-cp", "bin", "FileDownloaderServer"] 