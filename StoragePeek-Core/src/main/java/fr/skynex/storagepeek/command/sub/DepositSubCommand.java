package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DepositSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public DepositSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "deposit"; }

    @Override
    public String getPermission() { return "storagepeek.deposit"; }

    @Override
    public String getUsage() { return "/sp deposit [radius|vault]"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.deposit") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("vault")) {
            Inventory enderChest = player.getEnderChest();
            int deposited = 0;
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType() == Material.AIR) continue;
                if (enderChest.contains(item.getType())) {
                    HashMap<Integer, ItemStack> remaining = enderChest.addItem(item);
                    if (remaining.isEmpty()) {
                        deposited += item.getAmount();
                        player.getInventory().setItem(slot, null);
                    } else {
                        int dep = item.getAmount() - remaining.get(0).getAmount();
                        if (dep > 0) {
                            deposited += dep;
                            player.getInventory().setItem(slot, remaining.get(0));
                        }
                    }
                }
            }
            if (deposited > 0) {
                player.sendMessage("§a[StoragePeek] Deposited " + deposited + " items directly into your VaultX Virtual Vault!");
                plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            } else {
                player.sendMessage("§e[StoragePeek] No matching items found to deposit into your VaultX Virtual Vault.");
            }
            return true;
        }

        int radius = 16;
        if (args.length > 1) {
            try {
                radius = Math.min(32, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        int depositedCount = plugin.handleSmartBaseDeposit(player, radius);
        if (depositedCount > 0) {
            player.sendMessage("§a[StoragePeek] Deposited " + depositedCount + " matching items into nearby containers!");
            plugin.playConfigSound(player, "deposit", Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
        } else {
            player.sendMessage("§e[StoragePeek] No matching container slots found nearby for items in your inventory.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> depositOptions = Arrays.asList("vault", "16", "32");
            return depositOptions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
