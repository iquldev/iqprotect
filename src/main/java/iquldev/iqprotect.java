package iquldev;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class iqprotect extends JavaPlugin implements Listener {
    private Map<UUID, List<Location>> playerBlocks;
    private Map<UUID, List<Zone>> playerZones;
    private Map<UUID, List<UUID>> whitelists;
    private Map<UUID, List<String>> griefAttempts;
    private Connection dbConnection;

    @Override
    public void onEnable() {
        playerBlocks = new HashMap<>();
        playerZones = new HashMap<>();
        whitelists = new HashMap<>();
        griefAttempts = new HashMap<>();

        // Инициализация SQLite
        try {
            initDatabase();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
            setEnabled(false);
            return;
        }
        loadData();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("iqp").setExecutor(this);
        getLogger().info("iqProtect has been enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to close SQLite connection: " + e.getMessage());
        }
        getLogger().info("iqProtect has been disabled!");
    }

    private void initDatabase() throws SQLException {
        // Подключение к SQLite
        dbConnection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/data.db");
        try (Statement stmt = dbConnection.createStatement()) {
            // Создание таблицы blocks
            stmt.execute("CREATE TABLE IF NOT EXISTS blocks (" +
                "player_uuid TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL)");
            // Создание таблицы whitelists
            stmt.execute("CREATE TABLE IF NOT EXISTS whitelists (" +
                "owner_uuid TEXT NOT NULL, " +
                "whitelisted_uuid TEXT NOT NULL)");
            // Создание таблицы grief_attempts
            stmt.execute("CREATE TABLE IF NOT EXISTS grief_attempts (" +
                "owner_uuid TEXT NOT NULL, " +
                "attempt TEXT NOT NULL)");
        }
    }

    private void saveData() {
        try {
            // Очистка таблиц
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute("DELETE FROM blocks");
                stmt.execute("DELETE FROM whitelists");
                stmt.execute("DELETE FROM grief_attempts");
            }

            // Сохранение playerBlocks
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO blocks (player_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)")) {
                for (Map.Entry<UUID, List<Location>> entry : playerBlocks.entrySet()) {
                    for (Location loc : entry.getValue()) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, loc.getWorld().getName());
                        stmt.setDouble(3, loc.getX());
                        stmt.setDouble(4, loc.getY());
                        stmt.setDouble(5, loc.getZ());
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }

            // Сохранение whitelists
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO whitelists (owner_uuid, whitelisted_uuid) VALUES (?, ?)")) {
                for (Map.Entry<UUID, List<UUID>> entry : whitelists.entrySet()) {
                    for (UUID uuid : entry.getValue()) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, uuid.toString());
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }

            // Сохранение griefAttempts
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO grief_attempts (owner_uuid, attempt) VALUES (?, ?)")) {
                for (Map.Entry<UUID, List<String>> entry : griefAttempts.entrySet()) {
                    for (String attempt : entry.getValue()) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, attempt);
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to save data to SQLite: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            // Загрузка playerBlocks
            try (Statement stmt = dbConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM blocks")) {
                while (rs.next()) {
                    String playerUuid = rs.getString("player_uuid");
                    String world = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    if (Bukkit.getWorld(world) != null) {
                        UUID uuid = UUID.fromString(playerUuid);
                        playerBlocks.computeIfAbsent(uuid, k -> new ArrayList<>())
                            .add(new Location(Bukkit.getWorld(world), x, y, z));
                    }
                }
            }
            // Пересчёт зон для всех игроков
            for (UUID playerId : playerBlocks.keySet()) {
                updateZones(playerId);
            }

            // Загрузка whitelists
            try (Statement stmt = dbConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM whitelists")) {
                while (rs.next()) {
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    UUID whitelistedUuid = UUID.fromString(rs.getString("whitelisted_uuid"));
                    whitelists.computeIfAbsent(ownerUuid, k -> new ArrayList<>()).add(whitelistedUuid);
                }
            }

            // Загрузка griefAttempts
            try (Statement stmt = dbConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM grief_attempts")) {
                while (rs.next()) {
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    String attempt = rs.getString("attempt");
                    griefAttempts.computeIfAbsent(ownerUuid, k -> new ArrayList<>()).add(attempt);
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to load data from SQLite: " + e.getMessage());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        if (!canInteract(player, location)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете ставить блоки в этой зоне!");
            player.updateInventory(); // Немедленное обновление инвентаря
            recordGriefAttempt(player, location, "поставить блок");
            return;
        }

        playerBlocks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(location);
        updateZones(player.getUniqueId());
        saveData();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (!canInteract(player, location)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете ломать блоки в этой зоне!");
            recordGriefAttempt(player, location, "сломать блок");
            return;
        }
        // Удаляем блок из списка, если сломан владельцем
        playerBlocks.getOrDefault(player.getUniqueId(), new ArrayList<>()).remove(location);
        updateZones(player.getUniqueId());
        saveData();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        Zone zone = getZoneAt(location);
        if (zone != null) {
            Player owner = getServer().getPlayer(zone.getOwner());
            String ownerName = owner != null ? owner.getName() : Bukkit.getOfflinePlayer(zone.getOwner()).getName();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent("§eВладелец зоны: " + (ownerName != null ? ownerName : "Неизвестно")));
        }
    }

    private boolean canInteract(Player player, Location location) {
        Zone zone = getZoneAt(location);
        if (zone == null) return true;
        UUID owner = zone.getOwner();
        if (player.getUniqueId().equals(owner)) return true;
        List<UUID> whitelist = whitelists.getOrDefault(owner, new ArrayList<>());
        boolean canInteract = whitelist.contains(player.getUniqueId());
        getLogger().info(player.getName() + " attempting to interact in " + owner + "'s zone. Whitelist contains: " + whitelist + ". Can interact: " + canInteract);
        return canInteract;
    }

    private void recordGriefAttempt(Player player, Location location, String action) {
        Zone zone = getZoneAt(location);
        if (zone != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String attempt = String.format("%s пытался %s в %s (%s)", 
                player.getName(), action, location.toString(), timestamp);
            griefAttempts.computeIfAbsent(zone.getOwner(), k -> new ArrayList<>()).add(attempt);
            getLogger().info("Recorded grief attempt: " + attempt);
            saveData();
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
        if (blocks == null || blocks.isEmpty()) {
            playerZones.put(playerId, new ArrayList<>());
            return;
        }

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e/iqp list - Список ваших зон");
            player.sendMessage("§e/iqp gui - Открыть GUI");
            player.sendMessage("§e/iqp whitelist add/remove [игрок] - Управление доступом");
            player.sendMessage("§e/iqp whitelist players - Список игроков в белом списке");
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
        } else if (args[0].equalsIgnoreCase("whitelist")) {
            if (args.length < 2) {
                player.sendMessage("§cИспользование: /iqp whitelist [add/remove/players] [игрок]");
                return true;
            }
            if (args[1].equalsIgnoreCase("players")) {
                List<UUID> whitelist = whitelists.getOrDefault(player.getUniqueId(), new ArrayList<>());
                if (whitelist.isEmpty()) {
                    player.sendMessage("§cВаш белый список пуст!");
                } else {
                    player.sendMessage("§eИгроки в белом списке:");
                    for (UUID uuid : whitelist) {
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        player.sendMessage("§a- " + (name != null ? name : uuid.toString()));
                    }
                }
            } else if (args.length >= 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                Player target = getServer().getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cИгрок не найден!");
                    return true;
                }
                List<UUID> whitelist = whitelists.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
                if (args[1].equalsIgnoreCase("add")) {
                    if (whitelist.add(target.getUniqueId())) {
                        player.sendMessage("§a" + target.getName() + " добавлен в белый список!");
                        getLogger().info(player.getName() + " added " + target.getName() + " (" + target.getUniqueId() + ") to whitelist");
                        saveData();
                    } else {
                        player.sendMessage("§c" + target.getName() + " уже в белом списке!");
                    }
                } else if (args[1].equalsIgnoreCase("remove")) {
                    if (whitelist.remove(target.getUniqueId())) {
                        player.sendMessage("§a" + target.getName() + " удалён из белого списка!");
                        getLogger().info(player.getName() + " removed " + target.getName() + " (" + target.getUniqueId() + ") from whitelist");
                        saveData();
                    } else {
                        player.sendMessage("§c" + target.getName() + " не в белом списке!");
                    }
                }
            } else {
                player.sendMessage("§cИспользование: /iqp whitelist [add/remove/players] [игрок]");
            }
        }
        return true;
    }

    private void openGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "IQ Protection");
        List<String> attempts = griefAttempts.getOrDefault(player.getUniqueId(), new ArrayList<>());

        // Элемент для попыток гриферства
        ItemStack griefItem = new ItemStack(Material.TNT);
        ItemMeta griefMeta = griefItem.getItemMeta();
        griefMeta.setDisplayName("§cПопытки гриферства");
        List<String> griefLore = new ArrayList<>();
        if (attempts.isEmpty()) {
            griefLore.add("§7Нет записей о попытках гриферства");
        } else {
            for (int i = 0; i < Math.min(attempts.size(), 5); i++) {
                griefLore.add("§7" + attempts.get(attempts.size() - 1 - i));
            }
            if (attempts.size() > 5) {
                griefLore.add("§7...и ещё " + (attempts.size() - 5) + " записей");
            }
        }
        griefMeta.setLore(griefLore);
        griefItem.setItemMeta(griefMeta);
        gui.setItem(10, griefItem);

        // Элемент для списка зон
        ItemStack zonesItem = new ItemStack(Material.MAP);
        ItemMeta zonesMeta = zonesItem.getItemMeta();
        zonesMeta.setDisplayName("§eВаши зоны");
        List<String> zonesLore = new ArrayList<>();
        List<Zone> zones = playerZones.getOrDefault(player.getUniqueId(), new ArrayList<>());
        if (zones.isEmpty()) {
            zonesLore.add("§7У вас нет зон");
        } else {
            for (int i = 0; i < Math.min(zones.size(), 5); i++) {
                zonesLore.add("§7Зона " + (i + 1) + ": " + zones.get(i).getBlocks().size() + " блоков");
            }
            if (zones.size() > 5) {
                zonesLore.add("§7...и ещё " + (zones.size() - 5) + " зон");
            }
        }
        zonesMeta.setLore(zonesLore);
        zonesItem.setItemMeta(zonesMeta);
        gui.setItem(12, zonesItem);

        // Элемент для команд
        ItemStack commandsItem = new ItemStack(Material.BOOK);
        ItemMeta commandsMeta = commandsItem.getItemMeta();
        commandsMeta.setDisplayName("§aКоманды плагина");
        commandsMeta.setLore(Arrays.asList(
            "§7/iqp list - Список зон",
            "§7/iqp gui - Открыть GUI",
            "§7/iqp whitelist add/remove [игрок]",
            "§7/iqp whitelist players"
        ));
        commandsItem.setItemMeta(commandsMeta);
        gui.setItem(14, commandsItem);

        player.openInventory(gui);
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