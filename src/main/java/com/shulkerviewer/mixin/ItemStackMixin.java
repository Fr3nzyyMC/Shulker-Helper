package com.shulkerviewer.mixin;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.mark.ShulkerMarkUtil;
import com.shulkerviewer.util.ShulkerEditUtil;
import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void shulkerViewer_appendKeyHint(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!ShulkerItemUtil.isContainerItem(self)) {
            return;
        }
        List<Component> returnValue = cir.getReturnValue();
        if (returnValue == null) {
            return;
        }
        List<Component> result = new ArrayList<>(returnValue);
        // 1.3: hide vanilla's text content list on shulkers (contents are
        // shown in our own Alt+Shift preview instead). Keeps name, label and
        // our hint lines only.
        if (ShulkerItemUtil.isShulkerBox(self)) {
            stripVanillaContainerLines(result);
        }
        // 1.3 personal label: right under the item name (server-safe, stored
        // in custom_data, rendered client-side).
        if (ShulkerItemUtil.isShulkerBox(self)) {
            String label = ShulkerMarkUtil.getLabel(self);
            if (!label.isEmpty()) {
                Component labelLine = Component.empty()
                    .append(Component.literal("\u2710 ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(label).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                result.add(result.isEmpty() ? 0 : 1, labelLine);
            }
        }
        result.add(Component.literal(""));
        result.addAll(ShulkerViewerClient.getKeyHintLines());
        if (ShulkerItemUtil.isShulkerBox(self) && ShulkerEditUtil.isSingleplayer(Minecraft.getInstance())) {
            result.add(Component.empty()
                .append(Component.translatable("shulker-viewer.hint.real").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.translatable("shulker-viewer.hint.real.key").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        }
        cir.setReturnValue(result);
    }

    /** Removes vanilla shulker content lines + collapses leftover blanks. */
    private static void stripVanillaContainerLines(List<Component> lines) {
        lines.removeIf(ItemStackMixin::isVanillaContainerLine);
        // Collapse consecutive blank separators into one.
        boolean prevBlank = false;
        var it = lines.listIterator();
        while (it.hasNext()) {
            boolean blank = it.next().getString().isEmpty();
            if (blank && prevBlank) {
                it.remove();
            } else {
                prevBlank = blank;
            }
        }
        // No leading blank under the name.
        while (lines.size() > 1 && lines.get(1).getString().isEmpty()) {
            lines.remove(1);
        }
    }

    private static boolean isVanillaContainerLine(Component line) {
        return containsKey(line, "container.shulkerBox.itemCount")
            || containsKey(line, "container.shulkerBox.more")
            || containsKey(line, "container.shulkerBox.unknownContents");
    }

    private static boolean containsKey(Component c, String key) {
        if (c.getContents() instanceof TranslatableContents tc && tc.getKey().equals(key)) {
            return true;
        }
        for (Component sibling : c.getSiblings()) {
            if (containsKey(sibling, key)) return true;
        }
        return false;
    }
}