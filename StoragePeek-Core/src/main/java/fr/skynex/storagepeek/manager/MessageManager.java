package fr.skynex.storagepeek.manager;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
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
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
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
        return translateHexColorCodes(msg);
    }

    private String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(builder, replacement.toString());
        }
        matcher.appendTail(builder);
        return builder.toString().replace('&', '§');
    }
}
