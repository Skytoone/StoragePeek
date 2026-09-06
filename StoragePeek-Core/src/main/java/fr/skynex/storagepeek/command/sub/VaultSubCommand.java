package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.List;

public class VaultSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public VaultSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "vault"; }

    @Override
    public String getPermission() { return "storagepeek.vault"; }

    @Override
    public String getUsage() { return "/sp vault [number]"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.vault") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        int vaultNum = 1;
        if (args.length > 1) {
            try {
                vaultNum = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        if (plugin.getVaultXHook() != null && !plugin.getVaultXHook().hasVaultPermission(player, vaultNum)) {
            player.sendMessage("§c🔒 Vault #" + vaultNum + " is locked! Purchase or unlock it via VaultX.");
            plugin.playConfigSound(player, "sort", Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.0f);
            return true;
        }
        Inventory vaultInv = plugin.getVaultXHook() != null
                ? plugin.getVaultXHook().getPlayerEnderChestOrVault(player, vaultNum)
                : player.getEnderChest();
        boolean success = fr.skynex.storagepeek.api.StoragePeekProvider.get() != null
                && fr.skynex.storagepeek.api.StoragePeekProvider.get().openVirtualPeekSession(player, vaultInv,
                        "§6🏦 VaultX Vault #" + vaultNum);
        if (success) {
            player.sendMessage("§a[StoragePeek] Displaying 3D virtual preview for VaultX Vault #" + vaultNum + "!");
            plugin.playConfigSound(player, "sort", Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
