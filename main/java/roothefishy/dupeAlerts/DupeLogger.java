package roothefishy.dupeAlerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DupeLogger {
    private final File logsDirectory;
    private final File currentLogFile;
    private final File archiveDirectory;
    private final Gson gson;
    private final Logger logger;
    private final List<DupeLog> activeLogs;

    public DupeLogger(File dataFolder, Logger logger) {
        this.logger = logger;
        this.logsDirectory = new File(dataFolder, "logs");
        this.archiveDirectory = new File(logsDirectory, "archive");
        this.currentLogFile = new File(logsDirectory, "dupe_logs.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.activeLogs = new ArrayList<>();

        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs();
        }
        if (!archiveDirectory.exists()) {
            archiveDirectory.mkdirs();
        }

        loadLogs();
    }

    public void logDupe(DupeLog dupeLog) {
        activeLogs.add(dupeLog);
        saveLogs();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = sdf.format(new Date(dupeLog.getTimestamp()));
        
        logger.log(Level.WARNING, String.format(
            "[DUPE DETECTED] Player: %s | Material: %s | Before: %d | After: %d | Method: %s | Time: %s",
            dupeLog.getPlayerName(),
            dupeLog.getMaterial(),
            dupeLog.getOldAmount(),
            dupeLog.getNewAmount(),
            dupeLog.getDupeMethod(),
            timeStr
        ));
    }

    private void saveLogs() {
        try (FileWriter writer = new FileWriter(currentLogFile)) {
            gson.toJson(activeLogs, writer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save dupe logs", e);
        }
    }

    private void loadLogs() {
        if (!currentLogFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(currentLogFile)) {
            Type listType = new TypeToken<ArrayList<DupeLog>>(){}.getType();
            List<DupeLog> loadedLogs = gson.fromJson(reader, listType);
            if (loadedLogs != null) {
                activeLogs.addAll(loadedLogs);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load dupe logs", e);
        }
    }

    public List<DupeLog> getActiveLogs() {
        return new ArrayList<>(activeLogs);
    }

    public List<DupeLog> getNonDismissedLogs() {
        List<DupeLog> nonDismissed = new ArrayList<>();
        for (DupeLog log : activeLogs) {
            if (!log.isDismissed()) {
                nonDismissed.add(log);
            }
        }
        return nonDismissed;
    }

    public void dismissLog(String logId) {
        for (DupeLog log : activeLogs) {
            if (log.getId().equals(logId)) {
                log.setDismissed(true);
                saveLogs();
                return;
            }
        }
    }

    public void flagLog(String logId) {
        for (DupeLog log : activeLogs) {
            if (log.getId().equals(logId)) {
                log.setFlagged(true);
                saveLogs();
                return;
            }
        }
    }

    public DupeLog getLogById(String logId) {
        for (DupeLog log : activeLogs) {
            if (log.getId().equals(logId)) {
                return log;
            }
        }
        return null;
    }

    public void archiveOldLogs(int daysOld) {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        List<DupeLog> toArchive = new ArrayList<>();
        
        for (DupeLog log : activeLogs) {
            if (log.getTimestamp() < cutoffTime && log.isDismissed()) {
                toArchive.add(log);
            }
        }

        if (!toArchive.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String archiveFileName = "archive_" + sdf.format(new Date()) + ".json";
            File archiveFile = new File(archiveDirectory, archiveFileName);
            
            try (FileWriter writer = new FileWriter(archiveFile)) {
                gson.toJson(toArchive, writer);
                activeLogs.removeAll(toArchive);
                saveLogs();
                logger.info("Archived " + toArchive.size() + " old dupe logs to " + archiveFileName);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to archive old logs", e);
            }
        }
    }

    public int getTotalLogs() {
        return activeLogs.size();
    }

    public int getActiveDupeLogs() {
        int count = 0;
        for (DupeLog log : activeLogs) {
            if (!log.isDismissed()) {
                count++;
            }
        }
        return count;
    }
}
