package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FindSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public FindSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "find"; }

    @Override
    public String getPermission() { return "storagepeek.find"; }

    @Override
    public String getUsage() { return "/sp find <material>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.find") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cInvalid item material! Example: /sp find DIAMOND");
            return true;
        }
        String wantedName = args[1].toUpperCase().trim();
        Material mat = Material.matchMaterial(wantedName);
        if (mat == null) {
            player.sendMessage("§cInvalid item material! Example: /sp find DIAMOND");
            return true;
        }

        fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
        List<Block> containers = apiImpl.findNearbyContainers(player.getLocation(), 32.0, mat);

        if (containers.isEmpty()) {
            player.sendMessage("§cNo nearby containers containing " + mat.name() + " were found within 32 blocks.");
            return true;
        }

        Block nearest = containers.get(0);
        player.sendMessage("§aFound " + containers.size() + " container(s) with " + mat.name()
                + "! Pointing compass arrow to nearest container.");
        plugin.getRaycastTask().setCompassTarget(player, nearest.getLocation().add(0.5, 0.5, 0.5));
        plugin.startGPSWaypointTask(player, nearest);
        plugin.playConfigSound(player, "sort", Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8f, 1.2f);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
