package com.shulkerviewer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.3: recipe-book and creative inventory screens override
 * extractRenderState/keyPressed/mouseClicked without calling super, so the
 * AbstractContainerScreen injectors never fire for them (inventory, crafting,
 * creative). Same handlers, all idempotent, so double-firing on screens that
 * DO call super is harmless.
 */
@Mixin({AbstractRecipeBookScreen.class, CreativeModeInventoryScreen.class})
public abstract class ShulkerOverrideMixin26 {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void shulkerViewer_afterExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            ShulkerViewerClient.onExtract(self, graphics, mouseX, mouseY);
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer extract handler error", t);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_sortClick(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            if (ShulkerSortHook.isOverButton(self, event.x(), event.y())) {
                ShulkerSortHook.onClick(self);
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer sort button click error", t);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_markKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        try {
            InputConstants.Key bindKey =
                ((KeyMappingAccessor) ShulkerViewerClient.MARK_BIND).shulkerViewer_getKey();
            if (bindKey.getType() != InputConstants.Type.KEYSYM) return;
            if (event.key() != bindKey.getValue()) return;
            if (ShulkerViewerClient.tryOpenMarkScreen()) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer mark key error", t);
        }
    }
}
