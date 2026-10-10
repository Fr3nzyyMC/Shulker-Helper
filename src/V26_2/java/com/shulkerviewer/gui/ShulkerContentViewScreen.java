package com.shulkerviewer.gui;

import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ShulkerContentViewScreen extends ContainerScreen {

    private final Screen parent;

    protected ShulkerContentViewScreen(ChestMenu menu, Inventory playerInventory, Component title, Screen parent) {
        super(menu, playerInventory, title);
        this.parent = parent;
    }

    public static ShulkerContentViewScreen create(ItemStack shulker, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> contents = ShulkerItemUtil.getShulkerContents(shulker);
        SimpleContainer container = new SimpleContainer(27);
        for (int i = 0; i < contents.size(); i++) {
            container.setItem(i, contents.get(i));
        }
        ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x3, 0, mc.player.getInventory(), container, 3);
        return new ShulkerContentViewScreen(menu, mc.player.getInventory(), shulker.getHoverName(), parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(event);
        }
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}