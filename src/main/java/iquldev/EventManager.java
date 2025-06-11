package iquldev;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

public class EventManager implements Listener {
    private final ZoneManager zoneManager;
    private final ConfigManager configManager;

    public EventManager(ZoneManager zoneManager, DatabaseManager databaseManager, ConfigManager configManager) {
        this.zoneManager = zoneManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        Zone zone = zoneManager.getZoneAt(block.getLocation());
        if (zone != null && !zone.getOwner().equals(player.getUniqueId()) && !player.hasPermission(configManager.getBypassPermission())) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("protection.block_in_protected_zone"));
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_name", player.getName());
            placeholders.put("location", locationToString(block.getLocation()));
            Logger.warning(Logger.getLogMessage("protection.block_place_protected_zone", placeholders));
            return;
        }
        
        zoneManager.addBlock(player.getUniqueId(), block.getLocation());
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        Zone zone = zoneManager.getZoneAt(block.getLocation());
        if (zone != null && !zone.getOwner().equals(player.getUniqueId()) && !player.hasPermission(configManager.getBypassPermission())) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("protection.block_in_protected_zone"));
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_name", player.getName());
            placeholders.put("location", locationToString(block.getLocation()));
            Logger.warning(Logger.getLogMessage("protection.block_break_protected_zone", placeholders));
            return;
        }
        
        zoneManager.removeBlock(player.getUniqueId(), block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean hasBypass = player.hasPermission(configManager.getBypassPermission());

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Zone zone = zoneManager.getZoneAt(block.getLocation());
        if (zone == null) {
            return;
        }

        if (!zoneManager.canInteract(player.getUniqueId(), block.getLocation(), hasBypass)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            MessageUtils.sendMessage(player, configManager.getMessage("protection.block_interact_denied"));
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_name", player.getName());
            placeholders.put("location", locationToString(block.getLocation()));
            Logger.warning(Logger.getLogMessage("protection.block_interact_protected_zone", placeholders));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        Zone zone = zoneManager.getZoneAt(location);
        if (zone != null && !zone.getBlocks().isEmpty()) {
            String ownerName = Bukkit.getOfflinePlayer(zone.getOwner()).getName();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("owner_name", ownerName != null ? ownerName : "Unknown");
            MessageUtils.sendActionBar(player, configManager.getMessage("protection.zone_owner_actionbar", placeholders));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text(configManager.getMessage("gui.title")))) {
            event.setCancelled(true);
        }
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
} 