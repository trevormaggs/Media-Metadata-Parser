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
        TextField sourceText = MainViewPane.getById(root, MainViewPane.SRCID);
        TextField targetText = MainViewPane.getById(root, MainViewPane.TGTID);
        TextField prefixText = MainViewPane.getById(root, MainViewPane.PFXID);
        DatePicker modifyDatePicker = MainViewPane.getById(root, MainViewPane.DTMID);
        CheckBox embedDateTime = MainViewPane.getById(root, MainViewPane.EMBID);
        CheckBox forceDateChange = MainViewPane.getById(root, MainViewPane.FRCID);
        CheckBox skipVideo = MainViewPane.getById(root, MainViewPane.SKPID);
        CheckBox showMetadata = MainViewPane.getById(root, MainViewPane.SHWID);
        CheckBox descending = MainViewPane.getById(root, MainViewPane.SRTID);
        CheckBox debug = MainViewPane.getById(root, MainViewPane.DBGID);
        CheckBox trace = MainViewPane.getById(root, MainViewPane.TRCID);
        String filename = (sourceText != null ? sourceText.getText().trim() : "");
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);
        
        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files before running the batch process.");
        }

        String userDataParent = (String) sourceText.getUserData();

        if (filename.contains(","))
        {
            if (userDataParent == null || userDataParent.trim().isEmpty())
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
            Path path = Paths.get(filename).normalize();

            if (Files.isRegularFile(path))
            {
                Path parent = path.getParent();

                if (parent == null)
                {
                    throw new BatchErrorException("Specified file does not have a valid parent directory");
                }

                String parentDir = parent.toAbsolutePath().toString();
                builder.source(parentDir).fileSet(new String[]{path.getFileName().toString()});
            }

            else
            {
                builder.source(path.toAbsolutePath().toString());
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