package com.shulkerviewer.gui;

import com.shulkerviewer.mark.ShulkerMarkUtil;
import com.shulkerviewer.mark.ShulkerMarkStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Mark editor (1.3): pick an icon + type a personal label.
 * Stored into the hovered stack's vanilla custom_data, so it survives
 * servers; in singleplayer the server-side mirror is updated too, and the
 * local mark store reapplies it after relogs.
 *
 * Layout is compact and adapts to the window: more columns on wide screens
 * so it fits even at high GUI scales / small windows.
 */
public class ShulkerMarkScreen extends Screen {

    private static final int CELL = 22;
    private static final int PITCH = 24;

    private final ItemStack ref;
    private final Screen parent;
    private final List<ItemStack> iconStacks = new ArrayList<>();
    /** When the editor was opened: char events right after that belong to the
     * opening keystroke (e.g. "m"), not to the label field. */
    private final long openedAt = System.currentTimeMillis();

    private EditBox labelField;
    private int selectedIcon = ShulkerMarkUtil.NO_ICON;
    private int cols;
    private int gridX;
    private int gridY;
    private final List<Button> iconButtons = new ArrayList<>();

    public ShulkerMarkScreen(ItemStack ref, Screen parent) {
        super(Component.translatable("shulker-viewer.mark.title"));
        this.ref = ref;
        this.parent = parent;
        for (Item item : ShulkerMarkUtil.ICONS) {
            iconStacks.add(new ItemStack(item));
        }
    }

    @Override
    protected void init() {
        selectedIcon = ShulkerMarkUtil.getMarkIndex(ref);
        int cx = this.width / 2;

        int fieldW = Math.min(240, this.width - 30);
        labelField = new EditBox(this.font, cx - fieldW / 2, 26, fieldW, 18,
            Component.translatable("shulker-viewer.mark.label_hint"));
        labelField.setMaxLength(ShulkerMarkUtil.MAX_LABEL_LENGTH);
        labelField.setValue(ShulkerMarkUtil.getLabel(ref));
        labelField.setHint(Component.translatable("shulker-viewer.mark.label_hint"));
        labelField.setFocused(true);
        addRenderableWidget(labelField);

        // Adaptive grid: as many columns as fit, so small windows stay usable.
        cols = Math.max(6, Math.min(12, (this.width - 30) / PITCH));
        gridX = cx - (cols * PITCH - 2) / 2;
        gridY = 60;
        // Grow columns while the button row would overflow the bottom.
        while (cols < 12 && buttonsY() + 20 > this.height - 6) {
            cols++;
            gridX = cx - (cols * PITCH - 2) / 2;
        }

        int btnY = buttonsY();
        int totalBtnW = Math.min(4 * 68 + 3 * 4, this.width - 20);
        int bw = (totalBtnW - 3 * 4) / 4;
        int bx = cx - totalBtnW / 2;
        addRenderableWidget(Button.builder(
                Component.translatable("shulker-viewer.mark.no_icon"),
                b -> selectIcon(ShulkerMarkUtil.NO_ICON))
            .bounds(bx, btnY, bw, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("shulker-viewer.mark.save"),
                b -> save())
            .bounds(bx + (bw + 4), btnY, bw, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("shulker-viewer.mark.clear"),
                b -> clear())
            .bounds(bx + (bw + 4) * 2, btnY, bw, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("shulker-viewer.mark.cancel"),
                b -> onClose())
            .bounds(bx + (bw + 4) * 3, btnY, bw, 20).build());

        // Plain vanilla buttons for the icons (face, hover, click sound);
        // items + selection are painted over them in extractRenderState().
        iconButtons.clear();
        for (int i = 0; i < iconStacks.size(); i++) {
            int x = gridX + (i % cols) * PITCH;
            int y = gridY + (i / cols) * PITCH;
            final int index = i;
            Button btn = Button.builder(Component.empty(), b -> selectIcon(index))
                .bounds(x, y, CELL, CELL).build();
            iconButtons.add(btn);
            addRenderableWidget(btn);
        }
    }

    private int rows() {
        return (iconStacks.size() + cols - 1) / cols;
    }

    private int buttonsY() {
        return gridY + rows() * PITCH - 2 + 8;
    }

    private void selectIcon(int index) {
        selectedIcon = index;
    }

    private void save() {
        if (ref == null || ref.isEmpty()) {
            onClose();
            return;
        }
        String label = labelField == null ? "" : labelField.getValue();
        if (selectedIcon == ShulkerMarkUtil.NO_ICON && label.trim().isEmpty()) {
            clear();
            return;
        }
        ShulkerMarkUtil.setMark(ref, selectedIcon, label);
        ShulkerMarkUtil.mirrorToServer(Minecraft.getInstance(), ref, selectedIcon, label);
        ShulkerMarkStore.remember(ref);
        showMsg(Component.translatable("shulker-viewer.msg.marked"));
        onClose();
    }

    private void clear() {
        if (ref != null && !ref.isEmpty()) {
            ShulkerMarkStore.forget(ref);
            ShulkerMarkUtil.clearMark(ref);
            ShulkerMarkUtil.mirrorToServer(Minecraft.getInstance(), ref,
                ShulkerMarkUtil.NO_ICON, "");
        }
        showMsg(Component.translatable("shulker-viewer.msg.mark_cleared"));
        onClose();
    }

    private static void showMsg(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendOverlayMessage(msg);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, this.title,
            (this.width - this.font.width(this.title)) / 2, 10, 0xFFFFFFFF);

        Item selItem = ShulkerMarkUtil.getIconItem(selectedIcon);
        Component iconLine = selItem == null
            ? Component.translatable("shulker-viewer.mark.no_icon").withStyle(ChatFormatting.GRAY)
            : Component.translatable("shulker-viewer.mark.current_icon",
                new ItemStack(selItem).getHoverName()).withStyle(ChatFormatting.GRAY);
        graphics.text(this.font, iconLine,
            (this.width - this.font.width(iconLine)) / 2, 48, 0xFFFFFFFF);

        for (int i = 0; i < iconButtons.size(); i++) {
            Button btn = iconButtons.get(i);
            // Item centered on its vanilla button (drawn by super).
            graphics.item(iconStacks.get(i), btn.getX() + 3, btn.getY() + 3);
            if (i == selectedIcon) {
                // Strictly 1px white frame right on the button edge: coincides
                // with the vanilla hover border instead of doubling it.
                int x = btn.getX();
                int y = btn.getY();
                int c = 0xFFFFFFFF;
                graphics.fill(x, y, x + CELL, y + 1, c);
                graphics.fill(x, y + CELL - 1, x + CELL, y + CELL, c);
                graphics.fill(x, y, x + 1, y + CELL, c);
                graphics.fill(x + CELL - 1, y, x + CELL, y + CELL, c);
            }
            if (btn.isHovered()) {
                List<Component> tip = List.of(iconStacks.get(i).getHoverName());
                graphics.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (labelField != null && labelField.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // Swallow the opening keystroke's char (e.g. "m") if it arrives late.
        if (System.currentTimeMillis() - openedAt < 300) return true;
        if (labelField != null && labelField.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
