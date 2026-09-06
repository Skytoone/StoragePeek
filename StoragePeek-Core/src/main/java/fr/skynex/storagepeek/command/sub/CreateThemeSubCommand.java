package fr.skynex.storagepeek.command.sub;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.command.SubCommand;
import fr.skynex.storagepeek.manager.MessageManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CreateThemeSubCommand implements SubCommand {

    private final StoragePeek plugin;

    public CreateThemeSubCommand(StoragePeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "createtheme"; }

    @Override
    public String getPermission() { return "storagepeek.createtheme"; }

    @Override
    public String getUsage() { return "/sp createtheme <name>"; }

    @Override
    public boolean isPlayerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("storagepeek.createtheme") && !player.hasPermission("storagepeek.admin")) {
            player.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /sp createtheme <name>");
            return true;
        }
        String themeName = args[1].toLowerCase().trim();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        Material bgMat = (mainHand != null && mainHand.getType() != Material.AIR) ? mainHand.getType()
                : Material.BLACK_STAINED_GLASS;

        plugin.getConfig().set("themes.custom." + themeName + ".background-material", bgMat.name());
        plugin.getConfig().set("themes.custom." + themeName + ".particle-type", "END_ROD");
        plugin.getConfig().set("themes.custom." + themeName + ".glow-color", "255,215,0");
        plugin.saveConfig();
        plugin.loadConfigurationCache();

        player.sendMessage("§a[StoragePeek] Created custom 3D theme '§e" + themeName
                + "§a' using background block §f" + bgMat.name() + "§a!");
        plugin.playConfigSound(player, "sort", Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 1.2f);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
