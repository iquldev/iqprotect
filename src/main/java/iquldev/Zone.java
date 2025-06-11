package iquldev;

import org.bukkit.Location;
import java.util.List;
import java.util.UUID;

public class Zone {
    private final UUID owner;
    private final List<Location> blocks;
    private final int maxDistance;

    public Zone(UUID owner, List<Location> blocks, int maxDistance) {
        this.owner = owner;
        this.blocks = blocks;
        this.maxDistance = maxDistance;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<Location> getBlocks() {
        return blocks;
    }

    public boolean contains(Location location) {
        for (Location block : blocks) {
            if (block.getWorld().equals(location.getWorld()) && 
                block.distance(location) <= maxDistance) {
                return true;
            }
        }
        return false;
    }

    public int getMaxDistance() {
        return maxDistance;
    }
} 