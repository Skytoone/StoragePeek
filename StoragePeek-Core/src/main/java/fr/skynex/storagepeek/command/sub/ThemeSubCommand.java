package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ThemeSubCommand implements SubCommand {

    private final StoragePeek plugin;
    private static final List<String> VALID_THEMES = Arrays.asList(
            "default", "ender", "rich", "aqua", "nether", "neon", "cyberpunk", "rainbow"
    );

    public ThemeSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "theme"; }

    @Override
    public String getPermission() { return "storagepeek.theme"; }

    @Override
    public String getUsage() { return "/sp theme <name>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cInvalid theme! Choose from: " + String.join(", ", VALID_THEMES));
            return true;
        }
        String wanted = args[1].toLowerCase().trim();
        if (!VALID_THEMES.contains(wanted)) {
            player.sendMessage("§cInvalid theme! Choose from: " + String.join(", ", VALID_THEMES));
            return true;
        }
        if (!wanted.equals("default") && !player.hasPermission("storagepeek.theme." + wanted)) {
            player.sendMessage(messageManager.getMessage("theme-no-permission").replace("{theme}", wanted));
            return true;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(plugin.getThemeKey(), PersistentDataType.STRING, wanted);
        player.sendMessage(messageManager.getMessage("theme-updated").replace("{theme}", wanted));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return VALID_THEMES.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
