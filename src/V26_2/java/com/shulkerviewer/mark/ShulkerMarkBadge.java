package com.shulkerviewer.mark;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Corner badges for marked shulkers (1.3).
 *
 * <p>Drawn 8x8 in the slot's bottom-right corner. Purely client-side: reads
 * the local stack's custom_data, so it works on any server.
 */
public final class ShulkerMarkBadge {

    private static final int SIZE = 8;
    private static final int MARGIN = 1;

    private static final Map<Integer, ItemStack> ICON_CACHE = new HashMap<>();

    private ShulkerMarkBadge() {
    }

    private static ItemStack iconStack(int index) {
        ItemStack cached = ICON_CACHE.get(index);
        if (cached == null) {
            Item item = ShulkerMarkUtil.getIconItem(index);
            if (item == null) return ItemStack.EMPTY;
            cached = new ItemStack(item);
            ICON_CACHE.put(index, cached);
        }
        return cached;
    }

    public static void render(Minecraft client, AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                               int mouseX, int mouseY) {
        if (client == null || client.player == null || screen == null) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        int left = acc.shulkerViewer_getLeftPos();
        int top = acc.shulkerViewer_getTopPos();
        ItemStack hovered = ShulkerViewerClient.getHoveredItem();

        // A tooltip (vanilla or our preview) owns this frame: badges painted
        // now would float on top of it. Skip them entirely.
        if (ShulkerViewerClient.isPreviewVisible()) return;
        for (Slot s : screen.getMenu().slots) {
            if (s.isActive() && s.hasItem()
                && mouseX >= left + s.x - 1 && mouseX < left + s.x + 17
                && mouseY >= top + s.y - 1 && mouseY < top + s.y + 17) {
                return;
            }
        }

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            // Skip the hovered slot: its tooltip already shows the label,
            // and this pass runs after tooltips (would paint over them).
            if (!hovered.isEmpty() && stack == hovered) continue;
            if (!ShulkerMarkUtil.hasMark(stack)) continue;
            int mark = ShulkerMarkUtil.getMarkIndex(stack);
            if (mark == ShulkerMarkUtil.NO_ICON) continue;
            ItemStack icon = iconStack(mark);
            if (icon.isEmpty()) continue;

            int bx = left + slot.x + 16 - SIZE - MARGIN;
            int by = top + slot.y + 16 - SIZE - MARGIN;

            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5F, 0.5F);
            graphics.item(icon, bx * 2, by * 2);
            graphics.pose().popMatrix();
        }
    }
}
