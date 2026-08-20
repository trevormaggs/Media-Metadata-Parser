package gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
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

        String filename = (sourceText != null ? sourceText.getText().trim() : "");
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);
        Object userData = (sourceText != null ? sourceText.getUserData() : null);
        String userDataParent = (userData instanceof Path ? ((Path) userData).toString() : "");

        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files before running the batch process.");
        }

        // Multi-file selection handling
        if (filename.contains(","))
        {
            if (userDataParent.isEmpty())
            {
                throw new BatchErrorException("Individual files detected without a parent folder context.\n\nPlease use the 'Select Specific Files' menu option to select files.");
            }

            String[] parts = filename.split("\\s*,\\s*");
            String[] files = new String[parts.length];

            for (int i = 0; i < parts.length; i++)
            {
                files[i] = Paths.get(parts[i]).getFileName().toString();
            }

            builder.source(userDataParent).fileSet(files);
        }

        else
        {
            Path resolvedPath;
            Path rawPath = Paths.get(filename);

            if (!userDataParent.isEmpty() && !rawPath.isAbsolute())
            {
                resolvedPath = Paths.get(userDataParent).toAbsolutePath().resolve(rawPath).normalize();
            }

            else
            {
                resolvedPath = rawPath.toAbsolutePath().normalize();
            }

            if (Files.isRegularFile(resolvedPath))
            {
                Path parent = resolvedPath.getParent();

                if (parent == null)
                {
                    throw new BatchErrorException("Specified file does not have a valid parent directory");
                }

                builder.source(parent.toString()).fileSet(new String[]{resolvedPath.getFileName().toString()});
            }

            else if (Files.isDirectory(resolvedPath))
            {
                builder.source(resolvedPath.toString());
            }

            else
            {
                throw new BatchErrorException("The specified path does not exist or is not a valid file/directory:\n\n" + filename);
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