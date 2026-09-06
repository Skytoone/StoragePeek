package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;

public class ToggleSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public ToggleSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "toggle"; }

    @Override
    public String getPermission() { return "storagepeek.toggle"; }

    @Override
    public String getUsage() { return "/sp toggle"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.toggle")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey disabledKey = plugin.getDisabledKey();
        if (pdc.has(disabledKey, PersistentDataType.BYTE)) {
            pdc.remove(disabledKey);
            plugin.getDisabledPlayers().remove(player.getUniqueId());
            player.sendMessage(messageManager.getMessage("toggle-enabled"));
        } else {
            pdc.set(disabledKey, PersistentDataType.BYTE, (byte) 1);
            plugin.getDisabledPlayers().add(player.getUniqueId());
            player.sendMessage(messageManager.getMessage("toggle-disabled"));
            PeekSession session = plugin.getActiveSessions().remove(player.getUniqueId());
            if (session != null) {
                session.cleanup(true);
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
