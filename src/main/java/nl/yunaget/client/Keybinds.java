package nl.yunaget.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Properties;

public final class Keybinds {
    public enum Action {
        OPEN("Menu openen/sluiten"), AURA("Mine-aura aan/uit"), STOP("Alles direct stoppen"),
        GLOW("Item-glow aan/uit"), GLOW_FILTER("Glow: alles/selectie"),
        ITEMS("Tab items"), ENTITIES("Tab entities"), BINDS("Tab keybinds"),
        PREVIOUS("Vorige pagina"), NEXT("Volgende pagina"),
        SEARCH("Zoekveld"), AMOUNT("Itemselectie (oude toets)"), CLOSE("Menu sluiten"),
        DASHBOARD("Dashboard"), MINING("Mining-tab"), COLLECTOR("Collector-tab"), ESP("ESP-tab"), STORAGE("Storage-tab"),
        PAUSE("Pauze/hervatten"), COLLECT("Collector aan/uit"), ORE_ESP("Ore ESP aan/uit"),
        AUTO_TOOL("AutoTool aan/uit"), SORT("Nu sorteren"), MARK_CHEST("Chest selecteren/deselecteren"), CHEST_GLOW("Chest-glow aan/uit"), PANIC("NOODSTOP");
        public final String label;
        Action(String label) { this.label = label; }
    }
    private static final EnumMap<Action, Integer> KEYS = new EnumMap<>(Action.class);
    private static final EnumMap<Action, Boolean> DOWN = new EnumMap<>(Action.class);
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("yunaget.properties");
    public static Identifier item, entity;
    public static boolean itemGlow = true;
    public static boolean glowSelectedOnly = false;
    public static String error = "";

    public static void load() {
        KEYS.clear(); DOWN.clear();
        Properties config = new Properties();
        if (Files.exists(FILE)) {
            try (var in = Files.newInputStream(FILE)) { config.load(in); }
            catch (IOException ex) { failed(ex); }
        }
        for (Action action : Action.values()) {
            int key = action == Action.OPEN ? InputConstants.KEY_Y : action==Action.PANIC?InputConstants.KEY_F8:-1;
            try { key = Integer.parseInt(config.getProperty("key." + action.name(), "" + key)); }
            catch (NumberFormatException ignored) { }
            if (key < -1 || key >= 512 || key == InputConstants.KEY_ESCAPE) key = -1;
            if (key >= 0 && KEYS.containsValue(key)) key = -1;
            KEYS.put(action, key);
            DOWN.put(action, false);
        }
        item = Identifier.tryParse(config.getProperty("item", ""));
        entity = Identifier.tryParse(config.getProperty("entity", ""));
        itemGlow = Boolean.parseBoolean(config.getProperty("itemGlow", "true"));
        glowSelectedOnly = Boolean.parseBoolean(config.getProperty("glowSelectedOnly", "false"));
    }

    public static void save() {
        Properties config = new Properties();
        for (Action action : Action.values()) config.setProperty("key." + action.name(), "" + KEYS.get(action));
        if (item != null) config.setProperty("item", item.toString());
        if (entity != null) config.setProperty("entity", entity.toString());
        config.setProperty("itemGlow", "" + itemGlow);
        config.setProperty("glowSelectedOnly", "" + glowSelectedOnly);
        try {
            Files.createDirectories(FILE.getParent());
            try (var out = Files.newOutputStream(FILE)) { config.store(out, "YunaGet client settings"); }
            error = "";
        } catch (IOException ex) { failed(ex); }
    }

    private static void failed(IOException ex) {
        error = "Instellingen opslaan/laden mislukt.";
        LoggerFactory.getLogger("yunaget").warn(error, ex);
    }

    public static String name(Action action) {
        int key = KEYS.getOrDefault(action, -1);
        return key < 0 ? "Niet ingesteld" : InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
    }

    public static String bind(Action action, int key) {
        for (Action other : Action.values()) {
            if (key >= 0 && other != action && KEYS.get(other) == key) return "Al in gebruik: " + other.label;
        }
        KEYS.put(action, key);
        DOWN.put(action, true); // Do not execute the key used to assign the binding.
        save();
        return error.isEmpty() ? "Opgeslagen. Backspace wist; Esc annuleert." : error;
    }

    public static Action matching(int key) {
        for (Action action : Action.values()) if (KEYS.get(action) == key && key >= 0) return action;
        return null;
    }

    public static boolean consume(Action action) {
        return !DOWN.put(action, true);
    }

    public static void tick(Minecraft mc) {
        for (Action action : Action.values()) {
            int key = KEYS.getOrDefault(action, -1);
            boolean pressed = key >= 0 && InputConstants.isKeyDown(key);
            boolean wasDown = DOWN.getOrDefault(action, false);
            DOWN.put(action, pressed);
            if (pressed && !wasDown && mc.player != null && mc.isWindowActive() && (mc.gui.screen() == null
                || ((action == Action.STOP||action==Action.PANIC) && !(mc.gui.screen() instanceof YunaGetScreen)))) {
                run(action);
            }
        }
    }

    public static void run(Action action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (action == Action.GLOW || action == Action.GLOW_FILTER) {
            if (action == Action.GLOW) itemGlow = !itemGlow;
            else glowSelectedOnly = !glowSelectedOnly;
            save();
            tell("Item-glow " + (itemGlow ? "AAN" : "UIT") + "; "
                + (glowSelectedOnly ? "alleen aangevinkte items" : "alle gedropte items") + ".");
            return;
        }
        if (switch (action) { case OPEN, ITEMS, ENTITIES, BINDS, DASHBOARD, MINING, COLLECTOR, ESP, STORAGE -> true; default -> false; }) {
            if (action == Action.OPEN && mc.gui.screen() instanceof YunaGetScreen screen) screen.onClose();
            else {
                YunaGetScreen screen = new YunaGetScreen();
                mc.gui.setScreen(screen);
                screen.navigate(action);
            }
            return;
        }
        if (action == Action.STOP||action==Action.PANIC) { TaskManager.stop(); return; }
        if (action == Action.AURA && MiningTask.active()) { MiningTask.stop("Mine-aura UIT."); return; }
        if (action == Action.PAUSE) { if (TaskManager.paused) TaskManager.paused = false; else TaskManager.pause(); return; }
        if (action == Action.MARK_CHEST) { ChestManager.markLookedAt(); return; }
        if (action == Action.CHEST_GLOW) { YunaGetConfig.get.chestGlow = !YunaGetConfig.get.chestGlow; YunaGetConfig.save(); return; }
        if (action == Action.SORT) { AutoSorterManager.start(); return; }
        if (action == Action.COLLECT) { YunaGetConfig.get.collector = !YunaGetConfig.get.collector; TaskManager.paused = false; YunaGetConfig.save(); return; }
        if (action == Action.ORE_ESP) { YunaGetConfig.get.oreEsp = !YunaGetConfig.get.oreEsp; YunaGetConfig.save(); return; }
        if (action == Action.AUTO_TOOL) { YunaGetConfig.get.autoTool = !YunaGetConfig.get.autoTool; YunaGetConfig.save(); return; }
        if (action == Action.CLOSE && mc.gui.screen() instanceof YunaGetScreen screen) { screen.onClose(); return; }
        if (action == Action.AURA) {
            if (YunaGetConfig.get.selectedItems.isEmpty()) {
                tell("Kies eerst een item in de UI."); return;
            }
            MiningTask.start();
            mc.gui.setScreen(null);
        }
    }

    private static void tell(String message) {
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("[YunaGet] " + message));
    }
}
