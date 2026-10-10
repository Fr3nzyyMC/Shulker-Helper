package com.shulkerviewer.renderer;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShulkerViewerRenderer {

    private static final int PANEL_WIDTH = 176;
    private static final int GRID_HEIGHT = 72;
    private static final int BORDER_HEIGHT = 16;
    private static final int PANEL_HEIGHT = GRID_HEIGHT + BORDER_HEIGHT;
    private static final int HEADER_HEIGHT = 17;
    private static final int GRID_LEFT = 7;
    private static final int GRID_TOP = 17;
    private static final int SLOT_STEP = 18;
    private static final int ITEM_OFFSET = 1;
    private static final int SLOT_SIZE = 16;
    private static final int MAX_ITEMS_PER_ROW = 9;
    private static final int ROWS = 3;
    private static final Identifier CONTAINER_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public static void renderContainerTooltip(GuiGraphicsExtractor graphics, ItemStack container, int mouseX, int mouseY) {
        ShulkerItemUtil.ContainerPreview preview = ShulkerItemUtil.getPreviewContents(container);
        List<ItemStack> contents = preview.slots();
        int hiddenCount = preview.hiddenCount();

        Font font = Minecraft.getInstance().font;
        Component title = container.getHoverName();

        List<Component> hintLines = ShulkerViewerClient.getKeyHintLines();
        int footerHeight = 4 + hintLines.size() * (font.lineHeight + 1);
        if (hiddenCount > 0) {
            footerHeight += font.lineHeight + 1;
        }
        int totalHeight = PANEL_HEIGHT + footerHeight;

        Minecraft mc = Minecraft.getInstance();
        int x = mouseX + 12;
        int y = mouseY - totalHeight / 2;

        if (x + PANEL_WIDTH > mc.getWindow().getGuiScaledWidth()) {
            x = mouseX - PANEL_WIDTH - 12;
        }
        if (y < 0) {
            y = 0;
        }
        if (y + totalHeight > mc.getWindow().getGuiScaledHeight()) {
            y = mc.getWindow().getGuiScaledHeight() - totalHeight;
        }

        // Later stratum renders after everything submitted so far: the overlay
        // stays on top regardless of render-pipeline sort order.
        graphics.nextStratum();

        // Fully opaque rounded backdrop so background slots can't shine through
        int bx0 = x - 3;
        int by0 = y - 3;
        int bx1 = x + PANEL_WIDTH + 3;
        int by1 = y + totalHeight + 3;
        graphics.fill(bx0, by0 + 3, bx1, by1 - 3, 0xFF100010);
        graphics.fill(bx0 + 3, by0, bx1 - 3, by1, 0xFF100010);
        graphics.fill(bx0 + 1, by0 + 1, bx0 + 3, by0 + 3, 0xFF100010);
        graphics.fill(bx1 - 3, by0 + 1, bx1 - 1, by0 + 3, 0xFF100010);
        graphics.fill(bx0 + 1, by1 - 3, bx0 + 3, by1 - 1, 0xFF100010);
        graphics.fill(bx1 - 3, by1 - 3, bx1 - 1, by1 - 1, 0xFF100010);

        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            x + 2, y, 2f, 0f, PANEL_WIDTH - 4, 1, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            x + 1, y + 1, 1f, 1f, PANEL_WIDTH - 2, 1, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            x, y + 2, 0f, 2f, PANEL_WIDTH, GRID_HEIGHT - 2, 256, 256);

        for (int i = 1; i <= BORDER_HEIGHT; i++) {
            int destY = y + GRID_HEIGHT + (BORDER_HEIGHT - i);
            int cut = i == 1 ? 2 : (i == 2 ? 1 : 0);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
                x + cut, destY, (float) cut, (float) i, PANEL_WIDTH - cut * 2, 1, 256, 256);
        }

        graphics.text(font, title, x + (PANEL_WIDTH - font.width(title)) / 2, y + 5, 0xFF808080);

        int itemsStartY = y + GRID_TOP;
        ItemStack hoveredItem = null;
        int hoveredX = 0;
        int hoveredY = 0;

        for (int i = 0; i < 27; i++) {
            ItemStack item = contents.size() > i ? contents.get(i) : ItemStack.EMPTY;
            int col = i % MAX_ITEMS_PER_ROW;
            int row = i / MAX_ITEMS_PER_ROW;

            int slotX = x + GRID_LEFT + col * SLOT_STEP;
            int slotY = itemsStartY + row * SLOT_STEP;
            int itemX = slotX + ITEM_OFFSET;
            int itemY = slotY + ITEM_OFFSET;

            boolean isHovered = !item.isEmpty()
                && mouseX >= itemX && mouseX < itemX + SLOT_SIZE
                && mouseY >= itemY && mouseY < itemY + SLOT_SIZE;

            if (isHovered) {
                graphics.fill(itemX, itemY, itemX + SLOT_SIZE, itemY + SLOT_SIZE, 0x80FFFFFF);
            }
            if (!item.isEmpty()) {
                                graphics.item(item, itemX, itemY);
                graphics.itemDecorations(font, item, itemX, itemY);
            }

            if (isHovered) {
                hoveredItem = item;
                hoveredX = itemX;
                hoveredY = itemY;
            }
        }

        int hintY = y + PANEL_HEIGHT + 7;
        for (Component hint : hintLines) {
            graphics.text(font, hint, x + (PANEL_WIDTH - font.width(hint)) / 2, hintY, 0xFFFFFFFF);
            hintY += font.lineHeight + 1;
        }
        if (hiddenCount > 0) {
            Component more = Component.translatable("shulker-viewer.hint.more", hiddenCount);
            graphics.text(font, more, x + (PANEL_WIDTH - font.width(more)) / 2, hintY, 0xFFAAAAAA);
        }

        if (hoveredItem != null) {
            TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
            List<Component> tooltip = new ArrayList<>(
                hoveredItem.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, flag)
            );
            graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), hoveredX, hoveredY);
        }
    }
}