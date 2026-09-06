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

public class FilterSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public FilterSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "filter"; }

    @Override
    public String getPermission() { return "storagepeek.filter"; }

    @Override
    public String getUsage() { return "/sp filter <ALL|RESOURCES|FOOD|EQUIPMENT|rarity>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.filter") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cInvalid filter type! Choose from: ALL, RESOURCES, FOOD, EQUIPMENT, or /sp filter rarity <MYTHIC|LEGENDARY|EPIC|RARE|UNCOMMON|COMMON>");
            return true;
        }
        String wanted = args[1].toUpperCase().trim();
        if (args[1].equalsIgnoreCase("rarity")) {
            PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
            if (session == null) {
                player.sendMessage("§cYou must be looking at a container to apply a rarity filter.");
                return true;
            }
            String rarity = args.length > 2 ? args[2].toUpperCase() : "RESET";
            if ("RESET".equals(rarity) || "CLEAR".equals(rarity) || "ALL".equals(rarity)) {
                session.setRarityFilter(null);
                player.sendMessage("§a[StoragePeek] Rarity filter reset!");
            } else {
                session.setRarityFilter(rarity);
                player.sendMessage("§a[StoragePeek] Set 3D rarity filter to §e" + rarity + "§a!");
            }
            plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.3f);
            return true;
        }
        try {
            PeekSession.FilterType filter = PeekSession.FilterType.valueOf(wanted);
            PeekSession session = plugin.getActiveSessions().get(player.getUniqueId());
            if (session != null) {
                session.setActiveFilter(filter);
                player.sendMessage(messageManager.getMessage("filter-updated").replace("{filter}", wanted.toLowerCase()));
                plugin.playConfigSound(player, "sort", Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
            } else {
                player.sendMessage("§cYou must be looking at a container to apply a filter.");
            }
        } catch (Exception ex) {
            player.sendMessage("§cInvalid filter type! Choose from: ALL, RESOURCES, FOOD, EQUIPMENT, or /sp filter rarity <MYTHIC|LEGENDARY|EPIC|RARE|UNCOMMON|COMMON>");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> validFilters = Arrays.asList("ALL", "RESOURCES", "FOOD", "EQUIPMENT", "rarity");
            return validFilters.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("rarity")) {
            List<String> rarities = Arrays.asList("MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON", "RESET");
            return rarities.stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
