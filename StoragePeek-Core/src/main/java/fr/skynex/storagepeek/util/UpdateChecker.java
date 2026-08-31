package fr.skynex.storagepeek.util;

import fr.skynex.storagepeek.StoragePeek;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final StoragePeek plugin;
    private final int resourceId;

    public UpdateChecker(StoragePeek plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Runnable task = () -> {
            try (InputStream inputStream = URI
                    .create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL()
                    .openStream();
                    Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not check for updates: " + exception.getMessage());
            }
        };

        if (FoliaScheduler.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(this.plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
        }
    }
}
