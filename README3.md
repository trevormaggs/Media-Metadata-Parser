# Image Metadata Parsing Toolkit

Welcome to the **Image Metadata Parsing Toolkit** — a self-contained Java library built for extracting, analysing, and updating image metadata across standard industry formats, including TIFF, JPEG, PNG, HEIF/HEIC, and WebP.

This project focuses on high-performance file processing and clean modular design. By avoiding any external third-party dependencies, the toolkit uses a native, highly optimised framework to read and write metadata standards (Exif, XMP, and IFD structures) efficiently using standard Java 8.

---

## 🛠 Recent Core Improvements

The internal input/output engine has been completely redesigned to improve performance and reliability when processing large batches of files:

* **Optimised Storage Infrastructure:** Features a streamlined data-reading hierarchy that handles both direct disk files and memory-backed data streams uniformly.
* **Smart Block-Buffered Reading:** Instead of processing files byte-by-byte, data is processed in small, efficient blocks. This significantly reduces disk activity and accelerates file scanning speeds.
* **Intelligent File Realignment:** Includes a precise tracking algorithm that automatically handles file position correction during data scans, ensuring seamless layout tracking across all image formats.
* **Robust Error Handling:** System boundaries have been standardised to ensure predictable runtime error reporting and safe data validation safeguards.

---

## 🏗 Core Architecture Overview

The toolkit features a decoupled, extensible design structure built from standard Java interfaces:

```
 AutoCloseable
 ├── BinaryInput
 └── BinaryOutput

 AbstractBinaryStream (Root Base Framework)
 ├── ByteArrayReader [Memory Reader]
 └── AbstractRandomAccessStream [File Stream Controller]
     ├── RandomAccessReader [File Reader]
     └── RandomAccessWriter [File Writer]

```

---

## ✨ Key Features

### 🔍 Metadata Extraction & Parsing

* Deep extraction of **Exif** metadata tags from TIFF, JPEG, PNG, HEIF/HEIC, and WebP files.
* Stream-based extraction of inline **XMP** metadata envelopes.
* Low-level structural handling of image components, containers, and core directories.
* Full support for standard Exif tags with a modular registration system for custom vendor extensions.

### ✍️ Metadata Editing & Patching

* In-place, direct binary metadata modification **without image re-encoding**, preserving original image quality.
* Targeted date and time metadata patching capabilities.
* File-system attribute synchronisation (matching creation/modification times to image capture time).

### 🚀 Batch Processing Engine

* Recursive directory scanning with selective file inclusion/exclusion.
* Chronological file sorting and ordering using extracted metadata timestamps.
* Automated file reorganisation and filename generation (custom prefixes, structured capture dates).
* Automated repair and normalisation of missing or corrupted timestamps.
* Comprehensive processing logs and real-time execution progress updates.

---

## 📦 Supported Formats

| Format | Metadata Extraction | Metadata Updates | Structural Container |
| --- | --- | --- | --- |
| **JPEG / JPG** | ✓ | ✓ | Segment-based structures |
| **TIFF / TIF** | ✓ | ✓ | Native IFD Directory structures |
| **PNG** | ✓ | ✓ | Chunk-by-chunk layouts |
| **WebP** | ✓ | ✓ | RIFF container layouts |
| **HEIF / HEIC** | ✓ | ✓ | ISO Base Media Box structures |

---

## 💻 Command-Line Usage

```bash
java -jar MediaMetadataParser.jar [options] <source-directory>

```

### Options

| Option | Description |
| --- | --- |
| `-t <directory>` | Target directory where organised/copied files are saved. |
| `-p <prefix>` | Prepend processed files with a custom string prefix. |
| `-e` | Embed structural date and time formats into generated filenames. |
| `-m <date>` | Modify image metadata timestamps using a custom string format (`YYYY-MM-DD HH:MM:SS`). |
| `-f` | Force metadata date replacement, overriding existing file attributes. |
| `-l <files...>` | Pass a selective list of specific files to process. |
| `-k` | Skip video file patterns during directory scans. |
| `-s` | Display a detailed metadata inspection summary directly to the console. |
| `--desc` | Sort processed files in descending chronological order. |
| `-d, --debug` | Enable verbose debug logging for technical troubleshooting. |
| `-v, --version` | Display application build and version information. |
| `-h, --help` | Display terminal help documentation. |

### Example Command

```bash
java -jar MediaMetadataParser.jar \
    -t ./OrganisedPhotos \
    -p holiday_trip \
    -e \
    -m "2026-01-01 12:00:00" \
    -f \
    ./raw_photos

```

**What this command achieves:**

1. Scans the `./raw_photos` folder and subfolders.
2. Sorts media files chronologically using internal metadata time records.
3. Copies files safely into the `./OrganisedPhotos` folder without altering the original source files.
4. Renames output files using the custom prefix while embedding the exact captured date.
5. Safely force-patches the internal metadata timestamps to `2026-01-01 12:00:00`.

---

## 🎯 Project Goals

* **Advanced Codebase Showcase:** Demonstrates low-level file parsing techniques, pointer logic, and bit-level tracking.
* **Pure Java 8 Build:** Retains zero external framework overhead, relying purely on core language tools.
* **Production-Grade Readability:** Clean APIs, robust safety boundaries, and clear user documentation.

---

## 🚀 Getting Started

### Prerequisites

* Java 8 Development Kit (JDK) or higher.

### Installation

1. Clone the repository:
```bash
git clone https://github.com/trevormaggs/Media-Metadata-Parser.git

```


2. Import the project into your preferred Java IDE (IntelliJ IDEA, Eclipse, NetBeans).
3. Build the project or review the included Javadoc targets for detailed developer integrations.
