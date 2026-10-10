package com.shulkerviewer.mark;

import com.shulkerviewer.mixin.InventoryAccessor;
import com.shulkerviewer.util.ShulkerEditUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

/**
 * Shulker marks (1.3): a corner icon + a personal label per shulker.
 *
 * <p>Everything is stored inside the vanilla {@code minecraft:custom_data}
 * component, so marks survive vanilla/dedicated servers untouched: the server
 * treats custom_data as opaque NBT, syncs it back to every client, and keeps
 * it across relogs. No server-side mod is needed for storage; rendering and
 * editing are purely client-side.
 *
 * <p>Every marked shulker also gets a random UUID ({@code id}) written once.
 * Two visually identical shulkers therefore stay distinguishable: the exact
 * stack instance edited is the one that carries the mark.
 */
public final class ShulkerMarkUtil {

    public static final String TAG_ROOT = "shulker-viewer";
    private static final String KEY_MARK = "mark";
    private static final String KEY_LABEL = "label";
    private static final String KEY_ID = "id";

    public static final int NO_ICON = -1;
    public static final int MAX_LABEL_LENGTH = 32;

    /**
     * 54 muted-tone-friendly icons: tools, weapons, blocks, valuables.
     * Order is stable — the persisted value is the list index. New icons are
     * only ever APPENDED so marks saved with older versions stay valid.
     */
    public static final List<Item> ICONS = List.of(
        // --- tools & weapons ---
        Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.IRON_PICKAXE,
        Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.BOW,
        Items.CROSSBOW, Items.SHIELD, Items.DIAMOND_AXE,
        Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.FISHING_ROD,
        Items.TORCH, Items.COBBLESTONE, Items.OAK_PLANKS,
        Items.GLASS, Items.CHEST, Items.ENDER_CHEST,
        Items.FURNACE, Items.CRAFTING_TABLE, Items.ANVIL,
        Items.ENCHANTING_TABLE, Items.BEACON, Items.TNT,
        Items.ENDER_PEARL, Items.DIAMOND, Items.EMERALD,
        Items.GOLD_INGOT, Items.REDSTONE, Items.BOOKSHELF,
        // --- appended in 1.3 (order above frozen) ---
        Items.IRON_SWORD, Items.SHEARS, Items.MACE,
        Items.TRIDENT, Items.ELYTRA, Items.FLINT_AND_STEEL,
        Items.LANTERN, Items.HOPPER, Items.BREWING_STAND,
        Items.OBSERVER, Items.PISTON, Items.ENDER_EYE,
        Items.IRON_INGOT, Items.NETHERITE_INGOT, Items.LAPIS_LAZULI,
        Items.QUARTZ, Items.AMETHYST_SHARD, Items.GOLDEN_APPLE,
        Items.TOTEM_OF_UNDYING, Items.NETHER_STAR, Items.ECHO_SHARD,
        Items.HEART_OF_THE_SEA, Items.DRAGON_EGG, Items.EXPERIENCE_BOTTLE,
        Items.COMPASS, Items.CLOCK, Items.MAP,
        Items.SADDLE, Items.NAME_TAG, Items.LEAD
    );

    private ShulkerMarkUtil() {
    }

    private static CompoundTag root(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new CompoundTag();
        return data.copyTag().getCompoundOrEmpty(TAG_ROOT);
    }

