# Android Project Audit Report

## 1. Executive Summary

This audit identifies critical stability, performance, and compliance issues in the application. The most severe risks involve **OutOfMemoryErrors (OOM)** due to improper bitmap handling and **Application Not Responding (ANR)** errors caused by disk I/O on the main thread. Additionally, **Scoped Storage violations** will cause crashes or failures on Android 10+ devices.

**Overall Health Score:** 🚨 **CRITICAL** (Immediate Action Required)

---

## 2. Critical Issues (Crashes & ANRs)

### 2.1. Unsafe Bitmap Loading (OOM Risk)
*   **Severity:** **CRITICAL**
*   **File:** `app/src/main/java/com/chamundi/templete/PhotoshopEditorActivity.kt` (Line 92)
*   **File:** `app/src/main/java/com/chamundi/templete/utils/ImageGenerator.kt` (Line 158)
*   **Root Cause:** The app loads full-resolution images (e.g., 12MP+ camera photos) directly into memory using `BitmapFactory.decodeStream` without `inSampleSize`.
    *   In `PhotoshopEditorActivity`: `val bitmap = BitmapFactory.decodeStream(stream)`
    *   In `ImageGenerator`: `inSampleSize = 1` is explicitly set.
*   **Impact:** A single 12MP image (ARGB_8888) consumes ~48MB. Loading 2-3 layers or enhancing images (which creates copies) will exceed the heap size (typically 128MB-256MB on standard devices), causing `OutOfMemoryError` crashes.
*   **Fix:**
    1.  Read image dimensions first using `inJustDecodeBounds = true`.
    2.  Calculate an appropriate `inSampleSize` to downscale the image to the display or canvas size (e.g., max 2048x2048).
    3.  Decode using the calculated sample size.

### 2.2. Main Thread Disk I/O (ANR Risk)
*   **Severity:** **HIGH**
*   **File:** `app/src/main/java/com/chamundi/templete/PhotoshopEditorActivity.kt`
*   **Line(s):** 81 (Presets listing), 133 (`saveProject`), 147 (`exportAsPng`)
*   **Root Cause:** Heavy file operations are performed directly on the UI thread.
    *   `presetsDir.listFiles()` is called during Composable initialization.
    *   `ProjectSerializer.saveProject` and `ImageExporter.exportAsPng` (compression + disk write) block the main thread.
*   **Impact:** The UI will freeze during these operations. If they take >5 seconds (common for large PNG exports or slow storage), the OS will trigger an "Application Not Responding" (ANR) dialog.
*   **Fix:** Offload all file I/O to background threads using Coroutines (`Dispatchers.IO`).

### 2.3. Scoped Storage Violation (Crash/Failure)
*   **Severity:** **HIGH**
*   **File:** `app/src/main/java/com/chamundi/templete/PhotoshopEditorActivity.kt` (Line 118)
*   **File:** `app/src/main/java/com/chamundi/templete/utils/ImageGenerator.kt` (Line 607)
*   **Root Cause:** The code attempts to write directly to public directories:
    *   `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)`
*   **Impact:** On Android 10 (API 29) and above, direct access to these paths is restricted. Calls will fail with `FileNotFoundException` or `SecurityException`.
*   **Fix:**
    *   Use **MediaStore API** (`ContentResolver.insert`) to save images to the public Gallery.
    *   Use `Context.getExternalFilesDir()` for app-private files (projects).
    *   Use `ActivityResultContracts.CreateDocument` if the user should pick the save location.

---

## 3. Architecture & Lifecycle

### 3.1. Broken Intent Handling
*   **Severity:** **HIGH**
*   **File:** `app/src/main/AndroidManifest.xml`
*   **Root Cause:** `MainActivity` is declared with `android:exported="false"` but contains `<intent-filter>` tags for `ACTION_SEND`.
*   **Impact:** External applications cannot share content to this app. The operating system will block the intent because the activity is not exported.
*   **Fix:** Set `android:exported="true"` for `MainActivity` to allow inter-app communication.

