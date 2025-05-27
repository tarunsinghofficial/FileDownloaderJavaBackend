## File Downloader Java Project - README

### Overview

This project is a **multi-threaded file downloader** with a **Java backend** and a **web-based frontend**. The backend exposes a simple HTTP API allowing users to input a file URL, download it via the backend, and show the status via the frontend.

### Features

#### Backend

* **HTTP API**: `/download` and `/status` endpoints.
* **CORS Support**: Enables frontend on different origins to interact.
* **Multi-threaded Downloading**: Uses HTTP Range headers for parallel chunk downloading.
* **Fallback to Single-threaded**: For servers not supporting range requests.
* **Progress Logging**: Console logs for monitoring.
* **Robust Error Handling**: Handles HTTP/network errors and retries.
* **Automatic Directory Management**: Creates a download folder if missing.
* **Browser-like Headers**: Prevents getting blocked by some servers.

#### Frontend

* **Modern UI**: Responsive and clean interface.
* **Status Feedback**: Shows whether backend is running and displays download results.

---

### Backend Directory Structure

* `FileDownloaderServer.java`
* `MultiThreadFileDownloader.java`
* `DownloadTask.java`
* `HttpUtils.java`

---

### Code File Explanations

#### 1. FileDownloaderServer.java

**Purpose**: Main backend server class.

**Responsibilities**:

* Starts an HTTP server on port 8080.
* Handles `/`, `/download`, and `/status` routes.
* Parses JSON from POST requests.
* Sets up CORS headers.
* Initiates file downloads via `MultiThreadFileDownloader`.

---

#### 2. MultiThreadFileDownloader.java

**Purpose**: Contains core logic for downloading files.

**Responsibilities**:

* Checks if the server supports HTTP Range requests.
* Splits the download into chunks (multi-threaded) if supported.
* Falls back to a single-threaded download if not.
* Merges all downloaded chunks into the final file.
* Adds headers to mimic browser requests.

**Methods**:

* `downloadFile(fileUrl, downloadDir)`: Entry point from server.
* `downloadMultiThreaded(...)`: Uses threads for downloading chunks.
* `downloadSingleThreaded(...)`: Normal download.
* `mergeFiles(...)`: Combines chunk files.

---

#### 3. DownloadTask.java

**Purpose**: Represents a single chunk download.

**Responsibilities**:

* Downloads a specific byte range.
* Implements retry logic.
* Uses Callable for concurrent thread execution.
* Writes chunk to temporary file.

---

#### 4. HttpUtils.java

**Purpose**: (Currently a placeholder) Utility file for HTTP related functions. Can be expanded in future.

---

### How to Build Backend (Step-by-Step for Beginners)

#### Step 1: Create Files in this Order

1. `DownloadTask.java` – Core unit of chunk downloader.
2. `MultiThreadFileDownloader.java` – Uses DownloadTask, creates logic.
3. `HttpUtils.java` – Leave mostly blank or add future utilities.
4. `FileDownloaderServer.java` – Entry point that connects frontend and logic.

#### Step 2: Compile

Use `compile.bat` (contains `javac *.java`) to compile all classes.

#### Step 3: Run

Use `run.bat` (contains `java FileDownloaderServer`) to start server at `http://localhost:8080`

---

### API Usage

#### POST /download

**Body**:

```json
{
  "url": "https://example.com/file.jpg"
}
```

**Response**:

```json
{
  "success": true,
  "message": "File downloaded successfully: file.jpg (12345 bytes)"
}
```

#### GET /status

**Response**:

```json
{
  "status": "Server is running",
  "port": 8080
}
```

---

### How Downloading Works

#### Basic Flow

1. User enters URL in frontend.
2. Frontend sends a POST to backend `/download`.
3. Backend checks if server supports range requests.
4. If yes → Split into threads to download parts of the file.
5. If not → Download file normally.
6. All parts (if any) are merged and saved.
7. Result is returned to frontend.

---
