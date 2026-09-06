package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public ReloadSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "reload"; }

    @Override
    public String getPermission() { return "storagepeek.reload"; }

    @Override
    public String getUsage() { return "/sp reload"; }

    @Override
    public boolean isPlayerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!sender.hasPermission("storagepeek.reload") && !sender.hasPermission("storagepeek.admin")) {
            sender.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        plugin.reloadConfig();
        plugin.loadConfigurationCache();
        messageManager.reloadConfig();
        plugin.reloadRaycastTasks();
        sender.sendMessage(messageManager.getMessage("reload-success"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