    private static void writeRoot(ItemStack stack, CompoundTag root) {
        CompoundTag tag = new CompoundTag();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) tag = data.copyTag();
        tag.put(TAG_ROOT, root.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Icon index, or {@link #NO_ICON} if none. */
    public static int getMarkIndex(ItemStack stack) {
        CompoundTag r = root(stack);
        if (!r.contains(KEY_MARK)) return NO_ICON;
        int i = r.getIntOr(KEY_MARK, NO_ICON);
        return (i >= 0 && i < ICONS.size()) ? i : NO_ICON;
    }

    /** Personal label, or empty string. */
    public static String getLabel(ItemStack stack) {
        return root(stack).getStringOr(KEY_LABEL, "");
    }

    /** Stable identity of this exact shulker, or empty string if never marked. */
    public static String getId(ItemStack stack) {
        return root(stack).getStringOr(KEY_ID, "");
    }

    public static boolean hasMark(ItemStack stack) {
        return getMarkIndex(stack) != NO_ICON || !getLabel(stack).isEmpty();
    }

    public static Item getIconItem(int index) {
        return (index >= 0 && index < ICONS.size()) ? ICONS.get(index) : null;
    }

    /**
     Writes icon + label into the stack, generating a UUID identity on first
     mark. A {@link #NO_ICON} index with a blank label clears the mark.
     */
    public static void setMark(ItemStack stack, int iconIndex, String label) {
        if (stack == null || stack.isEmpty()) return;
        String clean = label == null ? "" : label.trim();
        if (clean.length() > MAX_LABEL_LENGTH) clean = clean.substring(0, MAX_LABEL_LENGTH);
        if (iconIndex == NO_ICON && clean.isEmpty()) {
            clearMark(stack);
            return;
        }
        CompoundTag r = root(stack);
        if (iconIndex == NO_ICON) {
            r.remove(KEY_MARK);
        } else {
            r.putInt(KEY_MARK, iconIndex);
        }
        if (clean.isEmpty()) {
            r.remove(KEY_LABEL);
        } else {
            r.putString(KEY_LABEL, clean);
        }
        if (r.getStringOr(KEY_ID, "").isEmpty()) {
            r.putString(KEY_ID, UUID.randomUUID().toString());
        }
        writeRoot(stack, r);
    }

    /** Removes the whole mark (icon, label and id). */
    public static void clearMark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        tag.remove(TAG_ROOT);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Applies the same mark to the server-side mirror of the edited client
     * stack (singleplayer only). The client edit is instant visually; without
     * this the integrated server would overwrite it on the next inventory
     * sync. On dedicated servers the mark syncs naturally the next time the
     * player moves/clicks the shulker (the click packet carries custom_data).
     *
     * <p>Matching is by exact stack reference for player inventory, else by
     * slot index of the currently open container menu.
     */
    public static void mirrorToServer(Minecraft mc, ItemStack clientRef, int iconIndex, String label) {
        if (mc == null || mc.player == null || clientRef == null || clientRef.isEmpty()) return;
        ServerPlayer sp = ShulkerEditUtil.serverPlayer(mc);
        if (sp == null) return;
        try {
            Inventory clientInv = mc.player.getInventory();
            Inventory serverInv = sp.getInventory();
            // Main inventory by reference (hotbar included).
            int n = ((InventoryAccessor) clientInv).shulkerViewer_getItems().size();
            for (int i = 0; i < n; i++) {
                if (((InventoryAccessor) clientInv).shulkerViewer_getItems().get(i) == clientRef) {
                    ItemStack serverStack = serverInv.getItem(i);
                    if (!serverStack.isEmpty()) setMark(serverStack, iconIndex, label);
                    serverInv.setChanged();
                    return;
                }
            }
            // Offhand by reference.
            if (mc.player.getOffhandItem() == clientRef) {
                ItemStack serverOff = sp.getOffhandItem();
                if (!serverOff.isEmpty()) setMark(serverOff, iconIndex, label);
                serverInv.setChanged();
                return;
            }
            // Open container (chest etc.): same slot index on the server menu.
            if (mc.player.containerMenu != null && sp.containerMenu != null
                && mc.player.containerMenu.containerId == sp.containerMenu.containerId) {
                List<Slot> clientSlots = mc.player.containerMenu.slots;
                for (int i = 0; i < clientSlots.size(); i++) {
                    if (clientSlots.get(i).getItem() == clientRef
                        && i < sp.containerMenu.slots.size()) {
                        ItemStack serverStack = sp.containerMenu.slots.get(i).getItem();
                        if (!serverStack.isEmpty()) setMark(serverStack, iconIndex, label);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
