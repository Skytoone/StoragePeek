package fr.skynex.storagepeek.session;

import fr.skynex.storagepeek.StoragePeek;
import fr.skynex.storagepeek.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeekSessionDisplayManager {

    public record ItemEntry(ItemDisplay display, double xOff, double yOff, int slot) {}

    private static final Map<Material, BlockData> blockDataCache = new ConcurrentHashMap<>();
    private static final Map<Material, Component> formattedMaterialNames = new ConcurrentHashMap<>();

    private final StoragePeek plugin;
    private final PeekSession session;
    private final DurabilityBarRenderer durabilityBarRenderer;
    private final SessionHeaderRenderer sessionHeaderRenderer;

    private final List<ItemEntry> itemEntries = new ArrayList<>();
    private final Map<Integer, ItemEntry> itemEntriesBySlot = new HashMap<>();
    private final Map<Integer, TextDisplay> fakeAmounts = new HashMap<>();
    private final Map<Integer, String> lastTextCache = new HashMap<>();
    private final List<Entity> entitiesToShow = new ArrayList<>();

    private BlockDisplay background;
    private TextDisplay anchor;
    private Interaction interactionEntity;
    private int hoveredSlot = -1;
    private TextDisplay hoverLabel;
    private BlockDisplay hoverHighlight;

    private boolean isSpawning = false;
    private boolean cleanedUp = false;

    public PeekSessionDisplayManager(StoragePeek plugin, PeekSession session) {
        this.plugin = plugin;
        this.session = session;
        this.durabilityBarRenderer = new DurabilityBarRenderer(plugin, session, blockDataCache);
        this.sessionHeaderRenderer = new SessionHeaderRenderer(plugin);
    }

    public void spawnDisplays(Location centerCache, Inventory inventory, int columns, double spacing, Material backgroundMaterial,
                              boolean animationsEnabled, boolean hoverNameplateEnabled, float hoverNameplateScale,
                              Color hoverNameplateBgColor, Material highlightMaterial, int teleportDuration,
                              float textScale, float textYOffset, float textZOffset, EquipmentSlot handSlot,
                              Block block, Entity entity, Player player, String searchQuery, int currentPage) {
        if (inventory == null) return;
        isSpawning = true;
        try {
            int size = inventory.getSize();
            int rows = (int) Math.ceil((double) size / columns);

            anchor = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setTeleportDuration(teleportDuration);
            });
            showEntityToPlayer(anchor, player);

            float bgWidth = (float) (columns * spacing) + 0.15f;
            float bgHeight = (float) (rows * spacing) + 0.15f;
            BlockData bgData = blockDataCache.computeIfAbsent(backgroundMaterial, Bukkit::createBlockData);

            background = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setBlock(bgData);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setVisibleByDefault(false);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setInterpolationDuration(1);
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getScale().set(animationsEnabled ? 0f : bgWidth, animationsEnabled ? 0f : bgHeight, 0.01f);
                t.getTranslation().set(-bgWidth / 2f, -bgHeight / 2f, -0.05f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(background);
            showEntityToPlayer(background, player);

            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < size; i++) {
                double xOff = (i % columns - (columns - 1) / 2.0) * spacing;
                double yOff = ((rows - 1) / 2.0 - i / columns) * spacing;

                ItemStack item = (i < contents.length) ? contents[i] : null;
                float scaleMultiplier = 1.0f;
                Color customGlowColor = null;

                if (item != null && item.getType() != Material.AIR) {
                    fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                        (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
                    
                    for (fr.skynex.storagepeek.api.security.LootSecurityFilter filter : apiImpl.getSecurityFilters()) {
                        fr.skynex.storagepeek.api.security.SecurityResult result = filter.evaluate(player, block, entity, item);
                        if (result.getType() == fr.skynex.storagepeek.api.security.SecurityResult.Type.HIDE) {
                            item = null;
                            break;
                        } else if (result.getType() == fr.skynex.storagepeek.api.security.SecurityResult.Type.MASK) {
                            Material mat = result.getPlaceholderMaterial() != null ? result.getPlaceholderMaterial() : Material.BARRIER;
                            item = new ItemStack(mat);
                            if (result.getCustomName() != null) {
                                ItemMeta meta = item.getItemMeta();
                                if (meta != null) {
                                    meta.displayName(LegacyComponentSerializer.legacySection().deserialize(result.getCustomName()));
                                    item.setItemMeta(meta);
                                }
                            }
                            break;
                        }
                    }

                    if (item != null) {
                        fr.skynex.storagepeek.api.events.StoragePeekRenderItemEvent renderEvent =
                            new fr.skynex.storagepeek.api.events.StoragePeekRenderItemEvent(player, item, i);
                        Bukkit.getPluginManager().callEvent(renderEvent);
                        if (renderEvent.isCancelled()) {
                            item = null;
                        } else {
                            scaleMultiplier = renderEvent.getCustomScaleMultiplier();
                            customGlowColor = renderEvent.getGlowColor();
                            if (customGlowColor == null && plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
                                customGlowColor = plugin.getLootGlowHook().getRarityColor(item);
                            }
                            if (searchQuery != null && !searchQuery.isEmpty()) {
                                String queryLower = searchQuery.toLowerCase().trim();
                                boolean matchesSearch = item.getType().name().toLowerCase().contains(queryLower) ||
                                    (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()).toLowerCase().contains(queryLower));
                                if (matchesSearch) {
                                    scaleMultiplier *= 1.3f;
                                    customGlowColor = Color.fromRGB(255, 215, 0);
                                } else {
                                    scaleMultiplier *= 0.25f;
                                }
                            }
                        }
                    }
                }

                final ItemStack finalItem = item;
                final float finalScaleMult = scaleMultiplier;
                final Color finalGlowColor = customGlowColor;

                ItemDisplay display = centerCache.getWorld().spawn(centerCache, ItemDisplay.class, ent -> {
                    plugin.tagDisplayEntity(ent);
                    ent.setItemStack(finalItem);
                    ent.setBillboard(Display.Billboard.CENTER);
                    ent.setVisibleByDefault(false);
                    ent.setBrightness(new Display.Brightness(15, 15));
                    ent.setInterpolationDelay(0);
                    ent.setInterpolationDuration(2);
                    ent.setTeleportDuration(1);
                    if (finalGlowColor != null) {
                        ent.setGlowing(true);
                        try {
                            ent.setGlowColorOverride(finalGlowColor);
                        } catch (Throwable ignored) {}
                    }
                    Transformation t = ent.getTransformation();
                    float baseScale = (animationsEnabled ? 0f : 0.15f) * finalScaleMult;
                    t.getScale().set(baseScale, baseScale, baseScale);
                    t.getTranslation().set((float) xOff, (float) yOff, 0.0f);
                    ent.setTransformation(t);
                    ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                });
                anchor.addPassenger(display);
                showEntityToPlayer(display, player);
                ItemEntry itemEntry = new ItemEntry(display, xOff, yOff, i);
                itemEntries.add(itemEntry);
                itemEntriesBySlot.put(i, itemEntry);

                if (item != null && item.getAmount() > 1 && plugin.isQuantityLabelsEnabled()) {
                    spawnFakeAmount(i, item.getAmount(), (float) xOff, (float) yOff, centerCache, player, textScale, textYOffset, textZOffset, animationsEnabled, inventory);
                }
                if (item != null && item.getType().getMaxDurability() > 0 && plugin.isDurabilityBarsEnabled()) {
                    durabilityBarRenderer.spawnDurabilityBar(anchor, i, item, (float) xOff, (float) yOff, centerCache, player, animationsEnabled, isSpawning, ent -> showEntityToPlayer(ent, player));
                }
            }

            hoverHighlight = centerCache.getWorld().spawn(centerCache, BlockDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setBlock(blockDataCache.computeIfAbsent(highlightMaterial, Bukkit::createBlockData));
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setVisibleByDefault(false);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setInterpolationDelay(0);
                ent.setInterpolationDuration(4);
                ent.setTeleportDuration(1);
                Transformation t = ent.getTransformation();
                t.getTranslation().set(-0.08f, -0.08f, -0.08f);
                t.getScale().set(0f, 0f, 0f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(hoverHighlight);
            showEntityToPlayer(hoverHighlight, player);

            hoverLabel = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
                plugin.tagDisplayEntity(ent);
                ent.setVisibleByDefault(false);
                ent.setBillboard(Display.Billboard.CENTER);
                ent.setInterpolationDelay(0);
                ent.setInterpolationDuration(4);
                ent.setTeleportDuration(1);
                ent.setBrightness(new Display.Brightness(15, 15));
                ent.setDefaultBackground(true);
                ent.setBackgroundColor(hoverNameplateBgColor);
                ent.setAlignment(TextDisplay.TextAlignment.CENTER);
                ent.text(Component.empty());
                Transformation t = ent.getTransformation();
                t.getTranslation().set(0f, 0f, 0.20f);
                t.getScale().set(0f, 0f, 0f);
                ent.setTransformation(t);
            });
            anchor.addPassenger(hoverLabel);
            showEntityToPlayer(hoverLabel, player);

            sessionHeaderRenderer.spawnHeaderBanners(anchor, centerCache, inventory, bgHeight, block, entity, player, size, currentPage, ent -> showEntityToPlayer(ent, player));
        } finally {
            isSpawning = false;
        }

        if (!entitiesToShow.isEmpty()) {
            FoliaScheduler.runTask(plugin, player, () -> {
                if (player.isOnline()) {
                    for (Entity ent : entitiesToShow) {
                        if (ent.isValid()) {
                            player.showEntity(plugin, ent);
                        }
                    }
                }
                entitiesToShow.clear();
            });
        }

        if (animationsEnabled) {
            animateScaleUp(player, inventory, columns, spacing, textScale);
        }
    }

    public void spawnFakeAmount(int slot, int amount, float localX, float localY, Location centerCache, Player player,
                                float textScale, float textYOffset, float textZOffset, boolean animationsEnabled, Inventory inventory) {
        String format = plugin.getMessageManager().getMessage("item-quantity-format");
        String text = format.replace("{amount}", String.valueOf(amount));
        lastTextCache.put(slot, text);

        ItemStack item = (inventory != null && slot < inventory.getSize()) ? inventory.getItem(slot) : null;
        Material filterMaterial = session.getFilterMaterial();
        boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);

        TextDisplay textDisplay = centerCache.getWorld().spawn(centerCache, TextDisplay.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setVisibleByDefault(false);
            ent.setBillboard(session.isFrozen() ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            ent.setInterpolationDuration(1);
            ent.setTeleportDuration(1);
            ent.setBrightness(new Display.Brightness(15, 15));
            ent.setDefaultBackground(false);
            ent.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            ent.setShadowed(true);
            ent.setAlignment(TextDisplay.TextAlignment.CENTER);
            ent.text(LegacyComponentSerializer.legacySection().deserialize(text));
            Transformation t = ent.getTransformation();
            t.getTranslation().set(localX, localY + textYOffset, textZOffset);
            float currentScale = (animationsEnabled || !matches) ? 0f : textScale;
            t.getScale().set(currentScale, currentScale, currentScale);
            ent.setTransformation(t);
        });
        anchor.addPassenger(textDisplay);
        showEntityToPlayer(textDisplay, player);
        fakeAmounts.put(slot, textDisplay);

        if (animationsEnabled && matches && !isSpawning) {
            FoliaScheduler.runLater(plugin, player, () -> {
                if (textDisplay.isValid() && anchor != null && anchor.isValid()) {
                    textDisplay.setInterpolationDelay(0);
                    textDisplay.setInterpolationDuration(5);
                    Transformation t = textDisplay.getTransformation();
                    t.getScale().set(textScale, textScale, textScale);
                    textDisplay.setTransformation(t);
                }
            }, 1L);
        }
    }

    public void updateFakeAmount(int slot, int amount, float localX, float localY, Location centerCache, Player player,
                                 float textScale, float textYOffset, float textZOffset, boolean animationsEnabled, Inventory inventory) {
        if (!plugin.isQuantityLabelsEnabled()) {
            destroyFakeEntity(slot);
            lastTextCache.put(slot, "");
            return;
        }
        String format = plugin.getMessageManager().getMessage("item-quantity-format");
        String newText = amount > 1 ? format.replace("{amount}", String.valueOf(amount)) : "";
        String lastText = lastTextCache.getOrDefault(slot, "");
        if (newText.equals(lastText)) return;

        if (amount <= 1) {
            if (fakeAmounts.containsKey(slot)) {
                destroyFakeEntity(slot);
                lastTextCache.put(slot, "");
            }
        } else {
            if (!fakeAmounts.containsKey(slot)) {
                spawnFakeAmount(slot, amount, localX, localY, centerCache, player, textScale, textYOffset, textZOffset, animationsEnabled, inventory);
            } else {
                TextDisplay display = fakeAmounts.get(slot);
                if (display != null && display.isValid()) {
                    display.text(LegacyComponentSerializer.legacySection().deserialize(newText));

                    ItemStack item = (inventory != null && slot < inventory.getSize()) ? inventory.getItem(slot) : null;
                    Material filterMaterial = session.getFilterMaterial();
                    boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);
                    float targetScale = matches ? textScale : 0f;
                    Transformation t = display.getTransformation();
                    t.getScale().set(targetScale, targetScale, targetScale);
                    display.setTransformation(t);
                }
                lastTextCache.put(slot, newText);
            }
        }
    }

    public void destroyFakeEntity(int slot) {
        TextDisplay display = fakeAmounts.remove(slot);
        if (display != null) {
            display.remove();
        }
    }

    public void spawnDurabilityBar(int slot, ItemStack item, float localX, float localY, Location centerCache, Player player, boolean animationsEnabled) {
        durabilityBarRenderer.spawnDurabilityBar(anchor, slot, item, localX, localY, centerCache, player, animationsEnabled, isSpawning, ent -> showEntityToPlayer(ent, player));
    }

    public void updateDurabilityBar(int slot, ItemStack item, float localX, float localY, Location centerCache, Player player, boolean animationsEnabled) {
        durabilityBarRenderer.updateDurabilityBar(anchor, slot, item, localX, localY, centerCache, player, animationsEnabled, isSpawning, ent -> showEntityToPlayer(ent, player));
    }

    public Material getDurabilityColor(double percent) {
        return durabilityBarRenderer.getDurabilityColor(percent);
    }

    public void destroyDurabilityBar(int slot) {
        durabilityBarRenderer.destroyDurabilityBar(slot);
    }

    public void applyHoverEffects(int newTarget, Player player, Block block, Entity entity, Inventory inventory,
                                  boolean hoverNameplateEnabled, float hoverNameplateScale) {
        Material filterMaterial = session.getFilterMaterial();

        if (hoveredSlot != -1) {
            ItemEntry entry = itemEntriesBySlot.get(hoveredSlot);
            if (entry != null && entry.display().isValid()) {
                ItemStack oldItem = entry.display().getItemStack();
                boolean matches = (filterMaterial == null) || (oldItem != null && oldItem.getType() == filterMaterial);
                float targetScale = matches ? 0.15f : 0.05f;
                Transformation t = entry.display().getTransformation();
                t.getScale().set(targetScale, targetScale, targetScale);
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                entry.display().setTransformation(t);
            }
        }

        if (newTarget != -1) {
            boolean hasItem = false;
            ItemStack hoveredItem = (inventory != null && newTarget < inventory.getSize()) ? inventory.getItem(newTarget) : null;
            if (hoveredItem != null && hoveredItem.getType() != Material.AIR) {
                hasItem = true;
            }

            if (newTarget != hoveredSlot) {
                fr.skynex.storagepeek.api.events.StoragePeekSlotHoverEvent hoverEvent =
                    new fr.skynex.storagepeek.api.events.StoragePeekSlotHoverEvent(player, block, entity, hoveredItem, hoveredSlot, newTarget);
                Bukkit.getPluginManager().callEvent(hoverEvent);
            }

            ItemEntry entry = itemEntriesBySlot.get(newTarget);
            if (entry != null && entry.display().isValid()) {
                boolean matches = (filterMaterial == null) || (hoveredItem != null && hoveredItem.getType() == filterMaterial);
                float targetScale = matches ? 0.22f : 0.05f;
                Transformation t = entry.display().getTransformation();
                t.getScale().set(targetScale, targetScale, targetScale);
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                entry.display().setTransformation(t);
                if (matches) {
                    plugin.playConfigSound(player, "hover", Sound.BLOCK_LEVER_CLICK, 0.2f, 1.5f);
                    if (hoveredItem != null) {
                        fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl apiImpl =
                            (fr.skynex.storagepeek.api.impl.StoragePeekAPIImpl) fr.skynex.storagepeek.api.StoragePeekProvider.get();
                        for (fr.skynex.storagepeek.api.audio.SlotHoverSound customSound : apiImpl.getSlotHoverSounds()) {
                            try {
                                if (customSound.getItemMatcher().test(hoveredItem)) {
                                    player.playSound(player.getLocation(), customSound.getSound(), customSound.getVolume(), customSound.getPitch());
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            double xOff = entry != null ? entry.xOff() : 0;
            double yOff = entry != null ? entry.yOff() : 0;

            if (hasItem && hoverNameplateEnabled && hoverLabel != null && hoverLabel.isValid()) {
                Component displayName = getItemDisplayName(hoveredItem);
                if (plugin.getLootGlowHook() != null && plugin.getLootGlowHook().isActive()) {
                    String plain = LegacyComponentSerializer.legacySection().serialize(displayName);
                    String formatted = plugin.getLootGlowHook().formatRarityHoverLabel(hoveredItem, plain);
                    displayName = LegacyComponentSerializer.legacySection().deserialize(formatted);
                }
                hoverLabel.text(displayName);

                Transformation t = hoverLabel.getTransformation();
                t.getTranslation().set((float) xOff, (float) (yOff + 0.12f), 0.10f);
                t.getScale().set(hoverNameplateScale, hoverNameplateScale, hoverNameplateScale);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            } else if (hoverLabel != null && hoverLabel.isValid()) {
                Transformation t = hoverLabel.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            }

            if (hasItem && hoverHighlight != null && hoverHighlight.isValid()) {
                Transformation t = hoverHighlight.getTransformation();
                t.getTranslation().set((float) xOff - 0.08f, (float) yOff - 0.08f, -0.04f);
                t.getScale().set(0.16f, 0.16f, 0.002f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            } else if (hoverHighlight != null && hoverHighlight.isValid()) {
                Transformation t = hoverHighlight.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            }
        } else {
            if (hoverLabel != null && hoverLabel.isValid()) {
                Transformation t = hoverLabel.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverLabel.setInterpolationDelay(0);
                hoverLabel.setInterpolationDuration(4);
                hoverLabel.setTransformation(t);
            }
            if (hoverHighlight != null && hoverHighlight.isValid()) {
                Transformation t = hoverHighlight.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                hoverHighlight.setInterpolationDelay(0);
                hoverHighlight.setInterpolationDuration(4);
                hoverHighlight.setTransformation(t);
            }
        }
        hoveredSlot = newTarget;
    }

    public void resetHoverEffects(Player player, Block block, Entity entity, Inventory inventory, boolean hoverNameplateEnabled, float hoverNameplateScale) {
        if (hoveredSlot != -1) {
            applyHoverEffects(-1, player, block, entity, inventory, hoverNameplateEnabled, hoverNameplateScale);
            hoveredSlot = -1;
        }
    }

    public Component getItemDisplayName(ItemStack item) {
        if (item == null) return Component.empty();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta enchantMeta) {
                Map<Enchantment, Integer> stored = enchantMeta.getStoredEnchants();
                if (!stored.isEmpty()) {
                    List<String> list = new ArrayList<>();
                    for (Map.Entry<Enchantment, Integer> e : stored.entrySet()) {
                        String name = e.getKey().getKey().getKey();
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase().replace('_', ' ');
                        list.add(name + " " + toRoman(e.getValue()));
                    }
                    return Component.text("Enchanted Book (" + String.join(", ", list) + ")", NamedTextColor.YELLOW);
                }
            }
            if ((item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK) && meta instanceof BookMeta bookMeta) {
                String title = bookMeta.getTitle();
                String author = bookMeta.getAuthor();
                if (title != null && !title.isEmpty()) {
                    String suffix = (author != null && !author.isEmpty()) ? " by " + author : "";
                    return Component.text(title + suffix, NamedTextColor.AQUA);
                }
            }
            Component customName = meta.displayName();
            if (customName != null) {
                return customName;
            }
        }
        return formattedMaterialNames.computeIfAbsent(item.getType(), mat -> {
            String raw = mat.name();
            String[] words = raw.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (word.isEmpty()) continue;
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                if (i < words.length - 1) sb.append(" ");
            }
            return Component.text(sb.toString(), NamedTextColor.WHITE);
        });
    }

    private String toRoman(int num) {
        if (num == 1) return "I";
        if (num == 2) return "II";
        if (num == 3) return "III";
        if (num == 4) return "IV";
        if (num == 5) return "V";
        return String.valueOf(num);
    }

    public void updateAllScales(float textScale) {
        Material filterMaterial = session.getFilterMaterial();
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                ItemStack item = entry.display().getItemStack();
                boolean matches = (filterMaterial == null) || (item != null && item.getType() == filterMaterial);
                float targetScale = matches ? (entry.slot() == hoveredSlot ? 0.22f : 0.15f) : 0.05f;

                Transformation t = entry.display().getTransformation();
                t.getScale().set(targetScale, targetScale, targetScale);
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                entry.display().setTransformation(t);

                TextDisplay amtText = fakeAmounts.get(entry.slot());
                if (amtText != null && amtText.isValid()) {
                    Transformation tText = amtText.getTransformation();
                    if (matches) {
                        tText.getScale().set(textScale, textScale, textScale);
                    } else {
                        tText.getScale().set(0f, 0f, 0f);
                    }
                    amtText.setInterpolationDelay(0);
                    amtText.setInterpolationDuration(4);
                    amtText.setTransformation(tText);
                }

                durabilityBarRenderer.updateScalesForEntry(entry, filterMaterial);
            }
        }
    }

    public void updateAllBillboards(Display.Billboard billboardMode) {
        if (anchor != null && anchor.isValid()) {
            anchor.setBillboard(billboardMode);
        }
        if (background != null && background.isValid()) {
            background.setBillboard(billboardMode);
        }
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                entry.display().setBillboard(billboardMode);
            }
        }
        if (hoverHighlight != null && hoverHighlight.isValid()) {
            hoverHighlight.setBillboard(billboardMode);
        }
        if (hoverLabel != null && hoverLabel.isValid()) {
            hoverLabel.setBillboard(billboardMode);
        }
        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null && display.isValid()) {
                display.setBillboard(billboardMode);
            }
        }
        durabilityBarRenderer.updateAllBillboards(billboardMode);
        sessionHeaderRenderer.setChildDisplayRotations(0f, 0f); // headers update if needed
    }

    public void setChildDisplayRotations(float yaw, float pitch) {
        if (background != null && background.isValid()) background.setRotation(yaw, pitch);
        if (hoverHighlight != null && hoverHighlight.isValid()) hoverHighlight.setRotation(yaw, pitch);
        if (hoverLabel != null && hoverLabel.isValid()) hoverLabel.setRotation(yaw, pitch);
        sessionHeaderRenderer.setChildDisplayRotations(yaw, pitch);
        for (ItemEntry entry : itemEntries)
            if (entry.display() != null && entry.display().isValid()) entry.display().setRotation(yaw, pitch);
        for (TextDisplay d : fakeAmounts.values())
            if (d != null && d.isValid()) d.setRotation(yaw, pitch);
        durabilityBarRenderer.setChildDisplayRotations(yaw, pitch);
    }

    public void spawnInteractionEntity(Location centerCache, int columns, double spacing, Inventory inventory, Player player) {
        if (inventory == null) return;
        int rows = (int) Math.ceil((double) inventory.getSize() / columns);
        float bgWidth = (float) (columns * spacing);
        float bgHeight = (float) (rows * spacing);

        interactionEntity = centerCache.getWorld().spawn(centerCache, Interaction.class, ent -> {
            plugin.tagDisplayEntity(ent);
            ent.setInteractionWidth(bgWidth);
            ent.setInteractionHeight(bgHeight);
            ent.setVisibleByDefault(false);
        });
        showEntityToPlayer(interactionEntity, player);
    }

    public void animateScaleUp(Player player, Inventory inventory, int columns, double spacing, float textScale) {
        FoliaScheduler.runLater(plugin, player, () -> {
            if (cleanedUp || anchor == null || !anchor.isValid()) return;

            Location soundLoc = session.getContainerCenter();
            if (soundLoc != null && soundLoc.getWorld() != null) {
                Block block = session.getBlock();
                Sound sound = switch (block != null ? block.getType() : Material.AIR) {
                    case ENDER_CHEST -> Sound.BLOCK_ENDER_CHEST_OPEN;
                    case BARREL -> Sound.BLOCK_BARREL_OPEN;
                    case FURNACE, BLAST_FURNACE, SMOKER -> Sound.BLOCK_FURNACE_FIRE_CRACKLE;
                    case BREWING_STAND -> Sound.BLOCK_BREWING_STAND_BREW;
                    case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> Sound.BLOCK_ANVIL_USE;
                    default -> Sound.BLOCK_CHEST_OPEN;
                };
                soundLoc.getWorld().playSound(soundLoc, sound, 0.4f, 1.1f);
            }

            if (background != null && background.isValid() && inventory != null) {
                int rows = (int) Math.ceil((double) inventory.getSize() / columns);
                float bgWidth = (float) (columns * spacing) + 0.15f;
                float bgHeight = (float) (rows * spacing) + 0.15f;

                background.setInterpolationDelay(0);
                background.setInterpolationDuration(5);
                Transformation t = background.getTransformation();
                t.getScale().set(bgWidth, bgHeight, 0.01f);
                background.setTransformation(t);
            }

            for (ItemEntry entry : itemEntries) {
                if (entry.display() != null && entry.display().isValid()) {
                    entry.display().setInterpolationDelay(0);
                    entry.display().setInterpolationDuration(5);
                    Transformation t = entry.display().getTransformation();
                    t.getScale().set(0.15f, 0.15f, 0.15f);
                    entry.display().setTransformation(t);
                }
            }

            for (Map.Entry<Integer, TextDisplay> entry : fakeAmounts.entrySet()) {
                TextDisplay display = entry.getValue();
                if (display != null && display.isValid()) {
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(5);
                    Transformation t = display.getTransformation();
                    t.getScale().set(textScale, textScale, textScale);
                    display.setTransformation(t);
                }
            }

            durabilityBarRenderer.animateScaleUp(inventory);
        }, 1L);
    }

    public void animateScaleDown() {
        if (background != null && background.isValid()) {
            background.setInterpolationDelay(0);
            background.setInterpolationDuration(4);
            Transformation t = background.getTransformation();
            t.getScale().set(0f, 0f, 0.01f);
            background.setTransformation(t);
        }
        if (hoverLabel != null && hoverLabel.isValid()) {
            hoverLabel.setInterpolationDelay(0);
            hoverLabel.setInterpolationDuration(4);
            Transformation t = hoverLabel.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            hoverLabel.setTransformation(t);
        }
        if (hoverHighlight != null && hoverHighlight.isValid()) {
            hoverHighlight.setInterpolationDelay(0);
            hoverHighlight.setInterpolationDuration(4);
            Transformation t = hoverHighlight.getTransformation();
            t.getScale().set(0f, 0f, 0f);
            hoverHighlight.setTransformation(t);
        }
        sessionHeaderRenderer.animateScaleDown();
        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null && entry.display().isValid()) {
                entry.display().setInterpolationDelay(0);
                entry.display().setInterpolationDuration(4);
                Transformation t = entry.display().getTransformation();
                t.getScale().set(0f, 0f, 0f);
                entry.display().setTransformation(t);
            }
        }
        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null && display.isValid()) {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(4);
                Transformation t = display.getTransformation();
                t.getScale().set(0f, 0f, 0f);
                display.setTransformation(t);
            }
        }
        durabilityBarRenderer.animateScaleDown();
    }

    public void removeEntities() {
        if (cleanedUp) return;
        cleanedUp = true;

        Location soundLoc = session.getContainerCenter();
        if (soundLoc != null && soundLoc.getWorld() != null) {
            Block block = session.getBlock();
            Sound sound = switch (block != null ? block.getType() : Material.AIR) {
                case ENDER_CHEST -> Sound.BLOCK_ENDER_CHEST_CLOSE;
                case BARREL -> Sound.BLOCK_BARREL_CLOSE;
                default -> Sound.BLOCK_CHEST_CLOSE;
            };
            soundLoc.getWorld().playSound(soundLoc, sound, 0.3f, 1.0f);
        }

        if (background != null) { background.remove(); background = null; }
        if (anchor != null) { anchor.remove(); anchor = null; }
        if (interactionEntity != null) { interactionEntity.remove(); interactionEntity = null; }
        if (hoverLabel != null) { hoverLabel.remove(); hoverLabel = null; }
        if (hoverHighlight != null) { hoverHighlight.remove(); hoverHighlight = null; }
        sessionHeaderRenderer.removeAll();

        for (ItemEntry entry : itemEntries) {
            if (entry.display() != null) entry.display().remove();
        }
        itemEntries.clear();
        itemEntriesBySlot.clear();

        for (TextDisplay display : fakeAmounts.values()) {
            if (display != null) display.remove();
        }
        fakeAmounts.clear();

        durabilityBarRenderer.removeAll();
    }

    private void showEntityToPlayer(Entity entity, Player player) {
        if (entity != null) {
            if (isSpawning) {
                entitiesToShow.add(entity);
            } else {
                FoliaScheduler.runTask(plugin, player, () -> {
                    if (entity.isValid() && player.isOnline()) {
                        player.showEntity(plugin, entity);
                    }
                });
            }
        }
    }

    public TextDisplay getAnchor() { return anchor; }
    public Interaction getInteractionEntity() { return interactionEntity; }
    public void setInteractionEntity(Interaction interactionEntity) { this.interactionEntity = interactionEntity; }
    public List<ItemEntry> getItemEntries() { return itemEntries; }
    public int getHoveredSlot() { return hoveredSlot; }
    public void setHoveredSlot(int hoveredSlot) { this.hoveredSlot = hoveredSlot; }
    public TextDisplay getFillIndicator() { return sessionHeaderRenderer.getFillIndicator(); }
    public Map<Integer, BlockDisplay> getDurabilityBars() { return durabilityBarRenderer.getDurabilityBars(); }
    public Map<Integer, BlockDisplay> getDurabilityBgs() { return durabilityBarRenderer.getDurabilityBgs(); }
    public boolean isCleanedUp() { return cleanedUp; }
    public static void clearBlockDataCache() { 
        blockDataCache.clear(); 
        formattedMaterialNames.clear();
    }
}
