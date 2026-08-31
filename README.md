# 📦 StoragePeek

<div align="center">

[![Plugin Version](https://img.shields.io/badge/Version-1.2.0-FFD700?style=for-the-badge&logo=minecraft)](https://www.spigotmc.org/resources/134712)
[![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Folia-005B9A?style=for-the-badge)](https://papermc.io/)
[![JitPack](https://img.shields.io/jitpack/v/github/Skytoone/StoragePeek?style=for-the-badge&color=2E7D32&logo=jitpack)](https://jitpack.io/#Skytoone/StoragePeek)
[![bStats](https://img.shields.io/bstats/servers/31024?style=for-the-badge&label=Servers&color=7B68EE)](https://bstats.org/plugin/bukkit/StoragePeek/31024)

**Revolutionary 3D Container Preview & Interactive Quick-Take System for Minecraft.**

*Preview chests, shulker boxes, ender chests, barrels & furniture in 3D without opening them.*

</div>

---

## 🌟 What is StoragePeek?

**StoragePeek** brings an immersive, futuristic 3D preview system to your Minecraft server. By simply crouching and looking at any chest, shulker box, barrel, furnace, ender chest, or custom furniture container, players can instantly view the container's contents suspended cleanly in 3D space using modern `Display` entities.

Built from the ground up for high performance, smooth interpolation, and broad plugin compatibility (Folia regionized threading, custom furniture hooks, claim protection integration, and a developer API).

---

## 🎮 Features & Capability Overview

### 🔍 3D Display & Preview System
| Feature | Description |
|---|---|
| **Modern Display Entities** | Jitter-free 3D rendering powered by Minecraft `ItemDisplay`, `BlockDisplay`, and `TextDisplay` |
| **Multi-Container Support** | Chests, Double Chests, Shulker Boxes, Barrels, End Chests, Furnaces, Dispensers, Droppers, Hoppers |
| **Custom Furniture & Items** | Seamless 3D previews for Oraxen, Nexo, ItemsAdder, and CraftEngine custom blocks & furniture |
| **Category Item Filters** | Filter displayed items on the fly by category: `ALL`, `RESOURCES`, `FOOD`, `EQUIPMENT` |
| **Quantity & Durability** | Integrated quantity stack badges and dynamic color-coded durability bars |
| **Fill Level Indicator** | Displays percentage fill indicator below preview displays |

### ⚡ Interactive Quick Actions & Animations
| Feature | Description |
|---|---|
| **Quick Take** | Instant item retrieval directly from the 3D preview without opening the GUI |
| **Quick Deposit / Swap** | Deposit items or swap hand items directly with container slots |
| **Smart Deposit** | Automatically deposit matching inventory items into the container |
| **Container Animations** | Smooth chest lid animations when looking at or interacting with containers |
| **Item Transfer Visualizer** | Particles and flying animation when items are transferred |

### 🎨 Cosmetic Themes & Focus Mode
| Feature | Description |
|---|---|
| **Cosmetic Themes** | Includes `DEFAULT`, `ENDER`, `RICH`, `AQUA`, `NETHER`, `NEON`, `CYBERPUNK`, `RAINBOW` themes |
| **Custom Theme API** | Register custom background materials, particle effects, and sounds via the API |
| **Player Preference** | Per-player `/storagepeek toggle` and `/storagepeek theme <name>` preferences |

---

## 🛡️ Protection & Plugin Integrations

StoragePeek automatically respects server permissions and claim protection plugins:

| Plugin | Feature |
|---|---|
| **WorldGuard** | Respects region container access & chest flags |
| **GriefPrevention** | Checks claim container permissions |
| **Lands** | Checks land container access flags |
| **Towny** | Respects town/nation container protection |
| **PlotSquared** | Respects plot container permissions |
| **Residence** | Checks container access flags |
| **LWC** | Checks protection locks |
| **BentoBox** | Checks island container access |
| **SuperiorSkyblock2**| Respects island container permissions |
| **GriefDefender** | Respects claim container access |
| **PlaceholderAPI** | Full PAPI expansion support for messages and UI |

---

## 💻 Developer API (`StoragePeek-API`)

StoragePeek provides a lightweight multi-module API (`StoragePeek-API`) available via **JitPack**.

### 1. Add Repository & Dependency

#### Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Skytoone.StoragePeek</groupId>
        <artifactId>StoragePeek-API</artifactId>
        <version>1.2.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle (`build.gradle`)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Skytoone.StoragePeek:StoragePeek-API:1.2.0'
}
```

---

### 2. API Code Examples

#### Accessing the API Provider
```java
import fr.skynex.storagepeek.api.StoragePeekAPI;
import fr.skynex.storagepeek.api.StoragePeekProvider;

StoragePeekAPI api = StoragePeekProvider.get();

// Check if player has an active 3D preview session
if (api.isSessionActive(player)) {
    Block containerBlock = api.getActiveSessionBlock(player);
}
```

#### Registering a Custom Container Provider
```java
api.registerContainerProvider(new CustomContainerProvider() {
    @Override
    public boolean isCustomContainer(Block block) {
        return MyPlugin.isCustomVaultBlock(block);
    }

    @Override
    public Inventory getContainerInventory(Block block, Player player) {
        return MyPlugin.getVaultInventory(block);
    }
});
```

#### Registering a Custom Cosmetic Theme
```java
api.registerCustomTheme(new CustomTheme(
    "GALAXY",
    Material.CRYSTAL_REALM_BLOCK,
    Particle.END_ROD,
    Sound.BLOCK_AMETHYST_BLOCK_CHIME,
    Color.fromRGB(128, 0, 128)
));
```

#### Loot Security & Item Masking API
```java
// Mask nether stars with a barrier placeholder unless the player has unlocked a key
api.registerLootSecurityFilter((player, block, entity, item) -> {
    if (item.getType() == Material.NETHER_STAR && !MyRpgPlugin.hasKey(player)) {
        return SecurityResult.maskWithPlaceholder(Material.BARRIER, "§c[Locked Vault - Key Required]");
    }
    return SecurityResult.allow();
});
```

#### Storage Search & Summary API
```java
// Find nearby container blocks within 15 blocks containing Diamonds
List<Block> diamondChests = api.findNearbyContainers(player.getLocation(), 15.0, Material.DIAMOND);

// Get total item count summary stored inside a container
Map<Material, Integer> summary = api.getContainerSummary(chestBlock);
```

#### 3D Holographic Container Tagline API
```java
// Set a custom 3D holographic title above a shop or guild vault container
api.setContainerTagline(chestBlock, "§e[ChestShop] §aSelling Diamonds for $50");
```

#### Dynamic Hover Slot Audio API
```java
// Play custom iron armor sound when hovering over armor items in 3D
api.registerSlotHoverSound(
    item -> item.getType().name().contains("HELMET") || item.getType().name().contains("CHESTPLATE"),
    Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.2f
);
```

#### 3D Custom Display Transformations API
```java
// Rotate Netherite Swords in 3D and make them glow purple
api.registerCustomTransform((player, item, slot) -> {
    if (item.getType() == Material.NETHERITE_SWORD) {
        return DisplayTransform.builder()
            .scale(1.3f)
            .rotationY(45f)
            .glowColor(Color.PURPLE)
            .build();
    }
    return null;
});
```

#### Container Economic Valuation API
```java
// Register item price evaluator and calculate total container value ($)
api.registerItemValuer(item -> MyShopPlugin.getItemPrice(item));
double totalValue = api.getContainerTotalValue(chestBlock, player);
```

#### 3D Beacon Beams & Halos API
```java
// Emit a legendary golden beam above a high-value chest
api.setContainerBeamColor(chestBlock, Color.fromRGB(255, 215, 0));
```

#### Virtual Backpack & Abstract Container API
```java
// Open a 3D StoragePeek preview session for a virtual backpack inventory
api.openVirtualPeekSession(player, virtualBackpackInventory, "§6Adventurer Backpack");
```

#### 3D Slot Hover Event
```java
@EventHandler
public void onSlotHover(StoragePeekSlotHoverEvent event) {
    if (event.getHoveredItem() != null && event.getHoveredItem().getType() == Material.DRAGON_EGG) {
        event.getPlayer().sendActionBar(Component.text("§d✨ You are gazing upon the Dragon Egg!"));
    }
}
```

#### Quick Deposit & Swap Events
```java
@EventHandler
public void onQuickDeposit(StoragePeekQuickDepositEvent event) {
    if (isQuestItem(event.getDepositedItem())) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cQuest items cannot be deposited in public vaults!");
    }
}
```

#### Listening to API Events
```java
@EventHandler
public void onRenderItem(StoragePeekRenderItemEvent event) {
    // Make legendary items glow golden in 3D space
    if (isLegendary(event.getItemStack())) {
        event.setGlowColor(Color.fromRGB(255, 215, 0));
        event.setCustomScaleMultiplier(1.2f);
    }
}
```

---

## 📜 Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/storagepeek reload` | `storagepeek.admin` | Reloads plugin configuration & messages |
| `/storagepeek toggle` | `storagepeek.toggle` | Toggles 3D container preview on/off |
| `/storagepeek themes` | `storagepeek.themes` | Opens the GUI theme selection menu |
| `/storagepeek theme <name>` | `storagepeek.theme.<name>` | Sets player's active visual theme |
| `/storagepeek filter <type>` | `storagepeek.filter` | Applies item filter (`ALL`, `RESOURCES`, `FOOD`, `EQUIPMENT`) |
| `/storagepeek purge` | `storagepeek.admin` | Purges orphaned display entities |

---

## 📄 License & Credits

- Developed & Maintained by **Skynex / Skytoone**
- Built for Spigot, Paper & Folia (Java 21)
