FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the compiled classes and dependencies
COPY bin/ /app/bin/
COPY lib/ /app/lib/

# Expose the port your application runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-cp", "bin:lib/*", "FileDownloaderServer"] 