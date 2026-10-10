package com.shulkerviewer.mixin;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.renderer.ShulkerViewerRenderer;
import com.shulkerviewer.sort.ShulkerSortHook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void shulkerViewer_hideVanillaTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (ShulkerViewerClient.isPreviewVisible()) {
            try {
                ShulkerViewerRenderer.renderContainerTooltip(graphics, ShulkerViewerClient.getHoveredItem(), mouseX, mouseY);
            } catch (Throwable t) {
                ShulkerViewerClient.LOGGER.error("ShulkerViewer preview render error", t);
            }
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void shulkerViewer_renderSortButton(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            ShulkerSortHook.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
        } catch (Throwable t) {
            ShulkerViewerClient.LOGGER.error("ShulkerViewer sort button render error", t);
        }
    }
}
