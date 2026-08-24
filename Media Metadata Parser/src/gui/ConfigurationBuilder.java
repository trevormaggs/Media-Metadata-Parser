package gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.time.LocalDate;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import batch.BatchBuilder;
import batch.BatchConfiguration;
import batch.BatchErrorException;

class ConfigurationBuilder
{
    private final Parent root;

    ConfigurationBuilder(Parent root)
    {
        this.root = root;
    }

    BatchConfiguration build() throws BatchErrorException
    {
        BatchBuilder builder = new BatchBuilder();
        TextField sourceText = GUIUtils.getById(root, MainViewPane.SRCID, TextField.class);
        TextField targetText = GUIUtils.getById(root, MainViewPane.TGTID, TextField.class);
        TextField prefixText = GUIUtils.getById(root, MainViewPane.PFXID, TextField.class);
        DatePicker modifyDatePicker = GUIUtils.getById(root, MainViewPane.DTMID, DatePicker.class);
        CheckBox embedDateTime = GUIUtils.getById(root, MainViewPane.EMBID, CheckBox.class);
        CheckBox forceDateChange = GUIUtils.getById(root, MainViewPane.FRCID, CheckBox.class);
        CheckBox skipVideo = GUIUtils.getById(root, MainViewPane.SKPID, CheckBox.class);
        CheckBox showMetadata = GUIUtils.getById(root, MainViewPane.SHWID, CheckBox.class);
        CheckBox descending = GUIUtils.getById(root, MainViewPane.SRTID, CheckBox.class);
        CheckBox debug = GUIUtils.getById(root, MainViewPane.DBGID, CheckBox.class);
        CheckBox trace = GUIUtils.getById(root, MainViewPane.TRCID, CheckBox.class);

        String filename = sourceText.getText().trim();
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);

        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files first.");
        }

        // Multi-file selection handling
        if (filename.contains(","))
        {
            Path parentDir = null;
            String[] parts = filename.split("\\s*,\\s*");

            // 1. Discover the common parent directory from absolute path tokens
            for (String token : parts)
            {
                try
                {
                    Path fpath = Paths.get(token);

                    if (fpath.isAbsolute() && Files.isRegularFile(fpath))
                    {
                        Path parent = fpath.getParent();
                        
                        parentDir = (parent == null ? fpath.getRoot() : parent);
                        break;
                    }
                }
                
                catch (InvalidPathException exc)
                {
                }
            }

            // 2. Fall back to Tooltip text if paths are relative (e.g. populated via FileChooser)
            if (parentDir == null && sourceText.getTooltip() != null)
            {
                try
                {
                    Path fpath = Paths.get(sourceText.getTooltip().getText());
                    
                    if (fpath.isAbsolute() && Files.isDirectory(fpath))
                    {
                        parentDir = fpath;
                    }
                }
                
                catch (InvalidPathException ignored)
                {
                }
            }

            if (parentDir != null)
            {
                String[] files = new String[parts.length];

                for (int i = 0; i < parts.length; i++)
                {
                    try
                    {
                        Path fpath = Paths.get(parts[i]);
                        Path resolved = fpath.isAbsolute() ? fpath : parentDir.resolve(fpath).normalize();

                        Path resolvedParent = resolved.getParent();
                        Path effectiveParent = (resolvedParent != null ? resolvedParent : resolved.getRoot());

                        if (!Files.isRegularFile(resolved) || !parentDir.equals(effectiveParent))
                        {
                            sourceText.setUserData(null);
                            throw new BatchErrorException("One or more source files do not exist or reside outside the directory:\n\n" + parts[i]);
                        }

                        files[i] = resolved.getFileName().toString();
                    }
                    
                    catch (InvalidPathException exc)
                    {
                        sourceText.setUserData(null);
                        throw new BatchErrorException("Invalid file path formatting: " + parts[i]);
                    }
                }

                // Centralised update for PathHistoryStore
                sourceText.setUserData(parentDir.toAbsolutePath());
                builder.source(parentDir.toAbsolutePath().toString()).fileSet(files);
            }
            
            else
            {
                sourceText.setUserData(null);
                throw new BatchErrorException("Individual files detected without an absolute parent directory context.\n\nPlease specify absolute paths or use the file picker.");
            }
        }
        
        else
        {
            try
            {
                // Single folder or file path
                Path fpath = Paths.get(filename);

                if (!fpath.isAbsolute())
                {
                    sourceText.setUserData(null);
                    throw new BatchErrorException("Unable to determine the location of the specified path. Specify its parent directory:\n\n" + filename);
                }

                Path fullPath = fpath.normalize();

                if (Files.notExists(fullPath))
                {
                    sourceText.setUserData(null);
                    throw new BatchErrorException("The specified path does not exist:\n\n" + filename);
                }

                if (Files.isDirectory(fullPath))
                {
                    // Centralised update for PathHistoryStore
                    sourceText.setUserData(fullPath);
                    builder.source(fullPath.toString());
                }
                else
                {
                    Path parentDir = fullPath.getParent();
                    Path realDir = (parentDir == null ? fullPath.getRoot() : parentDir);

                    // Centralised update for PathHistoryStore
                    sourceText.setUserData(realDir);
                    builder.source(realDir.toString()).fileSet(new String[]{fullPath.getFileName().toString()});
                }
            }
            catch (InvalidPathException exc)
            {
                sourceText.setUserData(null);
                throw new BatchErrorException("The content is not a valid file path.\n\nPath: " + filename);
            }
        }

        return builder.target(targetText == null ? null : targetText.getText())
                .prefix(prefixText == null ? null : prefixText.getText())
                .userDate(dateValue == null ? null : dateValue.toString())
                .embedDateTime(embedDateTime != null && embedDateTime.isSelected())
                .forceDateChange(forceDateChange != null && forceDateChange.isSelected())
                .skipVideo(skipVideo != null && skipVideo.isSelected())
                .descending(descending != null && descending.isSelected())
                .debug(debug != null && debug.isSelected())
                .showMetadata(showMetadata != null && showMetadata.isSelected())
                .trace(trace != null && trace.isSelected())
                .build();
    }
}