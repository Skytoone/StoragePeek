package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class StatsSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public StatsSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "stats"; }

    @Override
    public String getPermission() { return "storagepeek.stats"; }

    @Override
    public String getUsage() { return "/sp stats [radius]"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.stats") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        int radius = 32;
        if (args.length > 1) {
            try {
                radius = Math.min(64, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        plugin.displayBaseStatsHologram(player, radius);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
