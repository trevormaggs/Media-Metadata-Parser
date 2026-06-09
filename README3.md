# Media Metadata Parser

A Java 8 library for extracting and analysing image metadata from multiple image formats, including **TIFF, JPEG, PNG, HEIF/HEIC, and WebP**.

This project is built entirely from the ground up using standard Java and focuses on providing a lightweight, extensible, and dependency-free framework for binary file parsing and metadata extraction.

The library currently focuses on **Exif metadata**, providing access to camera, image, and capture information embedded within supported image formats. Future development will expand support to additional metadata standards such as **XMP**, **IPTC**, and proprietary vendor-specific metadata.

## Project Goals

This project was created to:

* Parse image metadata without third-party libraries
* Provide a reusable binary parsing framework
* Support multiple image container formats through a common architecture
* Maintain full Java 8 compatibility
* Demonstrate clean API design, extensibility, and comprehensive documentation

## Features

* Extract Exif metadata from TIFF, JPEG, PNG, HEIF/HEIC, and WebP images
* Native parsing of TIFF Image File Directories (IFDs)
* Support for standard Exif tags and metadata directories
* Binary stream abstraction supporting both in-memory and random-access reading
* Configurable big-endian and little-endian processing
* Lightweight design with no external dependencies
* Extensive Javadoc documentation
* Modular architecture designed for future metadata standards

## Supported Formats

| Format      | Metadata Support |
| ----------- | ---------------- |
| TIFF        | Exif             |
| JPEG        | Exif             |
| PNG         | Exif             |
| HEIF / HEIC | Exif             |
| WebP        | Exif             |

## Architecture

The parser is built on a reusable binary stream framework consisting of:

* `BinaryInput`
* `BinaryOutput`
* `AbstractBinaryStream`
* `ByteArrayReader`
* `RandomAccessReader`
* `RandomAccessWriter`

Format-specific parsers are layered on top of this framework:

* TIFF Parser
* JPEG Parser
* PNG Parser
* HEIF/HEIC Parser
* WebP Parser

Exif metadata is normalised into a common metadata model, providing a consistent API across supported image formats.

## Getting Started

Clone the repository:

```bash
git clone https://github.com/trevormaggs/Media-Metadata-Parser.git
```

Import the project into your preferred Java IDE supporting Java 8 or later.

Review the Javadoc documentation for detailed API usage:

[https://trevormaggs.github.io/Media-Metadata-Parser/](https://trevormaggs.github.io/Media-Metadata-Parser/)

## Example

```java
Path imagePath = Paths.get("image.jpg");

// Parse image metadata using the appropriate parser
// Example code may vary as the API evolves.
```

## Roadmap

Planned enhancements include:

* XMP metadata support
* IPTC metadata support
* Vendor-specific maker note support
* Additional metadata standards
* Metadata editing capabilities
* Expanded unit test coverage
* Additional image format support

## Development Status

This project is actively developed and continues to evolve as additional metadata standards and image formats are implemented.

## License

See the repository license for licensing information.
