package iquldev;

import org.bukkit.Location;
import java.util.*;
import java.util.UUID;

public class ZoneManager {
    private final Map<UUID, List<Location>> playerBlocks;
    private final Map<UUID, List<Zone>> playerZones;
    private ConfigManager configManager;

    public ZoneManager() {
        this.playerBlocks = new HashMap<>();
        this.playerZones = new HashMap<>();
    }
    
    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setPlayerBlocks(Map<UUID, List<Location>> playerBlocks) {
        this.playerBlocks.clear();
        this.playerBlocks.putAll(playerBlocks);
        updateAllZones();
    }

    public Map<UUID, List<Location>> getPlayerBlocks() {
        return new HashMap<>(playerBlocks);
    }

    public Map<UUID, List<Zone>> getPlayerZones() {
        return new HashMap<>(playerZones);
    }

    public void addBlock(UUID playerId, Location location) {
        List<Location> playerBlocksList = playerBlocks.getOrDefault(playerId, new ArrayList<>());
        
        if (configManager != null) {
            int maxBlocks = configManager.getMaxBlocksPerZone();
            if (playerBlocksList.size() >= maxBlocks) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player_id", playerId.toString());
                placeholders.put("max_blocks", String.valueOf(maxBlocks));
                Logger.warning(Logger.getLogMessage("zones.block_limit_reached", placeholders));
                return;
            }
        }
        
        playerBlocksList.add(location);
        playerBlocks.put(playerId, playerBlocksList);
        updateZones(playerId);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_id", playerId.toString());
        placeholders.put("block_count", String.valueOf(playerBlocksList.size()));
        Logger.info(Logger.getLogMessage("zones.block_added", placeholders));
    }

    public void removeBlock(UUID playerId, Location location) {
        List<Location> playerBlocksList = playerBlocks.getOrDefault(playerId, new ArrayList<>());
        boolean removed = playerBlocksList.remove(location);
        if (removed) {
            playerBlocks.put(playerId, playerBlocksList);
            updateZones(playerId);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_id", playerId.toString());
            placeholders.put("block_count", String.valueOf(playerBlocksList.size()));
            Logger.info(Logger.getLogMessage("zones.block_removed", placeholders));
        }
    }

    public Zone getZoneAt(Location location) {
        for (Map.Entry<UUID, List<Zone>> entry : playerZones.entrySet()) {
            for (Zone zone : entry.getValue()) {
                for (Location block : zone.getBlocks()) {
                    if (block.getWorld().equals(location.getWorld()) && 
                        block.distance(location) <= zone.getMaxDistance()) {
                        return zone;
                    }
                }
            }
        }
        return null;
    }

    public boolean canInteract(UUID playerUUID, Location location, boolean hasBypass) {
        if (hasBypass) {
            return true;
        }
        
        Zone zone = getZoneAt(location);
        if (zone == null) {
            return true;
        }

        UUID zoneOwner = zone.getOwner();
        boolean canInteract = playerUUID.equals(zoneOwner);
        if (!canInteract) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_uuid", playerUUID.toString());
            placeholders.put("zone_owner", zoneOwner.toString());
            placeholders.put("location", locationToString(location));
            Logger.warning(Logger.getLogMessage("zones.violation_attempt", placeholders));
        }
        return canInteract;
    }

    private void updateAllZones() {
        for (UUID playerId : playerBlocks.keySet()) {
            updateZones(playerId);
        }
    }

    private void updateZones(UUID playerId) {
        List<Location> blocks = playerBlocks.get(playerId);
        if (blocks == null || blocks.isEmpty()) {
            playerZones.put(playerId, new ArrayList<>());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player_id", playerId.toString());
            Logger.info(Logger.getLogMessage("zones.zones_cleared", placeholders));
            return;
        }

        List<Zone> newZones = new ArrayList<>();
        int maxDistance = configManager != null ? configManager.getMaxZoneDistance() : 10;

        newZones.add(new Zone(playerId, new ArrayList<>(blocks), maxDistance));
        
        playerZones.put(playerId, newZones);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_id", playerId.toString());
        placeholders.put("zone_count", String.valueOf(newZones.size()));
        placeholders.put("block_count", String.valueOf(blocks.size()));
        Logger.info(Logger.getLogMessage("zones.zones_updated", placeholders));
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
} 