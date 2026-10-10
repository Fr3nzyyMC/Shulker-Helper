package com.shulkerviewer.util;

import com.shulkerviewer.mixin.InventoryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Singleplayer-only helpers. A client-only mod cannot change server state on
 * a dedicated server, but in singleplayer the integrated server runs in the
 * same JVM, so shared code may resolve the server player for real menus.
 */
public final class ShulkerEditUtil {

    private ShulkerEditUtil() {
    }

    public static boolean isSingleplayer(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        if (!mc.hasSingleplayerServer()) return false;
        IntegratedServer server = mc.getSingleplayerServer();
        return server != null;
    }

    public static ServerPlayer serverPlayer(Minecraft mc) {
        if (!isSingleplayer(mc)) return null;
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return null;
        return server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    /**
     * Finds the main-inventory slot index (0..35, {@code Inventory.items})
     * holding this exact stack instance. Uses reference equality so two
     * identical shulkers are never confused.
     */
    public static int findPlayerMainSlot(Minecraft mc, ItemStack ref) {
        if (mc == null || mc.player == null || ref == null || ref.isEmpty()) return -1;
        NonNullList<ItemStack> items = ((InventoryAccessor) mc.player.getInventory()).shulkerViewer_getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == ref) return i;
        }
        return -1;
    }
}
