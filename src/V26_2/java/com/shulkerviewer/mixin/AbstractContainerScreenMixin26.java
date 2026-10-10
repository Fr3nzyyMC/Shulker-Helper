package com.shulkerviewer.mixin;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.gui.ShulkerContentViewScreen;
import com.shulkerviewer.renderer.ShulkerViewerRenderer;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin26 {

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_hideVanillaTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (ShulkerViewerClient.isPreviewVisible()) {
            // extractTooltip runs last in AbstractContainerScreen extraction, so
            // drawing here puts the preview strictly on top. NOTE: must render
            // BEFORE ci.cancel() — a cancelled method never reaches TAIL injectors.
            try {
                ShulkerViewerRenderer.renderContainerTooltip(graphics, ShulkerViewerClient.getHoveredItem(), mouseX, mouseY);
            } catch (Throwable t) {
                ShulkerViewerClient.LOGGER.error("ShulkerViewer preview render error", t);
            }
            ci.cancel();
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
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            if (self instanceof ShulkerContentViewScreen) return;
            com.mojang.blaze3d.platform.InputConstants.Key bindKey =
                ((KeyMappingAccessor) ShulkerViewerClient.MARK_BIND).shulkerViewer_getKey();
            if (bindKey.getType() != com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) return;
            if (event.key() != bindKey.getValue()) return;
            if (ShulkerViewerClient.tryOpenMarkScreen()) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer mark key error", t);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void shulkerViewer_afterExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            ShulkerViewerClient.onExtract(self, graphics, mouseX, mouseY);
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer extract handler error", t);
        }
    }
}
