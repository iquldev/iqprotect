package iquldev;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {
    private final ConfigManager configManager;

    public CommandTabCompleter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            
            subCommands.addAll(Arrays.asList("list", "gui", "cui", "help"));
            
            if (sender.hasPermission(configManager.getReloadPermission())) {
                subCommands.add("reload");
            }
            
            if (sender.hasPermission(configManager.getAdminPermission())) {
                subCommands.add("remove");
            }
            
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(subCommand -> subCommand.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return completions;
    }
} 