package com.shulkerviewer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.client.ShulkerViewerClient;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.3: AnvilScreen overrides keyPressed (see ShulkerOverrideMixin26). The EditBox-focus guard lives in tryOpenMarkScreen. */
@Mixin(AnvilScreen.class)
public abstract class ShulkerAnvilMixin26 {

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
