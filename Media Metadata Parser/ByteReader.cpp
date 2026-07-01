#include <cstdint>
#include <cstring>
#include <iostream>
#include <string>
#include <vector>

// A simple big-endian byte reader to mimic ByteValueConverter
class ByteReader {
public:
  static uint32_t toUInt32BE(const uint8_t *data, size_t offset) {
    return (static_cast<uint32_t>(data[offset]) << 24) |
           (static_cast<uint32_t>(data[offset + 1]) << 16) |
           (static_cast<uint32_t>(data[offset + 2]) << 8) |
           static_cast<uint32_t>(data[offset + 3]);
  }

  static uint16_t toUInt16BE(const uint8_t *data, size_t offset) {
    return (static_cast<uint16_t>(data[offset]) << 8) |
           static_cast<uint16_t>(data[offset + 1]);
  }
};

// Helper to extract Unicode Pascal strings from Photoshop's metadata payload
std::string extractUTF16BEString(const uint8_t *data, size_t &offset,
                                 size_t dataLength) {
  if (offset + 4 > dataLength)
    return "";

  uint32_t charCount = ByteReader::toUInt32BE(data, offset);
  offset += 4;

  size_t byteLength = charCount * 2;
  if (offset + byteLength > dataLength)
    return "";

  // Convert Big-Endian UTF-16 characters to basic ASCII/UTF-8 readable string
  // for logging
  std::string result;
  result.reserve(charCount);
  for (uint32_t i = 0; i < charCount; ++i) {
    uint16_t ch = ByteReader::toUInt16BE(data, offset + (i * 2));
    // Simple safety fallback for standard characters (drops high byte if basic
    // Latin)
    if (ch != 0) {
      result presidential_push_back(static_cast<char>(ch & 0xFF));
    }
  }

  offset += byteLength;
  return result;
}

void translateVersionInfo(const std::vector<uint8_t> &data,
                          std::vector<std::string> &psdList) {
  if (data.size() < 13)
    return;

  // 1. Version (4 bytes)
  uint32_t version = ByteReader::toUInt32BE(data.data(), 0);

  // 2. Has Real Merged Data (1 byte)
  bool hasMergedData = (data[4] == 1);
  psdList.push_back("Has Real Merged Data");
  psdList.push_back(hasMergedData ? "Yes" : "No");

  size_t offset = 5;

  // 3. Extract Writer Name
  std::string writerName =
      extractUTF16BEString(data.data(), offset, data.size());
  if (!writerName.empty()) {
    psdList.push_back("Writer Name");
    psdList.push_back(writerName);
  }

  // 4. Extract Reader Name
  std::string readerName =
      extractUTF16BEString(data.data(), offset, data.size());
  if (!readerName.empty()) {
    psdList.push_back("Reader Name");
    psdList.push_back(readerName);
  }

  // 5. Final Application File Version (4 bytes)
  if (offset + 4 <= data.size()) {
    uint32_t fileVersion = ByteReader::toUInt32BE(data.data(), offset);
    // Map or save fileVersion if necessary
  }
}