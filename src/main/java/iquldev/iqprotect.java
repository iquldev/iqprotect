package iquldev;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.*;

public class iqprotect extends JavaPlugin implements Listener {
    private Map<UUID, List<Location>> playerBlocks;
    private Map<UUID, List<Zone>> playerZones;
    private Map<UUID, List<UUID>> whitelists;
    private Map<UUID, List<String>> griefAttempts;

    @Override
    public void onEnable() {
        playerBlocks = new HashMap<>();
        playerZones = new HashMap<>();
        whitelists = new HashMap<>();
        griefAttempts = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("iqp").setExecutor(this);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        playerBlocks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(location);
        updateZones(player.getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (!canInteract(player, location)) {
            event.setCancelled(true);
            recordGriefAttempt(player, location);
            player.sendMessage("§cВы не можете ломать блоки в этой зоне!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        Zone zone = getZoneAt(location);
        if (zone != null) {
            Player owner = getServer().getPlayer(zone.getOwner());
            String ownerName = owner != null ? owner.getName() : "Неизвестно";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent("§eВладелец зоны: " + ownerName));
        }
    }

    private boolean canInteract(Player player, Location location) {
        Zone zone = getZoneAt(location);
        if (zone == null) return true;
        UUID owner = zone.getOwner();
        if (player.getUniqueId().equals(owner)) return true;
        List<UUID> whitelist = whitelists.getOrDefault(owner, new ArrayList<>());
        return whitelist.contains(player.getUniqueId());
    }

    private void recordGriefAttempt(Player player, Location location) {
        Zone zone = getZoneAt(location);
        if (zone != null) {
            griefAttempts.computeIfAbsent(zone.getOwner(), k -> new ArrayList<>())
                .add(player.getName() + " пытался сломать блок в " + location.toString());
        }
    }

    private Zone getZoneAt(Location location) {
        for (List<Zone> zones : playerZones.values()) {
            for (Zone zone : zones) {
                if (zone.contains(location)) return zone;
            }
        }
        return null;
    }

    private void updateZones(UUID playerId) {
        List<Location> blocks = playerBlocks.get(playerId);
        if (blocks == null || blocks.isEmpty()) return;

        List<Zone> newZones = new ArrayList<>();
        List<Location> remainingBlocks = new ArrayList<>(blocks);

        while (!remainingBlocks.isEmpty()) {
            List<Location> cluster = new ArrayList<>();
            Location start = remainingBlocks.remove(0);
            cluster.add(start);

            Iterator<Location> iterator = remainingBlocks.iterator();
            while (iterator.hasNext()) {
                Location loc = iterator.next();
                for (Location placed : cluster) {
                    if (loc.distance(placed) <= 10) {
                        cluster.add(loc);
                        iterator.remove();
                        break;
                    }
                }
            }
            newZones.add(new Zone(playerId, cluster));
        }
        playerZones.put(playerId, newZones);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e/iqp list - Список ваших зон");
            player.sendMessage("§e/iqp gui - Открыть GUI");
            player.sendMessage("§e/iqp whitelist add/remove [игрок] - Управление доступом");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            List<Zone> zones = playerZones.getOrDefault(player.getUniqueId(), new ArrayList<>());
            if (zones.isEmpty()) {
                player.sendMessage("§cУ вас нет зон!");
            } else {
                player.sendMessage("§eВаши зоны:");
                for (int i = 0; i < zones.size(); i++) {
                    player.sendMessage("§a" + (i + 1) + ": " + zones.get(i).getBlocks().size() + " блоков");
                }
            }
        } else if (args[0].equalsIgnoreCase("gui")) {
            openGui(player);
        } else if (args[0].equalsIgnoreCase("whitelist") && args.length >= 3) {
            Player target = getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage("§cИгрок не найден!");
                return true;
            }
            List<UUID> whitelist = whitelists.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
            if (args[1].equalsIgnoreCase("add")) {
                if (whitelist.add(target.getUniqueId())) {
                    player.sendMessage("§a" + target.getName() + " добавлен в белый список!");
                }
            } else if (args[1].equalsIgnoreCase("remove")) {
                if (whitelist.remove(target.getUniqueId())) {
                    player.sendMessage("§a" + target.getName() + " удалён из белого списка!");
                }
            }
        }
        return true;
    }

    private void openGui(Player player) {
        // Здесь должно быть создание инвентаря с использованием Bukkit API
        // Пример упрощённого текстового вывода вместо GUI:
        player.sendMessage("§e=== IQ Protection GUI ===");
        List<String> attempts = griefAttempts.getOrDefault(player.getUniqueId(), new ArrayList<>());
        player.sendMessage("§eПопытки гриферства:");
        for (String attempt : attempts) {
            player.sendMessage("§c- " + attempt);
        }
        player.sendMessage("§eКоманды: /iqp list, /iqp gui, /iqp whitelist add/remove [игрок]");
    }
}

class Zone {
    private UUID owner;
    private List<Location> blocks;

    public Zone(UUID owner, List<Location> blocks) {
        this.owner = owner;
        this.blocks = blocks;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<Location> getBlocks() {
        return blocks;
    }

    public boolean contains(Location location) {
        for (Location block : blocks) {
            if (block.distance(location) <= 10) return true;
        }
        return false;
    }
}