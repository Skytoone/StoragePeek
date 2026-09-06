package fr.skynex.storagepeek.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String getName();

    String getPermission();

    String getUsage();

    boolean isPlayerOnly();

    boolean execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}
