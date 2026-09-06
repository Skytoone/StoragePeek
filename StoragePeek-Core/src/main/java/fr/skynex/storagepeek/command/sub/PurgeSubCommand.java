package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PurgeSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public PurgeSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "purge"; }

    @Override
    public String getPermission() { return "storagepeek.purge"; }

    @Override
    public String getUsage() { return "/sp purge"; }

    @Override
    public boolean isPlayerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!sender.hasPermission("storagepeek.purge") && !sender.hasPermission("storagepeek.admin")) {
            sender.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        int purged = plugin.purgeOrphanedEntities();
        sender.sendMessage("§aPurged " + purged + " orphaned StoragePeek display entities across all loaded chunks.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
