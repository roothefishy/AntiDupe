package roothefishy.dupeAlerts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class StashGUI implements Listener {
    private final DupeAlerts plugin;
    private final StashLogger logger;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<String, String> openGUIs = new HashMap<>();
    
    private static final int STASHES_PER_PAGE = 28;

    public StashGUI(DupeAlerts plugin, StashLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void openMainGUI(Player player) {
        playerPages.put(player.getUniqueId(), 0);
        showStashesPage(player, 0);
    }

    private void showStashesPage(Player player, int page) {
        List<StashLog> stashes = logger.getActiveStashLogs();
        int totalPages = (int) Math.ceil(stashes.size() / (double) STASHES_PER_PAGE);
        
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        Inventory gui = Bukkit.createInventory(null, 54, "§6§lDupe Stashes §7(Page " + (page + 1) + "/" + Math.max(1, totalPages) + ")");
        
        int startIndex = page * STASHES_PER_PAGE;
        int endIndex = Math.min(startIndex + STASHES_PER_PAGE, stashes.size());

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");

        for (int i = startIndex; i < endIndex; i++) {
            StashLog stash = stashes.get(i);
            int slot = i - startIndex;
            
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§e" + stash.getLocationString());
                List<String> lore = new ArrayList<>();
                lore.add("§7Time: §f" + sdf.format(new Date(stash.getTimestamp())));
                lore.add("§7Total Items: §a" + stash.getTotalItemCount());
                lore.add("§7Reason: §6" + stash.getDetectionReason());
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
            infoMeta.setDisplayName("§b§lStash Statistics");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Total Stashes: §e" + logger.getTotalStashes());
            infoLore.add("§7Active Stashes: §c" + logger.getActiveStashes());
            infoLore.add("§7Dismissed: §a" + (logger.getTotalStashes() - logger.getActiveStashes()));
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);

        openGUIs.put(player.getUniqueId().toString(), "MAIN");
        player.openInventory(gui);
    }

    private void showDetailGUI(Player player, StashLog stash) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lStash Details: " + stash.getId().substring(0, 8));

        ItemStack locationItem = new ItemStack(Material.COMPASS);
        ItemMeta locationMeta = locationItem.getItemMeta();
        if (locationMeta != null) {
            locationMeta.setDisplayName("§e" + stash.getLocationString());
            List<String> lore = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lore.add("§7Time: §f" + sdf.format(new Date(stash.getTimestamp())));
            lore.add("");
            lore.add("§7Total Items: §a" + stash.getTotalItemCount());
            lore.add("");
            lore.add("§6Detection Reason:");
            lore.add("§f" + stash.getDetectionReason());
            locationMeta.setLore(lore);
            locationItem.setItemMeta(locationMeta);
        }
        gui.setItem(4, locationItem);

        Map<String, Integer> items = stash.getItems();
        int itemSlot = 18;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (itemSlot >= 35) break;
            
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
            gui.setItem(itemSlot, item);
            itemSlot++;
        }

        ItemStack teleportButton = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportMeta = teleportButton.getItemMeta();
        if (teleportMeta != null) {
            teleportMeta.setDisplayName("§b§lTELEPORT TO STASH");
            List<String> teleportLore = new ArrayList<>();
            teleportLore.add("§7Click to teleport to");
            teleportLore.add("§7this stash location");
            teleportMeta.setLore(teleportLore);
            teleportButton.setItemMeta(teleportMeta);
        }
        gui.setItem(11, teleportButton);

        ItemStack dismissButton = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta dismissMeta = dismissButton.getItemMeta();
        if (dismissMeta != null) {
            dismissMeta.setDisplayName("§e§lDISMISS STASH");
            List<String> dismissLore = new ArrayList<>();
            dismissLore.add("§7Click to dismiss this stash");
            dismissLore.add("§7(Not suspicious)");
            dismissMeta.setLore(dismissLore);
            dismissButton.setItemMeta(dismissMeta);
        }
        gui.setItem(13, dismissButton);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to Stashes");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(15, backButton);

        openGUIs.put(player.getUniqueId().toString(), "DETAIL:" + stash.getId());
        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            if (title.contains("Dupe Stashes") || title.contains("Stash Details")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String currentGUI = openGUIs.get(player.getUniqueId().toString());
                    if (currentGUI != null) {
                        String currentTitle = player.getOpenInventory().getTitle();
                        if (!currentTitle.contains("Dupe Stashes") && !currentTitle.contains("Stash Details")) {
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
            showStashesPage(player, currentPage - 1);
            playerPages.put(player.getUniqueId(), currentPage - 1);
        } else if (slot == 53) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            showStashesPage(player, currentPage + 1);
            playerPages.put(player.getUniqueId(), currentPage + 1);
        } else if (slot < STASHES_PER_PAGE) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            List<StashLog> stashes = logger.getActiveStashLogs();
            int stashIndex = currentPage * STASHES_PER_PAGE + slot;
            
            if (stashIndex < stashes.size()) {
                StashLog stash = stashes.get(stashIndex);
                showDetailGUI(player, stash);
            }
        }
    }

    private void handleDetailGUIClick(Player player, InventoryClickEvent event, String stashId) {
        int slot = event.getSlot();
        StashLog stash = logger.getStashById(stashId);
        
        if (stash == null) {
            player.sendMessage("§cError: Stash not found!");
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            Location location = new Location(
                Bukkit.getWorld(stash.getWorldName()),
                stash.getX() + 0.5,
                stash.getY() + 1,
                stash.getZ() + 0.5
            );
            
            if (location.getWorld() == null) {
                player.sendMessage("§cError: World not found!");
                return;
            }
            
            player.teleport(location);
            player.sendMessage("§aTeleported to stash location!");
            player.closeInventory();
        } else if (slot == 13) {
            logger.dismissStash(stashId);
            player.sendMessage("§eStash dismissed");
            player.closeInventory();
            openMainGUI(player);
        } else if (slot == 15) {
            player.closeInventory();
            openMainGUI(player);
        }
    }

    public void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId().toString());
        playerPages.remove(player.getUniqueId());
    }
}
