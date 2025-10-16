package roothefishy.dupeAlerts;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DupeLog {
    private final String id;
    private final UUID playerUUID;
    private final String playerName;
    private final String material;
    private final int oldAmount;
    private final int newAmount;
    private final String dupeMethod;
    private final long timestamp;
    private boolean dismissed;
    private boolean flagged;
    private Map<String, Integer> inventoryBefore;
    private Map<String, Integer> inventoryAfter;
    private String worldName;
    private int x;
    private int y;
    private int z;

    public DupeLog(UUID playerUUID, String playerName, String material, int oldAmount, int newAmount, String dupeMethod) {
        this.id = UUID.randomUUID().toString();
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.material = material;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.dupeMethod = dupeMethod;
        this.timestamp = Instant.now().toEpochMilli();
        this.dismissed = false;
        this.flagged = false;
        this.inventoryBefore = new HashMap<>();
        this.inventoryAfter = new HashMap<>();
        this.worldName = "";
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public String getId() {
        return id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMaterial() {
        return material;
    }

    public int getOldAmount() {
        return oldAmount;
    }

    public int getNewAmount() {
        return newAmount;
    }

    public String getDupeMethod() {
        return dupeMethod;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public int getDifference() {
        return newAmount - oldAmount;
    }

    public Map<String, Integer> getInventoryBefore() {
        if (inventoryBefore == null) {
            inventoryBefore = new HashMap<>();
        }
        return inventoryBefore;
    }

    public void setInventoryBefore(Map<String, Integer> inventoryBefore) {
        this.inventoryBefore = inventoryBefore;
    }

    public Map<String, Integer> getInventoryAfter() {
        if (inventoryAfter == null) {
            inventoryAfter = new HashMap<>();
        }
        return inventoryAfter;
    }

    public void setInventoryAfter(Map<String, Integer> inventoryAfter) {
        this.inventoryAfter = inventoryAfter;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setLocation(String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
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

    public String getLocationString() {
        if (worldName == null || worldName.isEmpty()) {
            return "Unknown Location";
        }
        return worldName + " (" + x + ", " + y + ", " + z + ")";
    }
}
