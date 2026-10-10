package com.shulkerviewer.mark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.shulkerviewer.mixin.InventoryAccessor;
import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local mark backup (1.3): dedicated servers never see our custom_data
 * unless a real interaction carries it, so a relog wipes client-side marks.
 * Instead of faking clicks (causes phantom copies when the server
 * disagrees), the client remembers every saved mark keyed by a fingerprint
 * of the shulker (type + vanilla name + sorted contents) and silently
 * reapplies it to matching unmarked shulkers — after relogs, moves between
 * inventory and chests, etc. Purely client-side, zero packets, nothing for
 * the server to reject.
 */
public final class ShulkerMarkStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FILE_TYPE = new TypeToken<LinkedHashMap<String, Entry>>() {
    }.getType();
    private static final int MAX_ENTRIES = 1024;

    private static final class Entry {
        int icon = ShulkerMarkUtil.NO_ICON;
        String label = "";
    }

    private static final Map<String, Entry> STORE = new LinkedHashMap<>();
    private static boolean loaded;
    private static int tickCounter;

    private ShulkerMarkStore() {
    }

    private static File file() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        File configDir = new File(mc.gameDirectory, "config");
        return new File(configDir, "shulker-viewer-marks.json");
    }

    /** Fingerprint: shulker type + vanilla name + sorted content multiset. */
    public static String fingerprint(ItemStack shulker) {
        if (shulker == null || shulker.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        Object key = BuiltInRegistries.ITEM.getKey(shulker.getItem());
        sb.append(key == null ? "?" : key.toString());
        if (shulker.get(DataComponents.CUSTOM_NAME) != null) {
            sb.append('|').append(shulker.getHoverName().getString());
        }
        List<String> parts = new ArrayList<>();
        for (ItemStack s : ShulkerItemUtil.getShulkerContents(shulker)) {
            if (s.isEmpty()) continue;
            Object k = BuiltInRegistries.ITEM.getKey(s.getItem());
            parts.add((k == null ? "?" : k.toString()) + '*' + s.getCount());
        }
        parts.sort(String::compareTo);
        for (String p : parts) sb.append('|').append(p);
        return sb.toString();
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (f == null || !f.isFile()) return;
            try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                LinkedHashMap<String, Entry> data = GSON.fromJson(r, FILE_TYPE);
                if (data != null) {
                    STORE.clear();
                    STORE.putAll(data);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static synchronized void save() {
        try {
            File f = file();
            if (f == null) return;
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            try (Writer w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(STORE, w);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Remembers the mark currently on the stack (call right after saving). */
    public static synchronized void remember(ItemStack shulker) {
        load();
        if (shulker == null || shulker.isEmpty() || !ShulkerMarkUtil.hasMark(shulker)) return;
        String fp = fingerprint(shulker);
        if (fp.isEmpty()) return;
        Entry e = new Entry();
        e.icon = ShulkerMarkUtil.getMarkIndex(shulker);
        e.label = ShulkerMarkUtil.getLabel(shulker);
        STORE.remove(fp);
        STORE.put(fp, e);
        while (STORE.size() > MAX_ENTRIES) {
            STORE.remove(STORE.keySet().iterator().next());
        }
        save();
    }

    /** Forgets the mark (call on clear). */
    public static synchronized void forget(ItemStack shulker) {
        load();
        String fp = fingerprint(shulker);
        if (fp.isEmpty()) return;
        if (STORE.remove(fp) != null) save();
    }

    /**
     * Every second, reapply known marks to matching unmarked shulkers in
     * the player inventory and the open container.
     */
    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null) return;
        if ((++tickCounter % 20) != 0) return;
        load();
        if (STORE.isEmpty()) return;
        try {
            Inventory inv = mc.player.getInventory();
            NonNullList<ItemStack> items = ((InventoryAccessor) inv).shulkerViewer_getItems();
            for (int i = 0; i < items.size(); i++) {
                reapply(items.get(i));
            }
            reapply(mc.player.getOffhandItem());
            if (mc.player.containerMenu != null) {
                for (Slot slot : mc.player.containerMenu.slots) {
                    if (slot.isActive() && slot.hasItem()) {
                        reapply(slot.getItem());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void reapply(ItemStack stack) {
        if (stack.isEmpty() || !ShulkerItemUtil.isShulkerBox(stack)) return;
        if (ShulkerMarkUtil.hasMark(stack)) return;
        Entry e;
        synchronized (ShulkerMarkStore.class) {
            e = STORE.get(fingerprint(stack));
        }
        if (e == null) return;
        ShulkerMarkUtil.setMark(stack, e.icon, e.label == null ? "" : e.label);
    }
}
