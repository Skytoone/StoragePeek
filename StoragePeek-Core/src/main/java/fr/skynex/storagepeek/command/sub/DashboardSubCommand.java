package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DashboardSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public DashboardSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "dashboard"; }

    @Override
    public String getPermission() { return "storagepeek.dashboard"; }

    @Override
    public String getUsage() { return "/sp dashboard [radius]"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        int radius = 25;
        if (args.length > 1) {
            try {
                radius = Math.min(64, Math.max(5, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        if (plugin.getStorageDashboardGUI() != null) {
            plugin.getStorageDashboardGUI().openDashboard(player, radius);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
