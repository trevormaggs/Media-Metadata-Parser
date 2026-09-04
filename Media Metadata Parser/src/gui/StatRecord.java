package gui;

import javafx.beans.property.SimpleStringProperty;

/**
 * Model representing a key-value metric pair inside the processing statistics table view.
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2026
 */
class StatRecord
{
    static final StatRecord SOURCE_FILES = new StatRecord("Source Files", "0");
    static final StatRecord TARGET_FILES = new StatRecord("Processed Files", "0");
    static final StatRecord FILES_SKIPPED = new StatRecord("Files Skipped", "0");
    static final StatRecord TOTAL_SIZE = new StatRecord("Total Size", "0.00 MB");

    private final SimpleStringProperty metric;
    private final SimpleStringProperty value;
    private final String defaultValue;

    StatRecord(String metric, String defaultValue)
    {
        this.metric = new SimpleStringProperty(metric);
        this.value = new SimpleStringProperty(defaultValue);
        this.defaultValue = defaultValue;
    }

    SimpleStringProperty metricProperty()
    {
        return metric;
    }

    SimpleStringProperty valueProperty()
    {
        return value;
    }

    String getValue()
    {
        return value.get();
    }

    void setValue(Object ref)
    {
        value.set(String.valueOf(ref));
    }

    void reset()
    {
        value.set(defaultValue);
    }

    static void resetAll()
    {
        SOURCE_FILES.reset();
        TARGET_FILES.reset();
        FILES_SKIPPED.reset();
        TOTAL_SIZE.reset();
    }
}