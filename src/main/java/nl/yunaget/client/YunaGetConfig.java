package nl.yunaget.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import nl.yunaget.client.core.StorageRule;
import nl.yunaget.client.core.ChestPreset;
import nl.yunaget.client.core.ItemSelection;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;

public final class YunaGetConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("yunaget-v2.json");
    public static YunaGetConfig get = new YunaGetConfig();
    public Set<String> selectedItems = new LinkedHashSet<>();
    private Map<String,Integer> targets; // Read-only migration from v0.2; omitted on the next save.
    public int searchRadius = 12, miningInterval = 1, minDurability = 10;
    public boolean onlyVisible = false, autoTool = true, restoreSlot = true;
    public boolean targetXray = true, navigationLine = true, statusHud = true;
    public boolean collector, selectedDropsOnly = true, safeNavigation = true;
    public int collectorRadius = 16;
    public boolean oreEsp, throughWalls, entityGlow;
    public int scanRadius = 16, renderDistance = 32;
    public Map<String, Integer> oreColors = defaultOres();
    public boolean sorter, sortWhenFull, walkToChest, skipHotbar = true, useFallback = true;
    public int inventoryThreshold = 80, chestDistance = 24, keepAmount = 0;
    public Set<String> favorites = new LinkedHashSet<>();
    public String worldProfile = "default";
    public boolean chestGlow = true, chestGlowThroughWalls = true;
    public Map<String,ChestPreset> chestPresets = new LinkedHashMap<>();
    public List<StorageRule> chests = new ArrayList<>();
    public Map<String, List<String>> categories = new LinkedHashMap<>();
    public static String error = "";

    private static Map<String, Integer> defaultOres() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] ores = {"diamond", "iron", "gold", "emerald", "redstone", "lapis", "copper", "coal"};
        int[] colors = {0xFF36E8ED, 0xFFEAC1A8, 0xFFFFD43B, 0xFF38E87B, 0xFFFF4040, 0xFF506EFF, 0xFFFF9950, 0xFFAAAAAA};
        for (int i = 0; i < ores.length; i++) {
            result.put("minecraft:" + ores[i] + "_ore", colors[i]);
            result.put("minecraft:deepslate_" + ores[i] + "_ore", colors[i]);
        }
        result.put("minecraft:ancient_debris", 0xFFB879FF);
        return result;
    }
    public static void load() {
        if (Files.exists(FILE)) try (var reader = Files.newBufferedReader(FILE)) {
            var loaded = JSON.fromJson(reader, YunaGetConfig.class);
            if (loaded != null) {
                if(loaded.targets!=null)loaded.onlyVisible=false;
                loaded.selectedItems=ItemSelection.migrate(loaded.selectedItems,loaded.targets);loaded.targets=null;
                loaded.validate(); get = loaded;
            }
        } catch (Exception ex) { fail("Configuratie kon niet worden geladen; huidige/default instellingen behouden", ex); }
        else if (Keybinds.item != null) get.selectedItems.add(Keybinds.item.toString());
    }
    private void validate() {
        searchRadius = Math.clamp(searchRadius, 1, 32); miningInterval = Math.clamp(miningInterval, 1, 20);
        scanRadius = Math.clamp(scanRadius, 1, 48); renderDistance = Math.clamp(renderDistance, 1, 96);
        collectorRadius = Math.clamp(collectorRadius, 1, 32); chestDistance = Math.clamp(chestDistance, 1, 48);
        minDurability = Math.clamp(minDurability, 0, 1000); keepAmount = Math.clamp(keepAmount, 0, 100000);
        inventoryThreshold = Math.clamp(inventoryThreshold, 1, 100);
        Objects.requireNonNull(selectedItems); Objects.requireNonNull(chests); Objects.requireNonNull(categories);
        Objects.requireNonNull(oreColors); Objects.requireNonNull(favorites);
        Objects.requireNonNull(chestPresets); Objects.requireNonNull(worldProfile);
        if (chestPresets.size() > 128) throw new IllegalArgumentException("Te veel presets");
        for (ChestPreset preset : chestPresets.values()) {
            Objects.requireNonNull(preset); Objects.requireNonNull(preset.filters);
            if (preset.filters.size() > 512) throw new IllegalArgumentException("Te veel presetfilters");
        }
        if (selectedItems.size() > 128 || chests.size() > 256 || oreColors.size() > 128 || categories.size() > 128) throw new IllegalArgumentException("Te veel configuratieregels");
        selectedItems.removeIf(id -> id==null||id.equals("minecraft:air")||net.minecraft.resources.Identifier.tryParse(id)==null);
        for (StorageRule r : chests) {
            Objects.requireNonNull(r); Objects.requireNonNull(r.scope); Objects.requireNonNull(r.dimension);
            Objects.requireNonNull(r.name); Objects.requireNonNull(r.filters);
            if (r.filters.size() > 512) throw new IllegalArgumentException("Te veel filters");
        }
    }
    public static void save() {
        try {
            get.validate(); Files.createDirectories(FILE.getParent());
            Path temp = FILE.resolveSibling("yunaget-v2.json.tmp");
            Files.writeString(temp, JSON.toJson(get));
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            error = "";
        } catch (Exception ex) { fail("Configuratie opslaan mislukt", ex); }
    }
    public static void fail(String message, Exception ex) {
        error = message; LoggerFactory.getLogger("yunaget").warn(message, ex);
    }
}
