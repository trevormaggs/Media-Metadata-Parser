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

class ConfigurationBuilder3
{
    private final Parent root;

    ConfigurationBuilder3(Parent root)
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

        String filename = (sourceText != null ? sourceText.getText().trim() : "");
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);

        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files before running the batch process.");
        }

        // Multi-file selection handling
        if (filename.contains(","))
        {
            boolean valid = false;
            Path parentDir = null;
            String[] parts = filename.split("\\s*,\\s*");

            for (String token : parts)
            {
                try
                {
                    Path fpath = Paths.get(token).toAbsolutePath();

                    if (Files.isRegularFile(fpath))
                    {
                        valid = true;
                        parentDir = fpath.getParent();
                        break;
                    }
                }

                catch (InvalidPathException exc)
                {
                    // Ignore invalid path components during initial root discovery
                }
            }

            if (valid)
            {
                for (String token : parts)
                {
                    try
                    {
                        Path fpath = parentDir.resolve(token);

                        if (!Files.isRegularFile(fpath) || !parentDir.equals(fpath.getParent()))
                        {
                            valid = false;
                            break;
                        }
                    }

                    catch (InvalidPathException exc)
                    {
                        valid = false;
                        break;
                    }
                }

                if (valid)
                {
                    String[] files = new String[parts.length];

                    for (int i = 0; i < parts.length; i++)
                    {
                        files[i] = Paths.get(parts[i]).getFileName().toString();
                    }

                    sourceText.setUserData(parentDir.toAbsolutePath());
                    builder.source(parentDir.toAbsolutePath().toString()).fileSet(files);
                }

                else
                {
                    throw new BatchErrorException("One or more source files is unknown or not in the same directory.\n\nReceived : " + filename);
                }
            }
        }

        else
        {
            // Evaluate single folder or file target path
            try
            {
                Path fpath = Paths.get(filename);

                if (Files.exists(fpath))
                {
                    Path fullPath = fpath.toAbsolutePath();
                    Path parentDir = Files.isDirectory(fullPath) ? fullPath : fullPath.getParent();

                    sourceText.setUserData(parentDir.toAbsolutePath());
                    builder.source(parentDir.toAbsolutePath().toString()).fileSet(new String[]{fpath.getFileName().toString()});
                }

                else
                {
                    throw new BatchErrorException("The path received does not exist.\n\nPath: " + filename);
                }
            }

            catch (InvalidPathException exc)
            {
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