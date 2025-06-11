package iquldev;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    public static Component format(String message) {
        return miniMessage.deserialize(message);
    }
    
    public static Component format(String message, Map<String, String> placeholders) {
        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolver.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return miniMessage.deserialize(message, resolver.build());
    }
    
    public static void sendMessage(Player player, String message) {
        Component component = format(message);
        player.sendMessage(component);
    }
    
    public static void sendMessage(Player player, Messages message) {
        sendMessage(player, message.getMessage());
    }
    
    public static void sendMessage(Player player, Messages message, Map<String, String> placeholders) {
        String msg = message.getMessage();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        sendMessage(player, msg);
    }
    
    public static void sendActionBar(Player player, String message) {
        Component component = format(message);
        player.sendActionBar(component);
    }
    
    public static void sendActionBar(Player player, Messages message, Map<String, String> placeholders) {
        String msg = message.getMessage();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        sendActionBar(player, msg);
    }
    
    public enum Messages {
        PLAYER_ONLY("<red>Эта команда доступна только игрокам!</red>"),
        NO_PERMISSION("<red>У вас нет прав для выполнения этой команды!</red>"),
        HELP_HEADER("<gradient:#0024FF:#5567FF>=== iqProtect - Помощь ===</gradient>"),
        HELP_LIST("<gray>/iqp list - Показать список ваших зон</gray>"),
        HELP_GUI("<gray>/iqp gui - Открыть графический интерфейс</gray>"),
        HELP_RELOAD("<gray>/iqp reload - Перезагрузить плагин</gray>"),
        NO_ZONES("<gradient:#0024FF:#5567FF>У вас нет защищенных зон</gradient>"),
        ZONES_LIST_HEADER("<gradient:#0024FF:#5567FF>Ваши зоны:</gradient>"),
        ZONE_INFO("<gray>Зона {zone_number}: {block_count} блоков</gray>"),
        ZONE_OWNER("<gradient:#0024FF:#5567FF>Владелец зоны: {owner_name}</gradient>"),
        PLUGIN_RELOADING("<gradient:#0024FF:#5567FF>Перезагрузка плагина...</gradient>"),
        PLUGIN_RELOADED("<green>Плагин успешно перезагружен!</green>");

        private final String message;

        Messages(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
} 