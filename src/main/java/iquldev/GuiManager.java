package iquldev;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class GuiManager {
    
    public static void openGui(Player player, Map<UUID, List<Zone>> playerZones, ConfigManager configManager) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text(configManager.getMessage("gui.title")));

        ItemStack zonesItem = new ItemStack(Material.MAP);
        ItemMeta zonesMeta = zonesItem.getItemMeta();
        if (zonesMeta != null) {
            zonesMeta.displayName(MessageUtils.format(configManager.getMessage("gui.zones_item.name")));
            List<Component> zonesLore = new ArrayList<>();
            List<Zone> zones = playerZones.getOrDefault(player.getUniqueId(), new ArrayList<>());
            if (zones.isEmpty()) {
                zonesLore.add(MessageUtils.format(configManager.getMessage("gui.zones_item.no_zones")));
            } else {
                for (int i = 0; i < Math.min(zones.size(), 5); i++) {
                    Zone zone = zones.get(i);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("zone_number", String.valueOf(i + 1));
                    placeholders.put("block_count", String.valueOf(zone.getBlocks().size()));
                    
                    String coordinates = getZoneCoordinates(zone);
                    placeholders.put("coordinates", coordinates);
                    
                    zonesLore.add(MessageUtils.format(configManager.getMessage("gui.zones_item.zone_info", placeholders)));
                }
                if (zones.size() > 5) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("count", String.valueOf(zones.size() - 5));
                    zonesLore.add(MessageUtils.format(configManager.getMessage("gui.zones_item.more_zones", placeholders)));
                }
            }
            zonesMeta.lore(zonesLore);
            zonesItem.setItemMeta(zonesMeta);
        }
        gui.setItem(10, zonesItem);

        ItemStack commandsItem = new ItemStack(Material.BOOK);
        ItemMeta commandsMeta = commandsItem.getItemMeta();
        if (commandsMeta != null) {
            commandsMeta.displayName(MessageUtils.format(configManager.getMessage("gui.commands_item.name")));
            List<Component> commandsLore = new ArrayList<>();
            commandsLore.add(MessageUtils.format(configManager.getMessage("gui.commands_item.list_command")));
            commandsLore.add(MessageUtils.format(configManager.getMessage("gui.commands_item.gui_command")));
            commandsLore.add(MessageUtils.format(configManager.getMessage("gui.commands_item.cui_command")));
            commandsLore.add(MessageUtils.format(configManager.getMessage("gui.commands_item.reload_command")));
            commandsMeta.lore(commandsLore);
            commandsItem.setItemMeta(commandsMeta);
        }
        gui.setItem(13, commandsItem);

        ItemStack permissionsItem = new ItemStack(Material.EMERALD);
        ItemMeta permissionsMeta = permissionsItem.getItemMeta();
        if (permissionsMeta != null) {
            permissionsMeta.displayName(MessageUtils.format(configManager.getMessage("gui.permissions_item.name")));
            List<Component> permissionsLore = new ArrayList<>();
            permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.bypass_permission")));
            permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.reload_permission")));
            permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.admin_permission")));
            if (player.hasPermission(configManager.getBypassPermission())) {
                permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.has_bypass")));
            } else {
                permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.no_bypass")));
            }
            if (player.hasPermission(configManager.getAdminPermission())) {
                permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.has_admin")));
            } else {
                permissionsLore.add(MessageUtils.format(configManager.getMessage("gui.permissions_item.no_admin")));
            }
            permissionsMeta.lore(permissionsLore);
            permissionsItem.setItemMeta(permissionsMeta);
        }
        gui.setItem(16, permissionsItem);

        player.openInventory(gui);
    }
    
    private static String getZoneCoordinates(Zone zone) {
        List<Location> blocks = zone.getBlocks();
        if (blocks.isEmpty()) {
            return "Unknown";
        }
        
        double avgX = blocks.stream().mapToDouble(Location::getX).average().orElse(0);
        double avgY = blocks.stream().mapToDouble(Location::getY).average().orElse(0);
        double avgZ = blocks.stream().mapToDouble(Location::getZ).average().orElse(0);
        String worldName = blocks.get(0).getWorld().getName();
        
        return String.format("%s (%.0f, %.0f, %.0f)", worldName, avgX, avgY, avgZ);
    }
} 