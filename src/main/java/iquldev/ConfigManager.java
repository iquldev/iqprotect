package iquldev;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        
        String prefix = config.getString("messages.general.prefix", "<green>[<blue>iqProtect</blue>]</green>");
        message = message.replace("{prefix}", prefix);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    public int getMaxZoneDistance() {
        return config.getInt("settings.max_zone_distance", 10);
    }
    
    public int getAutoSaveInterval() {
        return config.getInt("settings.auto_save_interval", 5);
    }
    
    public int getMaxZonesPerPlayer() {
        return config.getInt("settings.max_zones_per_player", 10);
    }
    
    public int getMaxBlocksPerZone() {
        return config.getInt("settings.max_blocks_per_zone", 1000);
    }
    
    public boolean isLoggingEnabled() {
        return config.getBoolean("settings.enable_logging", true);
    }
    
    public String getLogLevel() {
        return config.getString("settings.log_level", "WARNING");
    }
    
    public String getUsePermission() {
        return config.getString("permissions.use", "iqp.use");
    }
    
    public String getBypassPermission() {
        return config.getString("permissions.bypass", "iqprotect.bypass");
    }
    
    public String getReloadPermission() {
        return config.getString("permissions.reload", "iqprotect.reload");
    }
    
    public String getAdminPermission() {
        return config.getString("permissions.admin", "iqprotect.admin");
    }
    
    public boolean isLoggingEventEnabled(String event) {
        return config.getBoolean("logging.events." + event, true);
    }
    
    public String getLogTimeFormat() {
        return config.getString("logging.time_format", "yyyy-MM-dd HH:mm");
    }
    
    public int getZoneCacheTime() {
        return config.getInt("performance.zone_cache_time", 300);
    }
    
    public int getMaxCachedZones() {
        return config.getInt("performance.max_cached_zones", 1000);
    }
    
    public boolean isAsyncDataLoading() {
        return config.getBoolean("performance.async_data_loading", true);
    }
    
    public boolean isAsyncDataSaving() {
        return config.getBoolean("performance.async_data_saving", true);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
} 