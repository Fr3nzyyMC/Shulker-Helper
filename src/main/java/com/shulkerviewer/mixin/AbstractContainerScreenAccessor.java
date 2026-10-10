package com.shulkerviewer.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int shulkerViewer_getLeftPos();

    @Accessor("topPos")
    int shulkerViewer_getTopPos();

    @Accessor("imageWidth")
    int shulkerViewer_getImageWidth();

    @Accessor("imageHeight")
    int shulkerViewer_getImageHeight();
}