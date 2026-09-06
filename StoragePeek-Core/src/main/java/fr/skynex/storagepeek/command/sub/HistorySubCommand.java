package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.ContainerHistoryManager;
import fr.skynex.storagepeek.manager.MessageManager;
import fr.skynex.storagepeek.util.FoliaScheduler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.util.Collections;
import java.util.List;

public class HistorySubCommand implements SubCommand {

    private final StoragePeek plugin;

    public HistorySubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "history"; }

    @Override
    public String getPermission() { return "storagepeek.history"; }

    @Override
    public String getUsage() { return "/sp history"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.history") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || (!plugin.getHookManager().isCustomContainer(targetBlock)
                && !plugin.getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
            player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
            return true;
        }
        List<ContainerHistoryManager.AccessLog> logs = plugin.getContainerHistoryManager().getLogs(targetBlock.getLocation());
        if (logs.isEmpty()) {
            player.sendMessage(messageManager.getMessage("history-empty"));
            return true;
        }
        StringBuilder sb = new StringBuilder("§6§l📜 CONTAINER ACCESS HISTORY\n");
        long now = System.currentTimeMillis();
        for (ContainerHistoryManager.AccessLog log : logs) {
            long secAgo = Math.max(1, (now - log.timestamp()) / 1000);
            String agoStr = secAgo < 60 ? secAgo + "s ago" : (secAgo / 60) + "m ago";
            sb.append("§7• §e").append(log.playerName()).append(" §7- ").append(log.action()).append(" §8(")
                    .append(agoStr).append(")\n");
        }
        final String historyText = sb.toString().trim();

        Location loc = targetBlock.getLocation().add(0.5, 1.35, 0.5);
        TextDisplay textDisplay = loc.getWorld().spawn(loc, TextDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setVisibleByDefault(false);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setDefaultBackground(true);
            ent.setBackgroundColor(Color.fromARGB(200, 20, 20, 30));
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.text(LegacyComponentSerializer.legacySection().deserialize(historyText));
            Transformation t = ent.getTransformation();
            t.getScale().set(0.7f, 0.7f, 0.7f);
            ent.setTransformation(t);
        });
        player.showEntity(plugin, textDisplay);
        plugin.playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        player.sendMessage("§a[StoragePeek] Displaying 3D audit hologram above container!");

        FoliaScheduler.runLater(plugin, player, () -> {
            if (textDisplay.isValid()) {
                textDisplay.remove();
            }
        }, 200L);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
