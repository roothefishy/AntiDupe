//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package roothefishy.dupeAlerts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DupeAlerts extends JavaPlugin implements Listener {
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getLogger().info("Patching and alerting all possible dupers");
    }


    private final Map<UUID, Map<Material, Integer>> playerInventoryMap = new HashMap<>();

    @EventHandler
    public void onInventoryChange(PlayerItemHeldEvent e) {
        checkInventory(e.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        checkInventory(e.getPlayer());
    }



    private void checkInventory(Player player) {

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
            if (newAmt > oldAmt + mat.getMaxStackSize()) { // big jump indicates potential dupe
                String alert = "§c[Anti-Dupe] " + player.getName() + " is trying to dupe " + mat.name() + "!";
                Bukkit.getConsoleSender().sendMessage(alert);
                Bukkit.getOnlinePlayers().forEach(p -> { if (p.isOp()) p.sendMessage(alert); });
            }
        }

        playerInventoryMap.put(player.getUniqueId(), currentInventory);
    }


    @EventHandler
    public void preventDupe(PlayerEditBookEvent event) {
        String title = event.getNewBookMeta().getTitle();
        if (title != null && title.length() > 15) {
            event.setCancelled(true);
            Bukkit.getLogger().log(Level.WARNING, "Player " + event.getPlayer().getName() + " is attempting to dupe!");
        }

    }
}
