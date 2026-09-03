# 📦 StoragePeek

<div align="center">

[![Plugin Version](https://img.shields.io/badge/Version-v1.2.1-FFD700?style=for-the-badge&logo=minecraft)](https://www.spigotmc.org/resources/134712)
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
| **Multi-Container Support** | Chests, Double Chests, Shulker Boxes, Barrels, End Chests, Furnaces, Dispensers, Droppers, Hoppers, Crafters, Chiseled Bookshelves |
| **Custom Furniture & Items** | Seamless 3D previews for Oraxen, Nexo, ItemsAdder, and CraftEngine custom blocks & furniture |
| **Category Item Filters** | Filter displayed items on the fly by category: `ALL`, `RESOURCES`, `FOOD`, `EQUIPMENT` |
| **Live 3D Item Search** | Highlight matching items in 3D (`/sp search`) with golden glow while fading non-matching items |
| **3D Lock Status Badge** | Real-time `🔒 Locked` or `🔓 Unlocked` status badge based on claim & region protection permissions |
| **3D Page Navigation** | Smooth 27-slot page banner (`◀ Page 1 / 2 ▶`) for 54-slot double chests & virtual vaults (`/sp page`) |
| **3D Access Audit Log** | Floating 10-second audit hologram (`/sp history`) displaying recent container interaction logs |
| **Quantity & Durability** | Integrated quantity stack badges and dynamic color-coded durability bars |
| **Fill Level Indicator** | Displays percentage fill indicator below preview displays |
| **Auto-LOD & Adaptive Performance** | Dynamic TPS monitoring & Elytra speed raycast scaling for ultra-smooth 20 TPS performance |
| **3D Home Chest Markers** | Automatic `🏠 [HOME CHEST: Base]` 3D holographic badges attached to SethomeX home containers |

### ⚡ Interactive Quick Actions & Animations
| Feature | Description |
|---|---|
| **Quick Take** | Instant item retrieval directly from the 3D preview without opening the GUI |
| **Quick Deposit / Swap** | Deposit items or swap hand items directly with container slots |
| **Smart Deposit** | Automatically deposit matching inventory items into the container |
| **Configurable Click Mappings** | Map `TAKE` and `DEPOSIT` actions to custom left/right mouse clicks in `config.yml` |
| **3D Utility Visualizers** | Live 3D item & progress rendering on Crafting Tables, Furnaces, Brewing Stands, Anvils, Lecterns & Jukeboxes |
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
| **LootGlow** | Native rarity glow colors, beacon beams, mythic vault aura, item pop jump animation, rarity hover labels & magnet particles |
| **VaultX** | 3D virtual vault previews (`/sp vault <n>`), locked vault holograms, deposit to vault, and wealth integration |
| **SethomeX** | 3D Home Chest Markers (`🏠 [HOME CHEST: Base Alpha]`) attached to primary containers in player home locations |
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
| **Combat Tagging** | Automatic combat-culling hook (CombatLogX, CombatTagPlus, PvPManager, DeluxeCombat) |
| **PlaceholderAPI** | Full PAPI expansion support for messages and UI |
| **Paper 1.20.5+ / 1.21+** | Native `paper-plugin.yml` modern dependency graph loader |

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
        <version>v1.2.1</version>
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
    compileOnly 'com.github.Skytoone.StoragePeek:StoragePeek-API:v1.2.1'
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

#### 3D Pagination API
```java
// Set active 3D preview page for large inventories (54+ slots)
api.setSessionPage(player, 1);
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

#### Quick Take & Quick Deposit Events
```java
@EventHandler
public void onQuickTake(StoragePeekQuickTakeEvent event) {
    if (isProtectedVaultItem(event.getExtractedItem())) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou cannot quick-take vault items!");
    }
}

@EventHandler
public void onQuickDeposit(StoragePeekQuickDepositEvent event) {
    if (isQuestItem(event.getDepositedItem())) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cQuest items cannot be deposited in public vaults!");
    }
}
```

#### 3D Pagination Event
```java
@EventHandler
public void onPageChange(StoragePeekPageChangeEvent event) {
    event.getPlayer().sendMessage("§eSwitched to 3D page " + (event.getNewPage() + 1));
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

| Command | Permission | Parent / Default | Description |
|---|---|---|---|
| `/storagepeek reload` | `storagepeek.reload` | `storagepeek.admin` (op) | Reloads plugin configuration & messages |
| `/storagepeek purge` | `storagepeek.purge` | `storagepeek.admin` (op) | Purges orphaned display entities across all loaded chunks |
| `/storagepeek toggle` | `storagepeek.toggle` | `storagepeek.admin` (op) | Toggles 3D container preview on/off for player |
| `/storagepeek themes` | `storagepeek.themes` | `storagepeek.admin` (op) | Opens the GUI theme selection menu |
| `/storagepeek theme <name>` | `storagepeek.theme.<name>` | `storagepeek.theme.*` (op) | Sets player's active visual theme |
| `/storagepeek filter <type>` | `storagepeek.filter` | `storagepeek.admin` (op) | Applies item filter (`ALL`, `RESOURCES`, `FOOD`, `EQUIPMENT`, `rarity`) |
| `/storagepeek filter rarity <r>` | `storagepeek.filter` | `storagepeek.admin` (op) | Filters 3D HUD to only show LootGlow items of target rarity (`MYTHIC`, `LEGENDARY`, etc.) |
| `/storagepeek vault <n>` | `storagepeek.vault` | `storagepeek.vault` (op) | Previews VaultX virtual vault #n in 3D with Quick-Take/Deposit |
| `/storagepeek dashboard [r]` | `storagepeek.dashboard` | `storagepeek.dashboard` (op) | Opens 54-slot base storage GUI listing all containers, fill %, value ($), & GPS |
| `/storagepeek history` | `storagepeek.history` | `storagepeek.admin` (op) | Displays 3D container access audit hologram |
| `/storagepeek search <query>` | `storagepeek.search` | `storagepeek.search` (true) | Highlights matching items in 3D with golden glow |
| `/storagepeek page <n>` | `storagepeek.page` | `storagepeek.page` (true) | Switches 3D preview page for 54-slot containers |
| `/storagepeek find <item>` | `storagepeek.find` | `storagepeek.admin` (op) | Points 3D compass & GPS particle trail to nearest container holding item |
| `/storagepeek deposit [r|vault]` | `storagepeek.deposit` | `storagepeek.admin` (op) | Auto-deposits inventory items into nearby chests or VaultX virtual vaults |
| `/storagepeek label <text>` | `storagepeek.label` | `storagepeek.admin` (op) | Attaches persistent 3D holographic label above container |
| `/storagepeek createtheme <name>` | `storagepeek.createtheme` | `storagepeek.admin` (op) | Creates new 3D visual theme using held block |
| `/storagepeek stats [r]` | `storagepeek.stats` | `storagepeek.admin` (op) | Spawns floating 3D base storage statistics dashboard |
| *(Bypass Combat)* | `storagepeek.bypass.combat` | `storagepeek.admin` (op) | Bypasses PvP combat-culling restriction |
| *(Bypass Protection)* | `storagepeek.bypass.protection` | `storagepeek.admin` (op) | Bypasses region claim protection checks |

---

## 📊 PlaceholderAPI Support

| Placeholder | Description | Example Output |
|---|---|---|
| `%storagepeek_session_active%` | Indicates if player is currently peeking a container | `Yes` / `No` |
| `%storagepeek_disabled%` | Indicates if player has disabled 3D previews | `true` / `false` |
| `%storagepeek_active_theme%` | Returns player's currently selected theme | `DEFAULT`, `ENDER`, `RICH` |
| `%storagepeek_nearest_chest_distance%` | Distance to target container from `/sp find` | `14.2m` or `N/A` |
| `%storagepeek_session_block_type%` | Targeted container block material | `CHEST`, `BARREL`, `NONE` |
| `%storagepeek_session_page%` | Targeted container preview page index | `1`, `2` |
| `%storagepeek_session_item_count%` | Total items inside targeted container | `128` |
| `%storagepeek_session_total_value%` | Total economic value of targeted container | `1450.00` |
| `%storagepeek_session_tagline%` | Tagline / custom 3D label text of container | `[⛏️ Ore Vault]` |
| `%storagepeek_session_frozen%` | Indicates if preview orientation is locked | `true` / `false` |

---

## 📄 License & Credits

- Developed & Maintained by **Skynex / Skytoone**
- Built for Spigot, Paper & Folia (Java 21)
