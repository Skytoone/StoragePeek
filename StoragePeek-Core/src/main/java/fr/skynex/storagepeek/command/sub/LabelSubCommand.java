package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;

public class LabelSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public LabelSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "label"; }

    @Override
    public String getPermission() { return "storagepeek.label"; }

    @Override
    public String getUsage() { return "/sp label <text>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.label") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || (!plugin.getHookManager().isCustomContainer(targetBlock)
                && !plugin.getRaycastTask().getAllowedBlocks().contains(targetBlock.getType()))) {
            player.sendMessage("§cYou must be looking at a valid container block within 5 blocks!");
            return true;
        }

        if (!(targetBlock.getState() instanceof TileState tileState)) {
            player.sendMessage("§cThis block type cannot store persistent labels.");
            return true;
        }

        NamespacedKey labelKey = plugin.getLabelKey();
        if (args[0].equalsIgnoreCase("unlabel") || args.length == 1) {
            tileState.getPersistentDataContainer().remove(labelKey);
            tileState.update();
            player.sendMessage("§a[StoragePeek] Removed 3D label from container!");
            plugin.playConfigSound(player, "sort", Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.2f);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String labelText = sb.toString().trim().replace("&", "§");
            tileState.getPersistentDataContainer().set(labelKey, PersistentDataType.STRING, labelText);
            tileState.update();
            player.sendMessage("§a[StoragePeek] Set 3D container label to: §f" + labelText);
            plugin.playConfigSound(player, "sort", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
