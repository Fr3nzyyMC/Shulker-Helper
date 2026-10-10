package com.shulkerviewer.sort;

import com.shulkerviewer.client.ShulkerViewerClient;
import com.shulkerviewer.util.ShulkerEditUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.x sorting: the click pipeline was reworked (ClickType is gone), so we
 * edit server-side instead. Sort is singleplayer-only anyway, and the
 * integrated server is in the same JVM: snapshot the first 27 slots,
 * compute the value order, write back and broadcast. Instant, no ticks.
 */
public final class ShulkerSortQueue {

    private static final int SIZE = 27;

    private ShulkerSortQueue() {
    }

    public static boolean isActive() {
        return false;
    }

    public static void request(Minecraft mc, AbstractContainerMenu m) {
        if (mc == null || mc.player == null) return;
        if (!(ShulkerViewerClient.currentScreen(mc) instanceof AbstractContainerScreen<?>)) return;
        if (mc.player.containerMenu != m) return;
        ServerPlayer sp = ShulkerEditUtil.serverPlayer(mc);
        if (sp == null) {
            ShulkerViewerClient.showActionBar(
                Component.translatable("shulker-viewer.msg.sorting_singleplayer_only")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        List<ItemStack> cur = snap(m);
        List<ItemStack> want = sortedCopy(cur);
        if (listsEqual(cur, want)) {
            ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.already_sorted"));
            return;
        }
        ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.sorting"));
        int n = Math.min(SIZE, m.slots.size());
        for (int i = 0; i < n; i++) {
            m.slots.get(i).set(want.get(i).copy());
        }
        m.broadcastChanges();
        ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.sorted"));
    }

    /** No-op on 26.x: sorting is synchronous. Kept for a uniform call shape. */
    public static void tick(Minecraft mc) {
    }

    // --- helpers ---

    private static List<ItemStack> snap(AbstractContainerMenu menu) {
        List<ItemStack> r = new ArrayList<>(SIZE);
        int n = Math.min(SIZE, menu.slots.size());
        for (int i = 0; i < n; i++)
            r.add(menu.slots.get(i).getItem().copy());
        while (r.size() < SIZE) r.add(ItemStack.EMPTY);
        return r;
    }

    private static List<ItemStack> sortedCopy(List<ItemStack> src) {
        List<ItemStack> r = new ArrayList<>(SIZE);
        for (ItemStack s : src) if (!s.isEmpty()) r.add(s.copy());
        r.sort(ItemValueComparator.comparator());
        while (r.size() < SIZE) r.add(ItemStack.EMPTY);
        return r;
    }

    private static boolean eq(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.getCount() == b.getCount()
            && ItemStack.isSameItemSameComponents(a, b);
    }

    private static boolean listsEqual(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!eq(a.get(i), b.get(i))) return false;
        return true;
    }
}
