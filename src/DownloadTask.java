import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<Boolean> {
    private final String fileUrl;
    private final File outputFile;
    private final long startByte;
    private final long endByte;
    private final int threadId;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 3;

    public DownloadTask(String fileUrl, File outputFile, long startByte, long endByte, int threadId) {
        this.fileUrl = fileUrl;
        this.outputFile = outputFile;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadId = threadId;
    }

    private void setCommonHeaders(HttpURLConnection connection) {
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");
    }

    @Override
    public Boolean call() {
        System.out.println("Thread " + threadId + " starting download: bytes " + startByte + "-" + endByte);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (downloadChunk()) {
                    System.out.println("Thread " + threadId + " completed successfully");
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Thread " + threadId + " attempt " + attempt + " failed: " + e.getMessage());

                if (attempt == MAX_RETRIES) {
                    System.err.println("Thread " + threadId + " failed after " + MAX_RETRIES + " attempts");
                    return false;
                }

                // Wait before retry
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    private boolean downloadChunk() throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set range header for partial content
        connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("GET");
        setCommonHeaders(connection);

        int responseCode = connection.getResponseCode();

        // Check for partial content or OK response
        if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode + " for thread " + threadId);
        }

        try (InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;
            long expectedBytes = endByte - startByte + 1;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Progress reporting for this thread
                if (totalBytesRead % (100 * 1024) == 0) { // Every 100KB
                    double progress = (double) totalBytesRead / expectedBytes * 100;
                    System.out.printf("Thread %d progress: %.1f%% (%d/%d bytes)%n",
                            threadId, progress, totalBytesRead, expectedBytes);
                }

                // Safety check to prevent infinite loop
                if (totalBytesRead > expectedBytes) {
                    break;
                }
            }

            System.out.println("Thread " + threadId + " downloaded " + totalBytesRead + " bytes");

            // Verify we got the expected amount of data
            if (totalBytesRead < expectedBytes && responseCode == HttpURLConnection.HTTP_PARTIAL) {
                System.err.println("Thread " + threadId + " warning: Expected " + expectedBytes +
                        " bytes but got " + totalBytesRead);
            }

            return totalBytesRead > 0;

        } finally {
            connection.disconnect();
        }
    }
}