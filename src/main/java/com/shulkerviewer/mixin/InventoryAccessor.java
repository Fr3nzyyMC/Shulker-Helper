package com.shulkerviewer.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Inventory.items/selected were made private in 1.21.5 (and offhand was
 * removed). Accessor works regardless of visibility, on every version.
 */
@Mixin(Inventory.class)
public interface InventoryAccessor {

    @Accessor("items")
    NonNullList<ItemStack> shulkerViewer_getItems();

    @Accessor("selected")
    int shulkerViewer_getSelected();
}
