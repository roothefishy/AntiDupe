package roothefishy.dupeAlerts;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Chest;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.Location;

import java.util.*;
import java.util.logging.Level;

public class DupeAlerts extends JavaPlugin implements Listener {
    private DupeLogger dupeLogger;
    private DupeGUI dupeGUI;
    private StashLogger stashLogger;
    private StashGUI stashGUI;
    
    private final Map<UUID, Map<Material, Integer>> playerInventoryMap = new HashMap<>();
    private final Map<UUID, Long> lastInventoryCheck = new HashMap<>();
    private final Map<UUID, Integer> itemFrameInteractions = new HashMap<>();
    private final Map<UUID, Integer> craftingActions = new HashMap<>();
    private final Map<UUID, Long> lastAlertTime = new HashMap<>();
    private final Set<String> checkedChests = new HashSet<>();
    
    private static final long INVENTORY_CHECK_COOLDOWN = 100;
    private static final long ALERT_COOLDOWN = 5000;
    private static final int ITEM_FRAME_THRESHOLD = 10;
    private static final int CRAFTING_THRESHOLD = 20;
    private static final int STASH_ITEM_THRESHOLD = 500;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        dupeLogger = new DupeLogger(getDataFolder(), getLogger());
        dupeGUI = new DupeGUI(this, dupeLogger);
        stashLogger = new StashLogger(getDataFolder(), getLogger());
        stashGUI = new StashGUI(this, stashLogger);
        
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(dupeGUI, this);
        Bukkit.getPluginManager().registerEvents(stashGUI, this);
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            dupeLogger.archiveOldLogs(30);
        }, 20L * 60L * 60L, 20L * 60L * 60L);
        
        getLogger().info("§a================================");
        getLogger().info("§aDupeAlerts v2.1 Enabled!");
        getLogger().info("§aEnhanced detection active");
        getLogger().info("§aActive logs: " + dupeLogger.getActiveDupeLogs());
        getLogger().info("§aActive stashes: " + stashLogger.getActiveStashes());
        getLogger().info("§a================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("DupeAlerts disabled. Logs saved.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dupelogs")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("dupealerts.staff")) {
                player.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            
            dupeGUI.openMainGUI(player);
            return true;
        } else if (command.getName().equalsIgnoreCase("stashes")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("dupealerts.staff")) {
                player.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            
            stashGUI.openMainGUI(player);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryChange(PlayerItemHeldEvent event) {
        checkInventory(event.getPlayer(), "Hotbar Switch");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            checkInventory(event.getPlayer(), "Player Join");
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                checkInventory(player, "Inventory Click");
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                checkInventory(player, "Inventory Close");
            }, 2L);
            
            if (event.getInventory().getHolder() instanceof Chest) {
                Chest chest = (Chest) event.getInventory().getHolder();
                checkChestForStash(chest.getLocation(), event.getInventory());
            } else if (event.getInventory().getHolder() instanceof DoubleChest) {
                DoubleChest doubleChest = (DoubleChest) event.getInventory().getHolder();
                Chest leftSide = (Chest) doubleChest.getLeftSide();
                if (leftSide != null) {
                    checkChestForStash(leftSide.getLocation(), event.getInventory());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                checkInventory(player, "Item Pickup");
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            checkInventory(event.getPlayer(), "Item Drop");
        }, 2L);
    }

    private void checkInventory(Player player, String context) {
        if (player.hasPermission("dupealerts.bypass")) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastInventoryCheck.get(player.getUniqueId());
        if (lastCheck != null && currentTime - lastCheck < INVENTORY_CHECK_COOLDOWN) {
            return;
        }
        lastInventoryCheck.put(player.getUniqueId(), currentTime);

        Map<Material, Integer> lastInventory = playerInventoryMap.getOrDefault(player.getUniqueId(), new HashMap<>());
        Map<Material, Integer> currentInventory = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                currentInventory.put(item.getType(), currentInventory.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        for (Material mat : currentInventory.keySet()) {
            int oldAmt = lastInventory.getOrDefault(mat, 0);
            int newAmt = currentInventory.get(mat);
            
            int maxStackSize = mat.getMaxStackSize();
            int suspiciousThreshold = oldAmt + (maxStackSize * 2);
            
            if (newAmt > suspiciousThreshold && newAmt - oldAmt > maxStackSize) {
                DupeLog log = new DupeLog(
                    player.getUniqueId(),
                    player.getName(),
                    mat.name(),
                    oldAmt,
                    newAmt,
                    "Inventory Jump - " + context + " (+" + (newAmt - oldAmt) + " items)"
                );
                
                Location loc = player.getLocation();
                log.setLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                
                Map<String, Integer> inventoryBeforeSnapshot = new HashMap<>();
                for (Material m : lastInventory.keySet()) {
                    inventoryBeforeSnapshot.put(m.name(), lastInventory.get(m));
                }
                log.setInventoryBefore(inventoryBeforeSnapshot);
                
                Map<String, Integer> inventoryAfterSnapshot = new HashMap<>();
                for (Material m : currentInventory.keySet()) {
                    inventoryAfterSnapshot.put(m.name(), currentInventory.get(m));
                }
                log.setInventoryAfter(inventoryAfterSnapshot);
                
                dupeLogger.logDupe(log);
                sendAlert(player, mat.name(), oldAmt, newAmt, "Inventory Jump");
            }
        }

        playerInventoryMap.put(player.getUniqueId(), currentInventory);
    }

    @EventHandler
    public void preventBookDupe(PlayerEditBookEvent event) {
        String title = event.getNewBookMeta().getTitle();
        if (title != null && title.length() > 16) {
            event.setCancelled(true);
            
            Player player = event.getPlayer();
            DupeLog log = new DupeLog(
                player.getUniqueId(),
                player.getName(),
                "WRITABLE_BOOK",
                0,
                1,
                "Book Title Exploit - Title length: " + title.length()
            );
            
            Location loc = player.getLocation();
            log.setLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            
            Map<String, Integer> currentInventory = new HashMap<>();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    String matName = item.getType().name();
                    currentInventory.put(matName, currentInventory.getOrDefault(matName, 0) + item.getAmount());
                }
            }
            log.setInventoryAfter(currentInventory);
            
            dupeLogger.logDupe(log);
            sendAlert(player, "BOOK", 0, 1, "Book Title Exploit");
            player.sendMessage("§c[Anti-Dupe] Book title too long! Dupe attempt blocked.");
        }
    }

    @EventHandler
    public void onItemFrameBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player && event.getEntity() instanceof ItemFrame) {
            Player player = (Player) event.getRemover();
            ItemFrame frame = (ItemFrame) event.getEntity();
            ItemStack item = frame.getItem();
            
            if (item != null && item.getType() != Material.AIR) {
                UUID uuid = player.getUniqueId();
                int interactions = itemFrameInteractions.getOrDefault(uuid, 0) + 1;
                itemFrameInteractions.put(uuid, interactions);
                
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    itemFrameInteractions.put(uuid, Math.max(0, itemFrameInteractions.getOrDefault(uuid, 0) - 1));
                }, 20L * 10L);
                
                if (interactions > ITEM_FRAME_THRESHOLD) {
                    DupeLog log = new DupeLog(
                        player.getUniqueId(),
                        player.getName(),
                        item.getType().name(),
                        0,
                        item.getAmount(),
                        "Item Frame Spam - " + interactions + " interactions in 10s"
                    );
                    
                    Location loc = player.getLocation();
                    log.setLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    
                    Map<String, Integer> currentInventory = new HashMap<>();
                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem != null && invItem.getType() != Material.AIR) {
                            String matName = invItem.getType().name();
                            currentInventory.put(matName, currentInventory.getOrDefault(matName, 0) + invItem.getAmount());
                        }
                    }
                    log.setInventoryAfter(currentInventory);
                    
                    dupeLogger.logDupe(log);
                    sendAlert(player, item.getType().name(), 0, item.getAmount(), "Item Frame Exploit");
                }
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getRecipe().getResult();
            
            UUID uuid = player.getUniqueId();
            int crafts = craftingActions.getOrDefault(uuid, 0) + 1;
            craftingActions.put(uuid, crafts);
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                craftingActions.put(uuid, Math.max(0, craftingActions.getOrDefault(uuid, 0) - 1));
            }, 20L * 5L);
            
            if (crafts > CRAFTING_THRESHOLD) {
                DupeLog log = new DupeLog(
                    player.getUniqueId(),
                    player.getName(),
                    result.getType().name(),
                    0,
                    result.getAmount(),
                    "Rapid Crafting - " + crafts + " crafts in 5s (Auto-clicker suspected)"
                );
                
                Location loc = player.getLocation();
                log.setLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                
                Map<String, Integer> currentInventory = new HashMap<>();
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.getType() != Material.AIR) {
                        String matName = invItem.getType().name();
                        currentInventory.put(matName, currentInventory.getOrDefault(matName, 0) + invItem.getAmount());
                    }
                }
                log.setInventoryAfter(currentInventory);
                
                dupeLogger.logDupe(log);
                sendAlert(player, result.getType().name(), 0, result.getAmount(), "Crafting Exploit");
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Enderman) {
            Enderman enderman = (Enderman) entity;
            org.bukkit.block.data.BlockData carried = enderman.getCarriedBlock();
            
            if (carried != null) {
                Material carriedMaterial = carried.getMaterial();
                if (carriedMaterial != Material.AIR) {
                    Player player = event.getPlayer();
                    DupeLog log = new DupeLog(
                        player.getUniqueId(),
                        player.getName(),
                        carriedMaterial.name(),
                        0,
                        1,
                        "Enderman Interaction - Suspicious entity manipulation"
                    );
                    
                    Location loc = player.getLocation();
                    log.setLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    
                    Map<String, Integer> currentInventory = new HashMap<>();
                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem != null && invItem.getType() != Material.AIR) {
                            String matName = invItem.getType().name();
                            currentInventory.put(matName, currentInventory.getOrDefault(matName, 0) + invItem.getAmount());
                        }
                    }
                    log.setInventoryAfter(currentInventory);
                    
                    dupeLogger.logDupe(log);
                    sendAlert(player, carriedMaterial.name(), 0, 1, "Enderman Exploit");
                }
            }
        }
    }

    private void sendAlert(Player player, String material, int oldAmt, int newAmt, String method) {
        long currentTime = System.currentTimeMillis();
        Long lastAlert = lastAlertTime.get(player.getUniqueId());
        
        if (lastAlert != null && currentTime - lastAlert < ALERT_COOLDOWN) {
            return;
        }
        lastAlertTime.put(player.getUniqueId(), currentTime);

        String alert = String.format(
            "§c§l[DUPE ALERT] §e%s §7(%s) §c%s §7| Before: §a%d §7| After: §c%d §7| +§e%d",
            player.getName(),
            method,
            material,
            oldAmt,
            newAmt,
            newAmt - oldAmt
        );
        
        Bukkit.getConsoleSender().sendMessage(alert);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("dupealerts.alerts")) {
                p.sendMessage(alert);
                p.sendMessage("§7Use §e/dupelogs §7to view details");
            }
        }
    }

    private void checkChestForStash(Location location, Inventory inventory) {
        String chestKey = location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        
        if (checkedChests.contains(chestKey)) {
            return;
        }

        Map<String, Integer> items = new HashMap<>();
        int totalItems = 0;
        int suspiciousItemCount = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                String matName = item.getType().name();
                int amount = item.getAmount();
                items.put(matName, items.getOrDefault(matName, 0) + amount);
                totalItems += amount;

                if (amount >= 64) {
                    suspiciousItemCount++;
                }
            }
        }

        if (totalItems >= STASH_ITEM_THRESHOLD || suspiciousItemCount >= 10) {
            String reason;
            if (totalItems >= STASH_ITEM_THRESHOLD && suspiciousItemCount >= 10) {
                reason = "High item count (" + totalItems + " items) with " + suspiciousItemCount + " max stacks";
            } else if (totalItems >= STASH_ITEM_THRESHOLD) {
                reason = "High item count (" + totalItems + " items)";
            } else {
                reason = suspiciousItemCount + " max stacks of items";
            }

            StashLog stashLog = new StashLog(location, items, reason);
            stashLogger.logStash(stashLog);
            checkedChests.add(chestKey);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("dupealerts.alerts")) {
                    p.sendMessage("§6§l[STASH ALERT] §eFound suspicious chest at " + stashLog.getLocationString());
                    p.sendMessage("§7Use §e/stashes §7to view details");
                }
            }
        }
    }
}
