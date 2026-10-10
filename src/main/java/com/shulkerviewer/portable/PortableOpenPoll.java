package com.shulkerviewer.portable;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.mixin.InventoryAccessor;
import com.shulkerviewer.util.ShulkerEditUtil;
import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

/**
 * RMB polling for portable open (singleplayer only).
 *
 * Two triggers (both require singleplayer):
 * 1. World: shulker in main/off hand + RMB + no block targeted (MISS).
 *    Vanilla places nothing when MISS, so hijacking it is safe.
 * 2. Inventory: hovering a shulker + empty cursor + RMB.
 *    Vanilla picks up half on RMB, we skip that for shulkers in SP.
 *
 * Uses isDown() + previous-state tracking instead of a single-frame toggle,
 * so the check works on ANY frame the key is held — no timing issues.
 */
public final class PortableOpenPoll {

    private static boolean wasRmbDown;

    private PortableOpenPoll() {
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.options == null) return;
        if (!PortableOpener.hasHandler()) return;
        if (!ShulkerEditUtil.isSingleplayer(mc)) return;

        boolean rmbDown = mc.options.keyUse.isDown();
        boolean rmbPressed = rmbDown && !wasRmbDown;
        wasRmbDown = rmbDown;

        if (!rmbPressed) return;

        Screen s = ShulkerViewerClient.currentScreen(mc);
        if (s == null) {
            pollWorld(mc);
        } else if (s instanceof AbstractContainerScreen<?>) {
            pollInventory(mc);
        }
    }

    private static void pollWorld(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean mainOk = ShulkerItemUtil.isShulkerBox(main);
        boolean offOk = ShulkerItemUtil.isShulkerBox(off);
        if (!mainOk && !offOk) return;
        HitResult hr = mc.hitResult;
        if (hr != null && hr.getType() != HitResult.Type.MISS) return;
        if (mainOk) {
            PortableOpener.open(mc, PortableOpener.SECTION_MAIN, ((InventoryAccessor) mc.player.getInventory()).shulkerViewer_getSelected());
        } else {
            PortableOpener.open(mc, PortableOpener.SECTION_OFFHAND, 0);
        }
    }

    private static void pollInventory(Minecraft mc) {
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;
        ItemStack hovered = ShulkerViewerClient.getHoveredItem();
        if (hovered.isEmpty() || !ShulkerItemUtil.isShulkerBox(hovered)) return;
        // Find by reference equality (hoveredItem IS the slot's stack instance)
        int slot = -1;
        for (int i = 0; i < ((InventoryAccessor) mc.player.getInventory()).shulkerViewer_getItems().size(); i++) {
            if (((InventoryAccessor) mc.player.getInventory()).shulkerViewer_getItems().get(i) == hovered) { slot = i; break; }
        }
        if (slot < 0) {
            ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.from_inventory_only"));
            return;
        }
        PortableOpener.open(mc, PortableOpener.SECTION_MAIN, slot);
    }
}
