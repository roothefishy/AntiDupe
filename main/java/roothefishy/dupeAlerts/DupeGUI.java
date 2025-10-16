package roothefishy.dupeAlerts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class DupeGUI implements Listener {
    private final DupeAlerts plugin;
    private final DupeLogger logger;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<String, String> openGUIs = new HashMap<>();
    
    private static final int LOGS_PER_PAGE = 28;

    public DupeGUI(DupeAlerts plugin, DupeLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void openMainGUI(Player player) {
        playerPages.put(player.getUniqueId(), 0);
        showLogsPage(player, 0);
    }

    private void showLogsPage(Player player, int page) {
        List<DupeLog> logs = logger.getNonDismissedLogs();
        int totalPages = (int) Math.ceil(logs.size() / (double) LOGS_PER_PAGE);
        
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        Inventory gui = Bukkit.createInventory(null, 54, "§c§lDupe Logs §7(Page " + (page + 1) + "/" + Math.max(1, totalPages) + ")");
        
        int startIndex = page * LOGS_PER_PAGE;
        int endIndex = Math.min(startIndex + LOGS_PER_PAGE, logs.size());

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");

        for (int i = startIndex; i < endIndex; i++) {
            DupeLog log = logs.get(i);
            int slot = i - startIndex;
            
            Material material;
            try {
                material = Material.valueOf(log.getMaterial());
            } catch (IllegalArgumentException e) {
                material = Material.BARRIER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§e" + log.getPlayerName() + " §7- §c" + log.getMaterial());
                List<String> lore = new ArrayList<>();
                lore.add("§7Time: §f" + sdf.format(new Date(log.getTimestamp())));
                lore.add("§7Before: §a" + log.getOldAmount());
                lore.add("§7After: §c" + log.getNewAmount());
                lore.add("§7Difference: §e+" + log.getDifference());
                lore.add("§7Method: §6" + log.getDupeMethod());
                lore.add("");
                lore.add("§eClick to view details");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
        }

        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aPrevious Page");
                prevPage.setItemMeta(meta);
            }
            gui.setItem(45, prevPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aNext Page");
                nextPage.setItemMeta(meta);
            }
            gui.setItem(53, nextPage);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§b§lDupe Statistics");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Total Logs: §e" + logger.getTotalLogs());
            infoLore.add("§7Active Alerts: §c" + logger.getActiveDupeLogs());
            infoLore.add("§7Dismissed: §a" + (logger.getTotalLogs() - logger.getActiveDupeLogs()));
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);

        openGUIs.put(player.getUniqueId().toString(), "MAIN");
        player.openInventory(gui);
    }

    private void showInventoryComparisonGUI(Player player, DupeLog log) {
        showInventoryBeforeGUI(player, log);
    }

    private void showInventoryBeforeGUI(Player player, DupeLog log) {
        Inventory gui = Bukkit.createInventory(null, 54, "§c§lBEFORE Inventory: " + log.getPlayerName());

        ItemStack label = new ItemStack(Material.RED_STAINED_GLASS);
        ItemMeta labelMeta = label.getItemMeta();
        if (labelMeta != null) {
            labelMeta.setDisplayName("§c§lBEFORE INVENTORY");
            List<String> lore = new ArrayList<>();
            lore.add("§7This shows the player's");
            lore.add("§7inventory BEFORE the dupe");
            labelMeta.setLore(lore);
            label.setItemMeta(labelMeta);
        }
        gui.setItem(4, label);

        Map<String, Integer> inventoryBefore = log.getInventoryBefore();

        if (inventoryBefore.isEmpty()) {
            ItemStack noData = new ItemStack(Material.BARRIER);
            ItemMeta noDataMeta = noData.getItemMeta();
            if (noDataMeta != null) {
                noDataMeta.setDisplayName("§c§lNo Inventory Data");
                List<String> lore = new ArrayList<>();
                lore.add("§7This log was created before");
                lore.add("§7inventory tracking was added.");
                noDataMeta.setLore(lore);
                noData.setItemMeta(noDataMeta);
            }
            gui.setItem(22, noData);
        } else {
            int slot = 9;
            for (Map.Entry<String, Integer> entry : inventoryBefore.entrySet()) {
                if (slot >= 45) break;
                
                Material material;
                try {
                    material = Material.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    material = Material.BARRIER;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e" + entry.getKey());
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Amount: §f" + entry.getValue());
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                gui.setItem(slot, item);
                slot++;
            }
        }

        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName("§a§lNEXT: View AFTER Inventory");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to see the inventory");
            lore.add("§7AFTER the dupe occurred");
            nextMeta.setLore(lore);
            nextButton.setItemMeta(nextMeta);
        }
        gui.setItem(53, nextButton);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to Details");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(49, backButton);

        openGUIs.put(player.getUniqueId().toString(), "INVENTORY_BEFORE:" + log.getId());
        player.openInventory(gui);
    }

    private void showInventoryAfterGUI(Player player, DupeLog log) {
        Inventory gui = Bukkit.createInventory(null, 54, "§a§lAFTER Inventory: " + log.getPlayerName());

        ItemStack label = new ItemStack(Material.GREEN_STAINED_GLASS);
        ItemMeta labelMeta = label.getItemMeta();
        if (labelMeta != null) {
            labelMeta.setDisplayName("§a§lAFTER INVENTORY");
            List<String> lore = new ArrayList<>();
            lore.add("§7This shows the player's");
            lore.add("§7inventory AFTER the dupe");
            labelMeta.setLore(lore);
            label.setItemMeta(labelMeta);
        }
        gui.setItem(4, label);

        Map<String, Integer> inventoryBefore = log.getInventoryBefore();
        Map<String, Integer> inventoryAfter = log.getInventoryAfter();

        if (inventoryAfter.isEmpty()) {
            ItemStack noData = new ItemStack(Material.BARRIER);
            ItemMeta noDataMeta = noData.getItemMeta();
            if (noDataMeta != null) {
                noDataMeta.setDisplayName("§c§lNo Inventory Data");
                List<String> lore = new ArrayList<>();
                lore.add("§7This log was created before");
                lore.add("§7inventory tracking was added.");
                noDataMeta.setLore(lore);
                noData.setItemMeta(noDataMeta);
            }
            gui.setItem(22, noData);
        } else {
            int slot = 9;
            for (Map.Entry<String, Integer> entry : inventoryAfter.entrySet()) {
                if (slot >= 45) break;
                
                Material material;
                try {
                    material = Material.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    material = Material.BARRIER;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Integer beforeAmount = inventoryBefore.getOrDefault(entry.getKey(), 0);
                    Integer afterAmount = entry.getValue();
                    int difference = afterAmount - beforeAmount;
                    
                    String colorCode = difference > 0 ? "§a" : (difference < 0 ? "§c" : "§7");
                    
                    meta.setDisplayName("§e" + entry.getKey());
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Amount: §f" + afterAmount);
                    if (difference != 0) {
                        lore.add("§7Change: " + colorCode + (difference > 0 ? "+" : "") + difference);
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                gui.setItem(slot, item);
                slot++;
            }
        }

        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName("§e§lPREVIOUS: View BEFORE Inventory");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to see the inventory");
            lore.add("§7BEFORE the dupe occurred");
            prevMeta.setLore(lore);
            prevButton.setItemMeta(prevMeta);
        }
        gui.setItem(45, prevButton);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to Details");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(49, backButton);

        openGUIs.put(player.getUniqueId().toString(), "INVENTORY_AFTER:" + log.getId());
        player.openInventory(gui);
    }

    private void showDetailGUI(Player player, DupeLog log) {
        Inventory gui = Bukkit.createInventory(null, 27, "§c§lDupe Details: " + log.getPlayerName());

        Material material;
        try {
            material = Material.valueOf(log.getMaterial());
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }

        ItemStack dupeItem = new ItemStack(material);
        ItemMeta dupeMeta = dupeItem.getItemMeta();
        if (dupeMeta != null) {
            dupeMeta.setDisplayName("§e" + log.getMaterial());
            List<String> lore = new ArrayList<>();
            lore.add("§7Player: §f" + log.getPlayerName());
            lore.add("§7UUID: §f" + log.getPlayerUUID().toString());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lore.add("§7Time: §f" + sdf.format(new Date(log.getTimestamp())));
            lore.add("");
            lore.add("§7Amount Before: §a" + log.getOldAmount());
            lore.add("§7Amount After: §c" + log.getNewAmount());
            lore.add("§7Increase: §e+" + log.getDifference());
            lore.add("");
            lore.add("§6Detection Method:");
            lore.add("§f" + log.getDupeMethod());
            dupeMeta.setLore(lore);
            dupeItem.setItemMeta(dupeMeta);
        }
        gui.setItem(13, dupeItem);

        ItemStack teleportButton = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportMeta = teleportButton.getItemMeta();
        if (teleportMeta != null) {
            teleportMeta.setDisplayName("§d§lTELEPORT TO LOCATION");
            List<String> teleportLore = new ArrayList<>();
            if (log.getWorldName() != null && !log.getWorldName().isEmpty()) {
                teleportLore.add("§7Click to teleport to");
                teleportLore.add("§7where the dupe happened");
                teleportLore.add("§e" + log.getLocationString());
            } else {
                teleportLore.add("§cNo location data available");
            }
            teleportMeta.setLore(teleportLore);
            teleportButton.setItemMeta(teleportMeta);
        }
        gui.setItem(9, teleportButton);

        ItemStack banButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta banMeta = banButton.getItemMeta();
        if (banMeta != null) {
            banMeta.setDisplayName("§c§lBAN PLAYER");
            List<String> banLore = new ArrayList<>();
            banLore.add("§7Click to ban this player");
            banLore.add("§7for duping items");
            banMeta.setLore(banLore);
            banButton.setItemMeta(banMeta);
        }
        gui.setItem(10, banButton);

        ItemStack dismissButton = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta dismissMeta = dismissButton.getItemMeta();
        if (dismissMeta != null) {
            dismissMeta.setDisplayName("§e§lDISMISS LOG");
            List<String> dismissLore = new ArrayList<>();
            dismissLore.add("§7Click to dismiss this log");
            dismissLore.add("§7(False positive)");
            dismissMeta.setLore(dismissLore);
            dismissButton.setItemMeta(dismissMeta);
        }
        gui.setItem(12, dismissButton);

        ItemStack flagButton = new ItemStack(Material.ORANGE_CONCRETE);
        ItemMeta flagMeta = flagButton.getItemMeta();
        if (flagMeta != null) {
            flagMeta.setDisplayName("§6§lFLAG FOR REVIEW");
            List<String> flagLore = new ArrayList<>();
            flagLore.add("§7Click to flag this log");
            flagLore.add("§7for further investigation");
            flagMeta.setLore(flagLore);
            flagButton.setItemMeta(flagMeta);
        }
        gui.setItem(14, flagButton);

        ItemStack inventoryButton = new ItemStack(Material.CHEST);
        ItemMeta inventoryMeta = inventoryButton.getItemMeta();
        if (inventoryMeta != null) {
            inventoryMeta.setDisplayName("§b§lVIEW INVENTORY CHANGES");
            List<String> inventoryLore = new ArrayList<>();
            inventoryLore.add("§7Click to see before/after");
            inventoryLore.add("§7inventory comparison");
            inventoryMeta.setLore(inventoryLore);
            inventoryButton.setItemMeta(inventoryMeta);
        }
        gui.setItem(15, inventoryButton);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to Logs");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(17, backButton);

        openGUIs.put(player.getUniqueId().toString(), "DETAIL:" + log.getId());
        player.openInventory(gui);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String guiType = openGUIs.get(player.getUniqueId().toString());
        if (guiType == null) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (guiType.equals("MAIN")) {
            handleMainGUIClick(player, event);
        } else if (guiType.startsWith("DETAIL:")) {
            handleDetailGUIClick(player, event, guiType.substring(7));
        } else if (guiType.startsWith("INVENTORY_BEFORE:")) {
            handleInventoryBeforeClick(player, event, guiType.substring(17));
        } else if (guiType.startsWith("INVENTORY_AFTER:")) {
            handleInventoryAfterClick(player, event, guiType.substring(16));
        } else if (guiType.startsWith("INVENTORY:")) {
            handleInventoryComparisonClick(player, event, guiType.substring(10));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            if (title.contains("Dupe Logs") || title.contains("Dupe Details") || title.contains("Inventory Comparison") || 
                title.contains("BEFORE Inventory") || title.contains("AFTER Inventory")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String currentGUI = openGUIs.get(player.getUniqueId().toString());
                    if (currentGUI != null) {
                        String currentTitle = player.getOpenInventory().getTitle();
                        if (!currentTitle.contains("Dupe Logs") && !currentTitle.contains("Dupe Details") && 
                            !currentTitle.contains("Inventory Comparison") && !currentTitle.contains("BEFORE Inventory") && 
                            !currentTitle.contains("AFTER Inventory")) {
                            closeGUI(player);
                        }
                    }
                }, 2L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        closeGUI(event.getPlayer());
    }

    private void handleMainGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        
        if (slot == 45) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            showLogsPage(player, currentPage - 1);
            playerPages.put(player.getUniqueId(), currentPage - 1);
        } else if (slot == 53) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            showLogsPage(player, currentPage + 1);
            playerPages.put(player.getUniqueId(), currentPage + 1);
        } else if (slot < LOGS_PER_PAGE) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            List<DupeLog> logs = logger.getNonDismissedLogs();
            int logIndex = currentPage * LOGS_PER_PAGE + slot;
            
            if (logIndex < logs.size()) {
                DupeLog log = logs.get(logIndex);
                showDetailGUI(player, log);
            }
        }
    }

    private void handleDetailGUIClick(Player player, InventoryClickEvent event, String logId) {
        int slot = event.getSlot();
        DupeLog log = logger.getLogById(logId);
        
        if (log == null) {
            player.sendMessage("§cError: Log not found!");
            player.closeInventory();
            return;
        }

        if (slot == 9) {
            if (log.getWorldName() != null && !log.getWorldName().isEmpty()) {
                Location location = new Location(
                    Bukkit.getWorld(log.getWorldName()),
                    log.getX() + 0.5,
                    log.getY() + 1,
                    log.getZ() + 0.5
                );
                
                if (location.getWorld() == null) {
                    player.sendMessage("§cError: World not found!");
                    return;
                }
                
                player.teleport(location);
                player.sendMessage("§aTeleported to dupe location!");
                player.closeInventory();
            } else {
                player.sendMessage("§cNo location data available for this log!");
            }
        } else if (slot == 10) {
            String banReason = "Duping items - " + log.getDupeMethod();
            boolean banSuccess = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + log.getPlayerName() + " " + banReason);
            
            if (banSuccess) {
                logger.dismissLog(logId);
                player.sendMessage("§aBanned " + log.getPlayerName() + " for duping!");
                player.closeInventory();
                openMainGUI(player);
            } else {
                player.sendMessage("§cFailed to ban player! Check console for errors.");
            }
        } else if (slot == 12) {
            logger.dismissLog(logId);
            player.sendMessage("§eLog dismissed as false positive");
            player.closeInventory();
            openMainGUI(player);
        } else if (slot == 14) {
            logger.flagLog(logId);
            player.sendMessage("§6Log flagged for further review");
            player.closeInventory();
            openMainGUI(player);
        } else if (slot == 15) {
            showInventoryComparisonGUI(player, log);
        } else if (slot == 17) {
            player.closeInventory();
            openMainGUI(player);
        }
    }

    private void handleInventoryBeforeClick(Player player, InventoryClickEvent event, String logId) {
        int slot = event.getSlot();
        DupeLog log = logger.getLogById(logId);
        
        if (log == null) {
            player.sendMessage("§cError: Log not found!");
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            showInventoryAfterGUI(player, log);
        } else if (slot == 49) {
            showDetailGUI(player, log);
        }
    }

    private void handleInventoryAfterClick(Player player, InventoryClickEvent event, String logId) {
        int slot = event.getSlot();
        DupeLog log = logger.getLogById(logId);
        
        if (log == null) {
            player.sendMessage("§cError: Log not found!");
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            showInventoryBeforeGUI(player, log);
        } else if (slot == 49) {
            showDetailGUI(player, log);
        }
    }

    private void handleInventoryComparisonClick(Player player, InventoryClickEvent event, String logId) {
        int slot = event.getSlot();
        DupeLog log = logger.getLogById(logId);
        
        if (log == null) {
            player.sendMessage("§cError: Log not found!");
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            showDetailGUI(player, log);
        }
    }

    public void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId().toString());
        playerPages.remove(player.getUniqueId());
    }
}
