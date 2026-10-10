package com.shulkerviewer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.gui.ShulkerContentViewScreen;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sort-button click handling for MC 1.21.9+ (MouseButtonEvent input refactor).
 * 1.3: mark-editor key handling (guaranteed delivery while a GUI is open).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenClickMixin {

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
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            if (self instanceof ShulkerContentViewScreen) return;
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
