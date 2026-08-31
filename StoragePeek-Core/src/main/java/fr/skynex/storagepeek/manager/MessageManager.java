package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageManager {
    private final StoragePeek plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;

    public MessageManager(StoragePeek plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "messages.yml");
        }

        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = this.plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
            this.dataConfig.options().copyDefaults(true);
            try {
                this.dataConfig.save(this.configFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save messages.yml migration!");
            }
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null) {
            reloadConfig();
        }
        return this.dataConfig;
    }

    public void saveDefaultConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "messages.yml");
        }
        if (!this.configFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
    }

    public String getMessage(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "Message not found: messages." + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
