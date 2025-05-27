import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloaderServer {
    private static final int PORT = 8080;
    private static final String DOWNLOAD_DIR = "downloads/";

    public static void main(String[] args) throws Exception {
        // Create downloads directory if it doesn't exist
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // CORS handler for all requests
        server.createContext("/", new CORSHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/status", new StatusHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("File Downloader Server started on port " + PORT);
        System.out.println("Access the API at: http://localhost:" + PORT);
        System.out.println("Downloads will be saved to: " + new File(DOWNLOAD_DIR).getAbsolutePath());
    }

    // CORS Handler to handle preflight requests
    static class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            // For non-OPTIONS requests, send 404 if no specific handler matches
            String response = "File Downloader API Server is running!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Download Handler
    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Only POST method is allowed", 405);
                return;
            }

            try {
                // Read request body
                InputStream is = exchange.getRequestBody();
                String requestBody = readInputStream(is);

                // Parse JSON manually (simple parsing)
                String url = extractUrlFromJson(requestBody);

                if (url == null || url.trim().isEmpty()) {
                    sendErrorResponse(exchange, "URL is required", 400);
                    return;
                }

                System.out.println("Starting download for URL: " + url);

                // Get file information
                URL fileUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                String fileName = getFileNameFromUrl(url);
                long fileSize = connection.getContentLengthLong();
                String contentType = connection.getContentType();

                // Set response headers for file download
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                exchange.getResponseHeaders().add("Content-Type",
                        contentType != null ? contentType : "application/octet-stream");
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(fileSize));

                // Stream the file directly to the client
                connection = (HttpURLConnection) fileUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);

                try (InputStream inputStream = connection.getInputStream();
                        OutputStream outputStream = exchange.getResponseBody()) {

                    exchange.sendResponseHeaders(200, fileSize);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

            } catch (Exception e) {
                System.err.println("Download error: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Download failed: " + e.getMessage(), 500);
            }
        }

        private String getFileNameFromUrl(String fileUrl) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = "download_" + System.currentTimeMillis();
            }
            return fileName;
        }
    }

    // Status Handler for health check
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            String response = "{\"status\": \"Server is running\", \"port\": " + PORT + "}";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Utility methods
    private static String readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString("UTF-8");
    }

    private static String extractUrlFromJson(String json) {
        // Simple JSON parsing - look for "url" field
        try {
            int urlIndex = json.indexOf("\"url\"");
            if (urlIndex == -1)
                return null;

            int colonIndex = json.indexOf(":", urlIndex);
            int startQuote = json.indexOf("\"", colonIndex);
            int endQuote = json.indexOf("\"", startQuote + 1);

            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    private static void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        String jsonResponse = "{\"success\": false, \"error\": \"" + message + "\"}";
        exchange.sendResponseHeaders(statusCode, jsonResponse.length());
        OutputStream os = exchange.getResponseBody();
        os.write(jsonResponse.getBytes());
        os.close();
    }
}