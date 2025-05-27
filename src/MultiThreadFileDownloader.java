import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class MultiThreadFileDownloader {
    private static final int THREAD_COUNT = 4;
    private static final int BUFFER_SIZE = 8192;
    private static final int TIMEOUT_SECONDS = 30;

    private void setCommonHeaders(HttpURLConnection connection) {
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");
    }

    public String downloadFile(String fileUrl, String downloadDir) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            setCommonHeaders(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            long fileSize = connection.getContentLengthLong();
            String fileName = getFileNameFromUrl(fileUrl);
            String acceptRanges = connection.getHeaderField("Accept-Ranges");

            System.out.println("File: " + fileName);
            System.out.println("Size: " + fileSize + " bytes");
            System.out.println("Supports range requests: " + "bytes".equals(acceptRanges));

            File outputFile = new File(downloadDir, fileName);

            // If server doesn't support range requests or file is small, download normally
            if (!"bytes".equals(acceptRanges) || fileSize < THREAD_COUNT * 1024 * 1024) {
                System.out.println("Using single-threaded download...");
                downloadSingleThreaded(fileUrl, outputFile);
            } else {
                System.out.println("Using multi-threaded download with " + THREAD_COUNT + " threads...");
                downloadMultiThreaded(fileUrl, outputFile, fileSize);
            }

            System.out.println("Download completed: " + outputFile.getAbsolutePath());
            return "File downloaded successfully: " + fileName + " (" + fileSize + " bytes)";

        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }

    private void downloadSingleThreaded(String fileUrl, File outputFile) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        setCommonHeaders(connection);

        try (InputStream in = connection.getInputStream();
                FileOutputStream out = new FileOutputStream(outputFile);
                BufferedInputStream bis = new BufferedInputStream(in);
                BufferedOutputStream bos = new BufferedOutputStream(out)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (totalBytesRead % (1024 * 1024) == 0) {
                    System.out.println("Downloaded: " + (totalBytesRead / 1024 / 1024) + " MB");
                }
            }
        }
    }

    private void downloadMultiThreaded(String fileUrl, File outputFile, long fileSize) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Create temporary files for each chunk
        File[] tempFiles = new File[THREAD_COUNT];
        long chunkSize = fileSize / THREAD_COUNT;

        for (int i = 0; i < THREAD_COUNT; i++) {
            long startByte = i * chunkSize;
            long endByte = (i == THREAD_COUNT - 1) ? fileSize - 1 : (startByte + chunkSize - 1);

            tempFiles[i] = new File(outputFile.getParent(), outputFile.getName() + ".part" + i);

            DownloadTask task = new DownloadTask(fileUrl, tempFiles[i], startByte, endByte, i);
            futures.add(executor.submit(task));
        }

        // Wait for all downloads to complete
        executor.shutdown();
        boolean allCompleted = executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!allCompleted) {
            executor.shutdownNow();
            throw new RuntimeException("Download timed out after " + TIMEOUT_SECONDS + " seconds");
        }

        // Check if all downloads were successful
        for (int i = 0; i < futures.size(); i++) {
            if (!futures.get(i).get()) {
                throw new RuntimeException("Download failed for chunk " + i);
            }
        }

        // Merge all chunks into final file
        mergeFiles(tempFiles, outputFile);

        // Clean up temporary files
        for (File tempFile : tempFiles) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void mergeFiles(File[] tempFiles, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            for (int i = 0; i < tempFiles.length; i++) {
                System.out.println("Merging chunk " + (i + 1) + "/" + tempFiles.length);

                try (FileInputStream fis = new FileInputStream(tempFiles[i]);
                        BufferedInputStream bis = new BufferedInputStream(fis)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;

                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        System.out.println("All chunks merged successfully!");
    }

    private String getFileNameFromUrl(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        // Remove query parameters if present
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        // If no filename found, use a default
        if (fileName.isEmpty() || !fileName.contains(".")) {
            fileName = "download_" + System.currentTimeMillis();
        }

        return fileName;
    }
}