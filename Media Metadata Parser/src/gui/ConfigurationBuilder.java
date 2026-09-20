package gui;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import batch.BatchBuilder;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

/**
 * Extracts and validates JavaFX UI form field inputs to construct a immutable {@link BatchConfiguration}.
 */
final class ConfigurationBuilder
{
    private final Parent root;

    /**
     * Constructs a builder instance bound to the specified root UI layout container.
     *
     * @param root
     *        the parent container holding input controls
     */
    ConfigurationBuilder(Parent root)
    {
        this.root = root;
    }

    /**
     * Reads form controls and returns a fully validated {@link BatchConfiguration}.
     *
     * @return constructed batch configuration object
     * 
     * @throws BatchErrorException if any input fields contain invalid or non-existent file path targets
     */
    BatchConfiguration build() throws BatchErrorException
    {
        Path parentDir = null;
        String[] files = null;
        BatchBuilder builder = new BatchBuilder();

        TextField sourceText = UtilsJavaFX.getById(root, MainViewPane.SRCID, TextField.class);
        TextField targetText = UtilsJavaFX.getById(root, MainViewPane.TGTID, TextField.class);
        TextField prefixText = UtilsJavaFX.getById(root, MainViewPane.PFXID, TextField.class);
        DatePicker modifyDatePicker = UtilsJavaFX.getById(root, MainViewPane.DTMID, DatePicker.class);
        CheckBox embedDateTime = UtilsJavaFX.getById(root, MainViewPane.EMBID, CheckBox.class);
        CheckBox forceDateChange = UtilsJavaFX.getById(root, MainViewPane.FRCID, CheckBox.class);
        CheckBox skipVideo = UtilsJavaFX.getById(root, MainViewPane.SKPID, CheckBox.class);
        CheckBox showMetadata = UtilsJavaFX.getById(root, MainViewPane.SHWID, CheckBox.class);
        CheckBox descending = UtilsJavaFX.getById(root, MainViewPane.SRTID, CheckBox.class);
        CheckBox debug = UtilsJavaFX.getById(root, MainViewPane.DBGID, CheckBox.class);
        CheckBox trace = UtilsJavaFX.getById(root, MainViewPane.TRCID, CheckBox.class);

        String filename = sourceText.getText().trim();
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);

        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files first.");
        }

        // Find parent directory from tooltip if available
        if (sourceText.getTooltip() != null)
        {
            try
            {
                Path fpath = Paths.get(sourceText.getTooltip().getText());

                if (fpath.isAbsolute())
                {
                    parentDir = (Files.isDirectory(fpath) ? fpath : (fpath.getParent() == null ? fpath.getRoot() : fpath.getParent()));
                }
            }
            
            catch (InvalidPathException exc)
            {
                // Fall back if tooltip path cannot be parsed
            }
        }

        // Multi-file selection handling (comma-separated filenames)
        if (filename.contains(","))
        {
            String[] parts = filename.split("\\s*,\\s*");

            if (parentDir == null)
            {
                for (String token : parts)
                {
                    try
                    {
                        Path fpath = Paths.get(token);

                        if (fpath.isAbsolute())
                        {
                            Path parent = fpath.getParent();
                            parentDir = (parent == null ? fpath.getRoot() : parent);
                            break;
                        }
                    }
                    
                    catch (InvalidPathException exc)
                    {
                        // Pass through to inspect next token
                    }
                }
            }

            if (parentDir != null)
            {
                files = new String[parts.length];

                for (int i = 0; i < parts.length; i++)
                {
                    try
                    {
                        Path fpath = Paths.get(parts[i]);
                        Path fullPath = (fpath.isAbsolute() ? fpath : parentDir.resolve(fpath).normalize());

                        if (!Files.isRegularFile(fullPath) || !fullPath.startsWith(parentDir))
                        {
                            throw new BatchErrorException("One or more source files do not exist or come from a different directory:\n\n" + parts[i]);
                        }

                        files[i] = fullPath.getFileName().toString();
                    }
                    
                    catch (InvalidPathException exc)
                    {
                        throw new BatchErrorException("Invalid file path detected: " + parts[i]);
                    }
                }
            }
            
            else
            {
                throw new BatchErrorException("Individual files were detected without an absolute parent directory.\n\nPlease specify absolute paths or use the file picker.");
            }
        }
        
        else
        {
            // Single folder or single file path resolution
            try
            {
                Path fullPath;
                Path fpath = Paths.get(filename);

                if (fpath.isAbsolute())
                {
                    fullPath = fpath.normalize();
                }
                
                else if (parentDir != null)
                {
                    fullPath = parentDir.resolve(fpath).normalize();
                }
                
                else
                {
                    fullPath = fpath.toAbsolutePath().normalize();
                }

                if (Files.notExists(fullPath))
                {
                    throw new BatchErrorException("The specified path does not exist:\n\n" + filename);
                }

                if (Files.isDirectory(fullPath))
                {
                    files = null; // Target whole directory
                    parentDir = fullPath;
                }
                
                else
                {
                    Path parent = fullPath.getParent();
                    parentDir = (parent == null ? fullPath.getRoot() : parent);
                    files = new String[]{ fullPath.getFileName().toString() };
                }
            }
            
            catch (InvalidPathException exc)
            {
                throw new BatchErrorException("The content is not a valid file path.\n\nPath: " + filename);
            }
        }

        return builder.source(parentDir.toAbsolutePath().toString())
                .fileSet(files)
                .target(targetText.getText())
                .prefix(prefixText.getText())
                .userDate(dateValue == null ? null : dateValue.toString())
                .embedDateTime(embedDateTime.isSelected())
                .forceDateChange(forceDateChange.isSelected())
                .skipVideo(skipVideo.isSelected())
                .descending(descending.isSelected())
                .debug(debug.isSelected())
                .showMetadata(showMetadata.isSelected())
                .trace(trace.isSelected())
                .build();
    }
}