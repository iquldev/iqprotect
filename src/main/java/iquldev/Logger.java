package iquldev;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Logger {
    private static final String LOG_FILE = "plugins/iqProtect/log.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static ConfigManager configManager;

    public static void setConfigManager(ConfigManager configManager) {
        Logger.configManager = configManager;
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warning(String message) {
        log("WARNING", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }

    private static void log(String level, String message) {
        if (configManager != null && !configManager.isLoggingEnabled()) {
            return;
        }

        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        try {
            File logFile = new File(LOG_FILE);
            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(logMessage);
            }
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static String getLogMessage(String path) {
        return getLogMessage(path, new HashMap<>());
    }

    public static String getLogMessage(String path, Map<String, String> placeholders) {
        if (configManager == null) {
            return "Log message not found: " + path;
        }

        String message = configManager.getConfig().getString("logging_messages." + path, "Log message not found: " + path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }
} 