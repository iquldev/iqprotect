package iquldev;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection dbConnection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() throws SQLException {
        dbConnection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/data.db");
        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS blocks (" +
                    "player_uuid TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL)");
        }
    }

    public void saveData(Map<UUID, List<Location>> playerBlocks) {
        try {
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute("DELETE FROM blocks");
            }

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
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save data: " + e.getMessage());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            Logger.error(Logger.getLogMessage("database.data_save_error", placeholders));
            e.printStackTrace();
        }
    }

    public Map<UUID, List<Location>> loadData() {
        Map<UUID, List<Location>> playerBlocks = new HashMap<>();
        try {
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
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load data: " + e.getMessage());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            Logger.error(Logger.getLogMessage("database.data_load_error", placeholders));
            e.printStackTrace();
        }
        return playerBlocks;
    }

    public void closeConnection() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQLite: " + e.getMessage());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            Logger.error(Logger.getLogMessage("database.connection_close_error", placeholders));
            e.printStackTrace();
        }
    }
} 