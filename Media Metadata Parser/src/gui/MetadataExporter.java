package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import common.Metadata;
import common.PropertyConsumer;
import javafx.scene.control.TextArea;
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
final class MetadataExporter
{
    private MetadataExporter()
    {
        // Prevent instantiation
    }

    enum SAVE_FORMAT
    {
        TXT, CSV, JSON;
    }

    static void export(File targetFile, TextArea flatTextArea) throws IOException
    {
        try (FileWriter writer = new FileWriter(targetFile))
        {
            writer.write(flatTextArea.getText());
        }
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
    static void export(File targetFile, List<MediaFileMetadata> records, SAVE_FORMAT format) throws IOException
    {
        String content;

        if (format == SAVE_FORMAT.CSV)
        {
            content = toCSV(records);
        }

        else if (format == SAVE_FORMAT.JSON)
        {
            content = toJSON(records);
        }

        else
        {
            throw new IOException("Unknown format type expected to save metadata to a file.");
        }

        try (FileWriter writer = new FileWriter(targetFile))
        {
            writer.write(content);
        }
    }

    private static String toCSV(List<MediaFileMetadata> records)
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
                                
                                csv.append(escapeCSV(fileName)).append(",")
                                        .append(escapeCSV(group)).append(",")
                                        .append(escapeCSV(tag.getDescription())).append(",")
                                        .append(escapeCSV(val)).append("\n");
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
                            csv.append(escapeCSV(fileName)).append(",")
                                    .append(escapeCSV("PNG")).append(",")
                                    .append(escapeCSV(key)).append(",")
                                    .append(escapeCSV(String.valueOf(value))).append("\n");
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

    private static String toJSON(List<MediaFileMetadata> records)
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
                    int totalIfds = tif.getDirectoryCount();

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

                    if (pngProps.length() > 0)
                    {
                        json.append(pngProps).append("\n");
                    }

                    json.append("        }\n");
                    json.append("      }\n");
                }
            }
        }

        json.append("]");

        return json.toString();
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

    private static String escapeCSV(String input)
    {
        if (input == null)
        {
            return "\"\"";
        }

        String escaped = input.replace("\"", "\"\"");

        return "\"" + escaped + "\"";
    }
}