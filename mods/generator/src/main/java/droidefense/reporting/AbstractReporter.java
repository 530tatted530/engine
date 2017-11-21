package droidefense.reporting;

import droidefense.sdk.log4j.Log;
import droidefense.sdk.log4j.LoggerType;

import java.awt.*;
import java.io.File;

/**
 * Created by .local on 03/05/2017.
 */
public abstract class AbstractReporter {

    private File reportFile;

    public abstract boolean generateReport();

    public void open() {
        openReportOnBorwser();
    }

    private void openReportOnBorwser() {
        try {
            Desktop.getDesktop().open(reportFile);
        } catch (Exception e) {
            Log.write(LoggerType.ERROR, "Could no automatically open sample report on user browser", e.getLocalizedMessage());
        }
    }

    public File getReportFile() {
        return reportFile;
    }
}
