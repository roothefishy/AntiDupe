package roothefishy.dupeAlerts;

import org.bukkit.Location;
import java.time.Instant;
import java.util.*;

public class StashLog {
    private final String id;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Map<String, Integer> items;
    private final long timestamp;
    private final int totalItemCount;
    private final String detectionReason;
    private boolean dismissed;

    public StashLog(Location location, Map<String, Integer> items, String detectionReason) {
        this.id = UUID.randomUUID().toString();
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.items = new HashMap<>(items);
        this.timestamp = Instant.now().toEpochMilli();
        this.detectionReason = detectionReason;
        this.dismissed = false;
        
        int count = 0;
        for (int amount : items.values()) {
            count += amount;
        }
        this.totalItemCount = count;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Map<String, Integer> getItems() {
        return new HashMap<>(items);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTotalItemCount() {
        return totalItemCount;
    }

    public String getDetectionReason() {
        return detectionReason;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public String getLocationString() {
        return worldName + " (" + x + ", " + y + ", " + z + ")";
    }
}
