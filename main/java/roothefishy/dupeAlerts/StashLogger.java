package roothefishy.dupeAlerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StashLogger {
    private final File logsDirectory;
    private final File stashLogFile;
    private final Gson gson;
    private final Logger logger;
    private final List<StashLog> stashLogs;

    public StashLogger(File dataFolder, Logger logger) {
        this.logger = logger;
        this.logsDirectory = new File(dataFolder, "logs");
        this.stashLogFile = new File(logsDirectory, "stash_logs.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.stashLogs = new ArrayList<>();

        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs();
        }

        loadLogs();
    }

    public void logStash(StashLog stashLog) {
        stashLogs.add(stashLog);
        saveLogs();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = sdf.format(new Date(stashLog.getTimestamp()));
        
        logger.log(Level.WARNING, String.format(
            "[STASH DETECTED] Location: %s | Items: %d | Reason: %s | Time: %s",
            stashLog.getLocationString(),
            stashLog.getTotalItemCount(),
            stashLog.getDetectionReason(),
            timeStr
        ));
    }

    private void saveLogs() {
        try (FileWriter writer = new FileWriter(stashLogFile)) {
            gson.toJson(stashLogs, writer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save stash logs", e);
        }
    }

    private void loadLogs() {
        if (!stashLogFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(stashLogFile)) {
            Type listType = new TypeToken<ArrayList<StashLog>>(){}.getType();
            List<StashLog> loadedLogs = gson.fromJson(reader, listType);
            if (loadedLogs != null) {
                stashLogs.addAll(loadedLogs);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load stash logs", e);
        }
    }

    public List<StashLog> getActiveStashLogs() {
        List<StashLog> active = new ArrayList<>();
        for (StashLog log : stashLogs) {
            if (!log.isDismissed()) {
                active.add(log);
            }
        }
        return active;
    }

    public StashLog getStashById(String id) {
        for (StashLog log : stashLogs) {
            if (log.getId().equals(id)) {
                return log;
            }
        }
        return null;
    }

    public void dismissStash(String id) {
        for (StashLog log : stashLogs) {
            if (log.getId().equals(id)) {
                log.setDismissed(true);
                saveLogs();
                return;
            }
        }
    }

    public int getTotalStashes() {
        return stashLogs.size();
    }

    public int getActiveStashes() {
        int count = 0;
        for (StashLog log : stashLogs) {
            if (!log.isDismissed()) {
                count++;
            }
        }
        return count;
    }
}
