package net.manenti.reporter;

import net.manenti.reporter.model.LogEntry;
import net.manenti.reporter.model.OutputFormat;
import net.manenti.reporter.parser.LogParser;
import net.manenti.reporter.report.ReportGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the Traffic Report Generator.
 *
 * <pre>
 * Usage:
 *   java -jar traffic-reporter.jar
 * </pre>
 */
public class TrafficReporterApp {

    private static final Logger LOG = Logger.getLogger(TrafficReporterApp.class.getName());

    private static final String DEFAULT_LOG_PATH    = "logfiles/requests.log";
    private static final String DEFAULT_REPORT_DIR  = "reports/";
    private static final String DEFAULT_REPORT_NAME = "ipaddr";

    public static void main(String[] args) {
        // --- Prompt user for configuration ---
        Scanner scanner = new Scanner(System.in);

        System.out.print("Specify the log path? [" + DEFAULT_LOG_PATH + "]: ");
        String logPathInput = scanner.nextLine().trim();
        String logPathStr = logPathInput.isEmpty() ? DEFAULT_LOG_PATH : logPathInput;
        Path logPath = Paths.get(logPathStr);

        System.out.print("Specify the reports dir? [" + DEFAULT_REPORT_DIR + "]: ");
        String reportDirInput = scanner.nextLine().trim();
        String reportDir = reportDirInput.isEmpty() ? DEFAULT_REPORT_DIR : reportDirInput;
        if (!reportDir.endsWith("/")) reportDir += "/";

        System.out.print("Specify the report name? [" + DEFAULT_REPORT_NAME + "]: ");
        String reportNameInput = scanner.nextLine().trim();
        String reportName = reportNameInput.isEmpty() ? DEFAULT_REPORT_NAME : reportNameInput;

        String formatStr;
        OutputFormat format = null;
        System.out.print("Specify the output format: csv or json? [csv]: ");
        formatStr = scanner.nextLine().trim();
        if (formatStr.isEmpty() || formatStr.equalsIgnoreCase("csv")) {
            format = OutputFormat.CSV;
        } else if (formatStr.equalsIgnoreCase("json")) {
            format = OutputFormat.JSON;
        } else {
            System.out.println("Invalid format. Please enter 'csv' or 'json'.");
        }

        String extension = format == OutputFormat.JSON ? ".json" : ".csv";
        Path reportPath = Paths.get(reportDir + reportName + extension);

        LOG.info("Log source : " + logPath.toAbsolutePath());
        LOG.info("Report dest: " + reportPath.toAbsolutePath());
        LOG.info("Format     : " + format);

        // --- Parse ---
        LogParser parser = new LogParser();
        List<LogEntry> entries = List.of();
        try {
            entries = parser.parse(logPath.toAbsolutePath());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to read log file: " + logPath, e);
            System.exit(1);
            return;
        }

        if (entries.isEmpty()) {
            LOG.warning("No valid OK-status entries found in " + logPath + ". Report will be empty.");
        }

        // --- Generate report ---
        ReportGenerator generator = new ReportGenerator();
        try {
            generator.generate(entries, reportPath, format);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to write report: " + reportPath, e);
            System.exit(2);
            return;
        }

        LOG.info("Report written successfully to " + reportPath.toAbsolutePath());
    }
}
