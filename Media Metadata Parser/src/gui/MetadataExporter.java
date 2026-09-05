package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import common.Metadata;
import common.PropertyConsumer;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;

/**
 * Utility class that formats extracted metadata into structured JSON, CSV, or flat TXT formats
 * using core Java standard libraries without external dependencies.
 */
public final class MetadataExporter
{
    private MetadataExporter()
    {
        // Prevent instantiation
    }

    /**
     * Writes metadata records to disk in the chosen format.
     *
     * @param targetFile
     *        the file destination chosen by the user
     * @param records
     *        the list of media metadata models
     * @param format
     *        "JSON", "CSV", or "TXT"
     * @throws IOException
     *         if file output fails
     */
    public static void export(File targetFile, List<MediaFileMetadata> records, String format) throws IOException
    {
        String content;

        if ("JSON".equalsIgnoreCase(format))
        {
            content = toJson(records);
        }

        else if ("CSV".equalsIgnoreCase(format))
        {
            content = toCsv(records);
        }

        else
        {
            content = toFlatText(records);
        }

        try (FileWriter writer = new FileWriter(targetFile))
        {
            writer.write(content);
        }
    }

    private static String toJson(List<MediaFileMetadata> records)
    {
        StringBuilder json = new StringBuilder("[\n");

        if (records != null)
        {
            for (int i = 0; i < records.size(); i++)
            {
                MediaFileMetadata record = records.get(i);
                String fileName = record.getFileName() != null ? record.getFileName() : "Unknown File";
                Metadata<?> meta = record.getMetadata();

                json.append("  {\n");
                json.append("    \"fileName\": \"").append(escapeJson(fileName)).append("\",\n");
                json.append("    \"groups\": [\n");

                if (meta instanceof TifMetadataProvider)
                {
                    TifMetadataProvider tif = (TifMetadataProvider) meta;
                    int ifdIndex = 0;
                    int totalIfds = 0;

                    for (DirectoryIFD ignored : tif)
                    {
                        totalIfds++;
                    }

                    for (DirectoryIFD ifd : tif)
                    {
                        json.append("      {\n");
                        json.append("        \"groupName\": \"").append(escapeJson(ifd.getDirectoryType().getDescription())).append("\",\n");
                        json.append("        \"properties\": {\n");

                        int entryIndex = 0;
                        int totalEntries = 0;

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            if (entry.getTag() != null) totalEntries++;
                        }

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            Taggable tag = entry.getTag();

                            if (tag != null)
                            {
                                String val = tag.translate(entry.getData());

                                json.append("          \"").append(escapeJson(tag.getDescription())).append("\": \"").append(escapeJson(val)).append("\"");
                                entryIndex++;
                                json.append(entryIndex < totalEntries ? ",\n" : "\n");
                            }
                        }

                        json.append("        }\n");
                        ifdIndex++;
                        json.append("      }").append(ifdIndex < totalIfds ? ",\n" : "\n");
                    }
                }

                else if (meta instanceof PngMetadataProvider)
                {
                    PngMetadataProvider png = (PngMetadataProvider) meta;

                    json.append("      {\n");
                    json.append("        \"groupName\": \"PNG\",\n");
                    json.append("        \"properties\": {\n");

                    final StringBuilder pngProps = new StringBuilder();

                    PropertyConsumer consumer = new PropertyConsumer()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            if (pngProps.length() > 0)
                            {
                                pngProps.append(",\n");
                            }

                            pngProps.append("          \"").append(escapeJson(key)).append("\": \"").append(escapeJson(String.valueOf(value))).append("\"");
                        }
                    };

                    for (PngDirectory dir : png)
                    {
                        for (PngChunk chunk : dir)
                        {
                            chunk.printProperties(consumer);
                        }
                    }

                    json.append(pngProps).append("\n");
                    json.append("        }\n");
                    json.append("      }\n");
                }

                json.append("    ]\n");
                json.append("  }").append(i < records.size() - 1 ? ",\n" : "\n");
            }
        }

        json.append("]");

        return json.toString();
    }

    private static String toCsv(List<MediaFileMetadata> records)
    {
        StringBuilder csv = new StringBuilder("File Name,Group,Property,Value\n");

        if (records != null)
        {
            for (MediaFileMetadata record : records)
            {
                Metadata<?> meta = record.getMetadata();
                String fileName = record.getFileName() != null ? record.getFileName() : "Unknown File";

                if (meta instanceof TifMetadataProvider)
                {
                    TifMetadataProvider tif = (TifMetadataProvider) meta;

                    for (DirectoryIFD ifd : tif)
                    {
                        String group = ifd.getDirectoryType().getDescription();

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            Taggable tag = entry.getTag();

                            if (tag != null)
                            {
                                String val = tag.translate(entry.getData());
                                csv.append(escapeCsv(fileName)).append(",").append(escapeCsv(group)).append(",").append(escapeCsv(tag.getDescription())).append(",").append(escapeCsv(val)).append("\n");
                            }
                        }
                    }
                }

                else if (meta instanceof PngMetadataProvider)
                {
                    PngMetadataProvider png = (PngMetadataProvider) meta;

                    PropertyConsumer consumer = new PropertyConsumer()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            csv.append(escapeCsv(fileName)).append(",")
                                    .append(escapeCsv("PNG")).append(",")
                                    .append(escapeCsv(key)).append(",")
                                    .append(escapeCsv(String.valueOf(value))).append("\n");
                        }
                    };

                    for (PngDirectory dir : png)
                    {
                        for (PngChunk chunk : dir)
                        {
                            chunk.printProperties(consumer);
                        }
                    }
                }
            }
        }

        return csv.toString();
    }

    private static String toFlatText(List<MediaFileMetadata> records)
    {
        StringBuilder sb = new StringBuilder();

        if (records != null)
        {
            for (MediaFileMetadata record : records)
            {
                sb.append("File: ").append(record.getFileName()).append("\n");
                sb.append("----------------------------------------\n");
                // Flattening text matching your existing raw flat output structure...
            }
        }

        return sb.toString();
    }

    private static String escapeJson(String input)
    {
        if (input == null)
        {
            return "";
        }

        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeCsv(String input)
    {
        if (input == null)
        {
            return "\"\"";
        }

        String escaped = input.replace("\"", "\"\"");

        return "\"" + escaped + "\"";
    }
}