package fr.skynex.storagepeek.command;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.sub.*;
import fr.skynex.storagepeek.manager.MessageManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class StoragePeekCommand implements CommandExecutor, TabCompleter {

    private final StoragePeek plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public StoragePeekCommand(StoragePeek plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        register(new ReloadSubCommand(plugin));
        register(new ToggleSubCommand(plugin));
        register(new ThemesSubCommand(plugin));
        register(new ThemeSubCommand(plugin));
        register(new FilterSubCommand(plugin));
        register(new DashboardSubCommand(plugin));
        register(new PurgeSubCommand(plugin));
        register(new FindSubCommand(plugin));
        register(new DepositSubCommand(plugin));
        register(new VaultSubCommand(plugin));
        register(new HistorySubCommand(plugin));
        register(new SearchSubCommand(plugin));
        register(new PageSubCommand(plugin));
        register(new LabelSubCommand(plugin));
        register(new CreateThemeSubCommand(plugin));
        register(new StatsSubCommand(plugin));
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();

        if (args.length > 0) {
            String subName = args[0].toLowerCase();
            if (subName.equals("unlabel")) subName = "label"; // alias for label

            SubCommand subCommand = subCommands.get(subName);
            if (subCommand != null) {
                if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
                    sender.sendMessage(messageManager.getMessage("only-players"));
                    return true;
                }
                return subCommand.execute(sender, args);
            }
        }

        sender.sendMessage(messageManager.getMessage("usage-reload"));
        sender.sendMessage(messageManager.getMessage("usage-toggle"));
        sender.sendMessage(messageManager.getMessage("usage-themes"));
        sender.sendMessage(messageManager.getMessage("usage-filter"));
        sender.sendMessage(messageManager.getMessage("usage-history"));
        sender.sendMessage(messageManager.getMessage("usage-search"));
        sender.sendMessage(messageManager.getMessage("usage-page"));
        sender.sendMessage("§e/storagepeek label <text> §7- Attach persistent 3D label to targeted container.");
        sender.sendMessage("§e/storagepeek createtheme <name> §7- Create new 3D theme using held block.");
        sender.sendMessage("§e/storagepeek stats [radius] §7- Display 3D base storage statistics dashboard.");
        sender.sendMessage("§e/storagepeek find <item> §7- Point compass arrow to nearby chest containing item.");
        sender.sendMessage("§e/storagepeek deposit [radius] §7- Auto-deposit matching items into nearby chests.");
        sender.sendMessage("§e/storagepeek purge §7- Purge orphaned display entities.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(s -> s.startsWith(input))
                    .sorted()
                    .toList();
        }
        if (args.length > 1) {
            String subName = args[0].toLowerCase();
            if (subName.equals("unlabel")) subName = "label";
            SubCommand subCommand = subCommands.get(subName);
            if (subCommand != null) {
                return subCommand.tabComplete(sender, args);
            }
        }
        return Collections.emptyList();
    }
}
