package com.shulkerviewer.portable;

import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** MenuProvider serving a {@link PortableShulkerMenu} prefilled from a shulker item. */
public class PortableShulkerProvider implements MenuProvider {

    private final SimpleContainer box;
    private final Component title;
    private final int section;
    private final int index;

    public PortableShulkerProvider(ItemStack shulker, int section, int index) {
        List<ItemStack> contents = ShulkerItemUtil.getShulkerContents(shulker);
        this.box = new SimpleContainer(27);
        for (int i = 0; i < contents.size(); i++) {
            box.setItem(i, contents.get(i));
        }
        this.title = shulker.getHoverName();
        this.section = section;
        this.index = index;
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        PortableShulkerMenu menu = new PortableShulkerMenu(containerId, playerInventory, box, section, index);
        if (player instanceof ServerPlayer sp) {
            menu.setOwner(sp);
        }
        return menu;
    }
}
