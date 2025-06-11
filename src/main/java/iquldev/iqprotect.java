package iquldev;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public class iqprotect extends JavaPlugin {
    private ZoneManager zoneManager;
    private DatabaseManager databaseManager;
    private EventManager eventManager;
    private CommandManager commandManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        Logger.setConfigManager(configManager);
        
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.initDatabase();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("plugin_name", getName());
            Logger.info(Logger.getLogMessage("system.plugin_enabled", placeholders));
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            Logger.error(Logger.getLogMessage("system.database_init_error", placeholders));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        zoneManager = new ZoneManager();
        zoneManager.setConfigManager(configManager);
        
        Map<UUID, List<Location>> loadedData = databaseManager.loadData();
        zoneManager.setPlayerBlocks(loadedData);
        
        eventManager = new EventManager(zoneManager, databaseManager, configManager);
        commandManager = new CommandManager(zoneManager, databaseManager, this);
        
        getServer().getPluginManager().registerEvents(eventManager, this);
        getCommand("iqp").setExecutor(commandManager);
        getCommand("iqp").setTabCompleter(new CommandTabCompleter(configManager));
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("plugin_name", getName());
        placeholders.put("version", getPluginMeta().getVersion());
        Logger.info(Logger.getLogMessage("system.plugin_loaded", placeholders));
    }

    @Override
    public void onDisable() {
        if (zoneManager != null && databaseManager != null) {
            databaseManager.saveData(zoneManager.getPlayerBlocks());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("plugin_name", getName());
            Logger.info(Logger.getLogMessage("system.plugin_disabled", placeholders));
        }
        
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    public void reloadPlugin() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("plugin_name", getName());
        Logger.info(Logger.getLogMessage("system.plugin_reload_start", placeholders));
        
        configManager.reloadConfig();
        Logger.setConfigManager(configManager);
        
        zoneManager.setConfigManager(configManager);
        
        if (commandManager != null) {
            commandManager.updateManagers();
        }
        
        Logger.info(Logger.getLogMessage("system.plugin_reload_complete", placeholders));
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}