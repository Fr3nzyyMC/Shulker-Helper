package com.shulkerviewer.mixin;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.3: EnchantmentScreen overrides extractRenderState/mouseClicked (see ShulkerOverrideMixin26). */
@Mixin(EnchantmentScreen.class)
public abstract class ShulkerEnchantMixin26 {

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
}
