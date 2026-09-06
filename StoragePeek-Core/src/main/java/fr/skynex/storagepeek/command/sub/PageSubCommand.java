package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import fr.skynex.storagepeek.session.PeekSession;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PageSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public PageSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "page"; }

    @Override
    public String getPermission() { return "storagepeek.page"; }

    @Override
    public String getUsage() { return "/sp page <next|prev|number>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.page") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§cYou must be looking at a container to change pages!");
            return true;
        }
        int targetPage = session.getCurrentPage();
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("next")) {
                targetPage++;
            } else if (args[1].equalsIgnoreCase("prev") || args[1].equalsIgnoreCase("previous")) {
                targetPage = Math.max(0, targetPage - 1);
            } else {
                try {
                    targetPage = Math.max(0, Integer.parseInt(args[1]) - 1);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        session.setCurrentPage(targetPage);
        player.sendMessage(messageManager.getMessage("page-updated").replace("{page}", String.valueOf(targetPage + 1)));
        plugin.playConfigSound(player, "sort", Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.5f, 1.2f);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> pageOptions = Arrays.asList("next", "prev", "1", "2");
            return pageOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
