package com.shulkerviewer.mark;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Corner badges for marked shulkers (1.3).
 *
 * <p>Drawn 8x8 in the slot's bottom-right corner, muted (dark overlay + gray
 * frame) so it never screams. Purely client-side: reads the local stack's
 * custom_data, so it works on any server.
 */
public final class ShulkerMarkBadge {

    private static final int SIZE = 8;
    private static final int MARGIN = 1;

    private static final Map<Integer, ItemStack> ICON_CACHE = new HashMap<>();
    private static boolean spriteFailedLogged;

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

    public static void render(Minecraft client, AbstractContainerScreen<?> screen, GuiGraphics graphics,
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

            // Soft drop shadow first (the icon's own silhouette, shifted
            // down-right so minor shape differences stay hidden), then the
            // full 3D icon on top.
            drawSpriteShadow(client, icon, bx + 1, by + 1, graphics);
            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5F, 0.5F);
            graphics.renderItem(icon, bx * 2, by * 2);
            graphics.pose().popMatrix();
        }
    }

    private static void drawSpriteShadow(Minecraft mc, ItemStack iconSource, int sx, int sy, GuiGraphics graphics) {
        try {
            if (mc == null || mc.player == null) return;
            // No shadow for blocks: their square silhouette never hugs the
            // rendered 3D cube, it just looks like a stray box.
            if (iconSource.getItem() instanceof net.minecraft.world.item.BlockItem) return;
            ItemStackRenderState state = new ItemStackRenderState();
            mc.getItemModelResolver().updateForNonLiving(
                state, iconSource, ItemDisplayContext.GUI, mc.player);
            if (state.isEmpty()) return;
            TextureAtlasSprite sprite = state.pickParticleIcon(RandomSource.create());
            if (sprite == null) return;
            // Tight drop shadow: same silhouette, 1px smaller and shifted
            // +1/+1, so exactly one pixel peeks out at the bottom-right.
            // Square block faces hide under the rendered cube this way.
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, sx, sy, SIZE - 1, SIZE - 1, 0x80000000);
        } catch (Throwable t) {
            if (!spriteFailedLogged) {
                spriteFailedLogged = true;
                ShulkerViewerClient.LOGGER.error("ShulkerViewer sprite shadow failed", t);
            }
        }
    }
}
