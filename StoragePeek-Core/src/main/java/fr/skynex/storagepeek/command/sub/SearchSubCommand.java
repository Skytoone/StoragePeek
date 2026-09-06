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

public class SearchSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public SearchSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "search"; }

    @Override
    public String getPermission() { return "storagepeek.search"; }

    @Override
    public String getUsage() { return "/sp search <query|reset>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.search") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§cYou must be looking at a container to use search!");
            return true;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("clear")) {
            session.setSearchQuery(null);
            player.sendMessage(messageManager.getMessage("search-cleared"));
            plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        } else {
            String query = args[1].trim();
            session.setSearchQuery(query);
            player.sendMessage(messageManager.getMessage("search-updated").replace("{query}", query));
            plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> searchOptions = Arrays.asList("reset", "clear");
            return searchOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
