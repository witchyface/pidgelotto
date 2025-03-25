package ie.sammie.pidgelotto.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DebugLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("PidgelottoDebugLogger");
    private static FileWriter fileWriter;
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String LOG_FILE = "config/pidgelotto/debug.log";

    static {
        try {
            File logFile = new File(LOG_FILE);
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File oldLogFile = new File("config/pidgelotto/debug_" + timestamp + ".log");
                logFile.renameTo(oldLogFile);
            }
            fileWriter = new FileWriter(LOG_FILE, true);
        } catch (IOException e) {
            LOGGER.error("Failed to create or open the debug log file", e);
        }
    }

    public static void log(String message) {
        if (fileWriter == null) {
            LOGGER.error("FileWriter is not initialized. Cannot log message: {}", message);
            return;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logMessage = "[" + timestamp + "] " + message + "\n";
            fileWriter.write(logMessage);
            fileWriter.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to write to the debug log file", e);
        }
    }

    public static void close() {
        if (fileWriter == null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close the debug log file", e);
            }
        }
    }
}
