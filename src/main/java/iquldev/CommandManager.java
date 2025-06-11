package iquldev;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandManager implements CommandExecutor {
    private final iqprotect plugin;
    private ZoneManager zoneManager;
    private ConfigManager configManager;

    public CommandManager(ZoneManager zoneManager, DatabaseManager databaseManager, iqprotect plugin) {
        this.zoneManager = zoneManager;
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void updateManagers() {
        this.zoneManager = plugin.getZoneManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage((Player) sender, configManager.getMessage("general.player_only"));
            Logger.warning(Logger.getLogMessage("commands.console_execute"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleListCommand(player);
                break;
            case "gui":
                handleGuiCommand(player);
                break;
            case "reload":
                handleReloadCommand(player);
                break;
            case "cui":
                handleCuiCommand(player);
                break;
            case "remove":
                handleRemoveCommand(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        if (!player.hasPermission(configManager.getUsePermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("general.no_permission"));
            return;
        }
        
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.header"));
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.list"));
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.gui"));
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.cui"));
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.reload"));
        
        if (player.hasPermission(configManager.getAdminPermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.help.remove"));
        }
        
        MessageUtils.sendMessage(player, configManager.getMessage("commands.help.help"));
    }

    private void handleListCommand(Player player) {
        if (!player.hasPermission(configManager.getUsePermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("general.no_permission"));
            return;
        }
        
        Map<UUID, List<Zone>> playerZones = zoneManager.getPlayerZones();
        List<Zone> zones = playerZones.getOrDefault(player.getUniqueId(), new ArrayList<>());
        if (zones.isEmpty()) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.list.no_zones"));
        } else {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.list.header"));
            for (int i = 0; i < zones.size(); i++) {
                Zone zone = zones.get(i);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("zone_number", String.valueOf(i + 1));
                placeholders.put("block_count", String.valueOf(zone.getBlocks().size()));
                
                String coordinates = getZoneCoordinates(zone);
                placeholders.put("coordinates", coordinates);
                
                String teleportCoords = getTeleportCoordinates(zone);
                placeholders.put("teleport_coordinates", teleportCoords);
                
                MessageUtils.sendMessage(player, configManager.getMessage("commands.list.zone_info", placeholders));
            }
            Map<String, String> footerPlaceholders = new HashMap<>();
            footerPlaceholders.put("total_zones", String.valueOf(zones.size()));
            MessageUtils.sendMessage(player, configManager.getMessage("commands.list.footer", footerPlaceholders));
        }
    }
    
    private String getZoneCoordinates(Zone zone) {
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
    
    private String getTeleportCoordinates(Zone zone) {
        List<Location> blocks = zone.getBlocks();
        if (blocks.isEmpty()) {
            return "0 64 0";
        }
        
        double avgX = blocks.stream().mapToDouble(Location::getX).average().orElse(0);
        double avgY = blocks.stream().mapToDouble(Location::getY).average().orElse(0);
        double avgZ = blocks.stream().mapToDouble(Location::getZ).average().orElse(0);
        
        return String.format("%.0f %.0f %.0f", avgX, avgY, avgZ);
    }

    private void handleGuiCommand(Player player) {
        if (!player.hasPermission(configManager.getUsePermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("general.no_permission"));
            return;
        }
        
        GuiManager.openGui(player, zoneManager.getPlayerZones(), configManager);
    }

    private void handleReloadCommand(Player player) {
        if (!player.hasPermission(configManager.getReloadPermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.reload.no_permission"));
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_name", player.getName());
            Logger.warning(Logger.getLogMessage("commands.reload_no_permission", placeholders));
            return;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", player.getName());
        Logger.info(Logger.getLogMessage("commands.reload_start", placeholders));
        MessageUtils.sendMessage(player, configManager.getMessage("general.plugin_reloading"));
        
        plugin.reloadPlugin();
        
        updateManagers();
        
        MessageUtils.sendMessage(player, configManager.getMessage("commands.reload.success"));
        Logger.info(Logger.getLogMessage("commands.reload_success", placeholders));
    }

    private void handleCuiCommand(Player player) {
        if (!player.hasPermission(configManager.getUsePermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("general.no_permission"));
            return;
        }
        
        Map<UUID, List<Zone>> playerZones = zoneManager.getPlayerZones();
        List<Zone> zones = playerZones.getOrDefault(player.getUniqueId(), new ArrayList<>());
        
        if (zones.isEmpty()) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.list.no_zones"));
            return;
        }
        
        MessageUtils.sendMessage(player, configManager.getMessage("commands.cui.showing_zones"));
        
        for (Zone zone : zones) {
            showZoneBoundaries(player, zone);
        }
    }
    
    private void showZoneBoundaries(Player player, Zone zone) {
        List<Location> blocks = zone.getBlocks();
        if (blocks.isEmpty()) {
            return;
        }
        
        int maxDistance = zone.getMaxDistance();
        
        for (Location block : blocks) {
            for (int x = -maxDistance; x <= maxDistance; x++) {
                for (int y = -maxDistance; y <= maxDistance; y++) {
                    for (int z = -maxDistance; z <= maxDistance; z++) {
                        Location borderLoc = block.clone().add(x, y, z);
                        
                        double distance = block.distance(borderLoc);
                        if (Math.abs(distance - maxDistance) < 0.5) {
                            player.spawnParticle(org.bukkit.Particle.END_ROD, borderLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    private void handleRemoveCommand(Player player) {
        if (!player.hasPermission(configManager.getAdminPermission())) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.remove.no_permission"));
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_name", player.getName());
            Logger.warning(Logger.getLogMessage("commands.remove_no_permission", placeholders));
            return;
        }
        
        Location playerLocation = player.getLocation();
        Zone zoneToRemove = zoneManager.getZoneAt(playerLocation);
        
        if (zoneToRemove == null) {
            MessageUtils.sendMessage(player, configManager.getMessage("commands.remove.no_zone_at_location"));
            return;
        }
        
        UUID zoneOwner = zoneToRemove.getOwner();
        String ownerName = plugin.getServer().getOfflinePlayer(zoneOwner).getName();
        if (ownerName == null) {
            ownerName = zoneOwner.toString();
        }
        
        List<Location> zoneBlocks = zoneToRemove.getBlocks();
        for (Location blockLocation : zoneBlocks) {
            zoneManager.removeBlock(zoneOwner, blockLocation);
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("owner_name", ownerName);
        placeholders.put("block_count", String.valueOf(zoneBlocks.size()));
        placeholders.put("coordinates", getZoneCoordinates(zoneToRemove));
        
        MessageUtils.sendMessage(player, configManager.getMessage("commands.remove.success", placeholders));
        
        placeholders.put("admin_name", player.getName());
        Logger.info(Logger.getLogMessage("commands.remove_success", placeholders));
        
        Player ownerPlayer = plugin.getServer().getPlayer(zoneOwner);
        if (ownerPlayer != null && ownerPlayer.isOnline()) {
            Map<String, String> ownerPlaceholders = new HashMap<>();
            ownerPlaceholders.put("admin_name", player.getName());
            ownerPlaceholders.put("coordinates", getZoneCoordinates(zoneToRemove));
            MessageUtils.sendMessage(ownerPlayer, configManager.getMessage("commands.remove.zone_removed_by_admin", ownerPlaceholders));
        }
    }
} 