### 3.2. Memory Leaks in ViewModels
*   **Severity:** **MEDIUM**
*   **File:** `app/src/main/java/com/chamundi/templete/editor/EditorViewModel.kt`
*   **File:** `app/src/main/java/com/chamundi/templete/viewmodel/BreakingNewsViewModel.kt`
*   **Root Cause:** `Bitmap` objects are held in `StateFlow` (`_state`).
*   **Impact:** `StateFlow` retains its value even when not collected. If the Activity is destroyed (e.g., rotation, backgrounding) but the ViewModel survives, the heavy Bitmaps remain in memory. In `BreakingNewsViewModel`, creating multiple states without clearing old ones increases pressure.
*   **Fix:**
    *   Avoid holding `Bitmap` in StateFlow if possible. Use URIs or file paths.
    *   If Bitmaps are necessary for performance, ensure they are explicitly recycled in `ViewModel.onCleared()` or when replaced.

### 3.3. Composition Side Effects
*   **Severity:** **MEDIUM**
*   **File:** `app/src/main/java/com/chamundi/templete/PhotoshopEditorActivity.kt` (Line 81)
*   **Root Cause:** `presetsDir.listFiles()` is called directly inside the Composable function body (not in `LaunchedEffect` or `remember` block calculation, though it is in `remember`'s init block, it blocks the first composition).
*   **Impact:** Every time `PhotoshopEditorScreen` is recomposed (if state changes trigger it), this disk read might re-run if not correctly memoized. Even inside `remember`, it blocks the initial frame.

---

## 4. Code Quality & Correctness

### 4.1. Transform Logic Error
*   **Severity:** **MEDIUM**
*   **File:** `app/src/main/java/com/chamundi/templete/editor/TransformLogic.kt`
*   **Root Cause:** `calculateResize` implements a simplified logic that often behaves like "Scale from Center" regardless of the handle used. It does not correctly adjust `offsetX`/`offsetY` to anchor the opposite corner during resizing.
*   **Impact:** Dragging a corner handle (e.g., Top-Left) will cause the layer to expand in all directions, causing the Bottom-Right corner to move, which is unexpected UI behavior.

### 4.2. Swallowed Exceptions
*   **Severity:** **LOW**
*   **File:** `app/src/main/java/com/chamundi/templete/editor/persistence/ProjectSerializer.kt`
*   **Root Cause:** `catch (e: Exception) { e.printStackTrace(); null }`
*   **Impact:** If loading fails (e.g., OOM, permission), the user sees no error message, just a silent failure or empty state.

### 4.3. Hardcoded File Paths in JSON
*   **Severity:** **MEDIUM**
*   **File:** `app/src/main/java/com/chamundi/templete/editor/persistence/ProjectSerializer.kt`
*   **Root Cause:** Image paths are stored relative to `projectDir`.
*   **Impact:** If the project folder is moved or renamed, or if the file handling logic changes, the images will be lost. This is generally acceptable for a local bundle but fragile.

---

## 5. Configuration

*   **Target SDK:** `targetSdk = 36` is used. This is likely an Android 16 Developer Preview version. Ensure this is intentional, as it may introduce unstable behaviors.
*   **Minification:** `isMinifyEnabled = false`. Shipping without R8/ProGuard increases APK size and makes reverse engineering trivial.

---

## 6. Recommendations

1.  **Immediate Fix:** Implement `inSampleSize` for all `BitmapFactory` calls.
2.  **Immediate Fix:** Move `saveProject`, `exportAsPng`, and `generateBreakingNewsImage` to `Dispatchers.IO`.
3.  **Immediate Fix:** Update `AndroidManifest.xml` to export `MainActivity` or remove the intent filters.
4.  **Immediate Fix:** Replace direct file access for exports with `MediaStore` API.
5.  **Refactor:** Rewrite `TransformLogic` to support proper anchor-based resizing.
6.  **Refactor:** Implement a `BitmapPool` or caching mechanism to reuse memory instead of allocating new Bitmaps for every operation.
