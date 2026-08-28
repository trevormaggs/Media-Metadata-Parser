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

final class ConfigurationBuilder
{
    private final Parent root;

    ConfigurationBuilder(Parent root)
    {
        this.root = root;
    }

    BatchConfiguration build() throws BatchErrorException
    {
        Path parentDir = null;
        String[] files = null;
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
            String[] parts = filename.split("\\s*,\\s*");

            if (sourceText.getTooltip() != null)
            {
                try
                {
                    Path fpath = Paths.get(sourceText.getTooltip().getText().trim());

                    if (fpath.isAbsolute())
                    {
                        parentDir = Files.isDirectory(fpath) ? fpath : (fpath.getParent() == null ? fpath.getRoot() : fpath.getParent());
                    }
                }

                catch (InvalidPathException exc)
                {
                    // Just pass through
                }
            }

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
                        // Do nothing and try again next
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
                        Path parent = fullPath.getParent();
                        Path effectiveParent = (parent == null ? fullPath.getRoot() : parent);

                        if (!Files.isRegularFile(fullPath) || !parentDir.equals(effectiveParent))
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
            try
            {
                // Single folder or file path
                Path fpath = Paths.get(filename);
                Path fullPath = fpath.normalize();

                if (!fpath.isAbsolute())
                {
                    throw new BatchErrorException("Unable to determine the location of the specified path. Specify its parent directory:\n\n" + filename);
                }

                if (Files.notExists(fullPath))
                {
                    throw new BatchErrorException("The specified path does not exist:\n\n" + filename);
                }

                if (Files.isDirectory(fullPath))
                {
                    parentDir = fullPath;
                }

                else
                {
                    Path parent = fullPath.getParent();

                    parentDir = (parent == null ? fullPath.getRoot() : parent);
                    files = new String[]{fullPath.getFileName().toString()};
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