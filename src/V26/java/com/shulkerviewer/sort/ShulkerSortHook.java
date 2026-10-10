package com.shulkerviewer.sort;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.mixin.AbstractContainerScreenAccessor;
import com.shulkerviewer.util.ShulkerEditUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 12x12 sort-icon for 26.x (extraction rendering, no GuiGraphics).
 * Render is driven from ScreenEvents.beforeExtract, clicks via ScreenMouseEvents.
 */
public final class ShulkerSortHook {

    public static final int SIZE = 12;

    private ShulkerSortHook() {
    }

    public static boolean qualifies(AbstractContainerMenu menu) {
        if (menu == null || menu.containerId == 0) return false;
        if (menu.slots.isEmpty()) return false;
        Object first = menu.slots.get(0).container;
        if (first == null) return false;
        int n = 0;
        for (Slot s : menu.slots) {
            if (s.container != first) break;
            n++;
        }
        return n == 27;
    }

    public static int buttonX(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        return acc.shulkerViewer_getLeftPos() + acc.shulkerViewer_getImageWidth() - SIZE - 4;
    }

    public static int buttonY(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        return acc.shulkerViewer_getTopPos() + 3;
    }

    public static boolean isOverButton(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int bx = buttonX(screen);
        int by = buttonY(screen);
        return mouseX >= bx && mouseX < bx + SIZE && mouseY >= by && mouseY < by + SIZE;
    }

    public static void onClick(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        AbstractContainerMenu menu = screen.getMenu();
        if (mc == null || mc.player == null) return;
        if (!qualifies(menu)) return;
        if (!ShulkerEditUtil.isSingleplayer(mc)) {
            ShulkerViewerClient.showActionBar(
                Component.translatable("shulker-viewer.msg.sorting_singleplayer_only")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        SortRequester.request(mc, menu);
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!SortRequester.hasHandler()) return;
        if (!qualifies(screen.getMenu())) return;

        int x = buttonX(screen);
        int y = buttonY(screen);
        boolean hovered = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;

        // Later stratum draws on top of vanilla, like the preview renderer.
        g.nextStratum();

        // Vanilla-style 3D button face
        int face = hovered ? 0xFFC6C6C6 : 0xFF8B8B8B;
        g.fill(x, y, x + SIZE, y + SIZE, face);

        // Light edge (top + left)
        int light = hovered ? 0xFFFFFFFF : 0xFFA0A0A0;
        g.fill(x, y, x + SIZE, y + 1, light);           // top
        g.fill(x, y, x + 1, y + SIZE, light);           // left
        g.fill(x + 1, y + 1, x + 2, y + SIZE - 1, light); // inner top-left

        // Dark edge (bottom + right)
        int dark = hovered ? 0xFF555555 : 0xFF373737;
        g.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, dark); // bottom
        g.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, dark); // right
        g.fill(x + 1, y + SIZE - 2, x + SIZE - 1, y + SIZE - 1, dark); // inner bottom-right

        // Sort icon: two tiny arrows
        int col = 0xFF2D2D2D;

        // Up arrow
        g.fill(x + 5, y + 2, x + 7, y + 3, col);
        g.fill(x + 4, y + 3, x + 8, y + 4, col);

        // Down arrow
        g.fill(x + 4, y + 7, x + 8, y + 8, col);
        g.fill(x + 5, y + 8, x + 7, y + 9, col);

        if (hovered) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("shulker-viewer.button.sort.tooltip.line1").withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("shulker-viewer.button.sort.tooltip.line2").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            g.setTooltipForNextFrame(Minecraft.getInstance().font, tip, Optional.empty(), mouseX, mouseY);
        }
    }
}
