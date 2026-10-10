package com.shulkerviewer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sort-button click handling for MC <=1.21.8 (old mouseClicked signature).
 * 1.21.9+ refactored input to MouseButtonEvent (see sibling variant).
 * 1.3: mark-editor key handling (guaranteed delivery while a GUI is open).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenClickMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_sortClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            if (ShulkerSortHook.isOverButton(self, mouseX, mouseY)) {
                ShulkerSortHook.onClick(self);
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer sort button click error", t);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_markKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            if (self instanceof com.shulkerviewer.gui.ShulkerContentViewScreen) return;
            InputConstants.Key bindKey =
                ((KeyMappingAccessor) ShulkerViewerClient.MARK_BIND).shulkerViewer_getKey();
            if (bindKey.getType() != InputConstants.Type.KEYSYM) return;
            if (keyCode != bindKey.getValue()) return;
            if (ShulkerViewerClient.tryOpenMarkScreen()) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer mark key error", t);
        }
    }
}
