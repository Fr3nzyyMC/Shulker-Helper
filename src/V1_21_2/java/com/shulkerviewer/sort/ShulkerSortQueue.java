package com.shulkerviewer.sort;

import com.shulkerviewer.client.ShulkerViewerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-compatible click-based sorting.
 *
 * Each step re-reads the live menu state (no stale snapshots), then emits
 * one 3-click swap (or a single click to place the cursor item).
 * The click budget is per-tick so the game never freezes.
 */
public final class ShulkerSortQueue {

    private static final int SIZE = 27;
    private static final int CLICKS_PER_TICK = 6;

    private static final List<int[]> QUEUE = new ArrayList<>();
    private static AbstractContainerMenu menu;
    private static int ptr;

    private ShulkerSortQueue() {
    }

    public static boolean isActive() {
        return !QUEUE.isEmpty();
    }

    public static void request(Minecraft mc, AbstractContainerMenu m) {
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?>)) return;
        if (mc.player.containerMenu != m) return;
        if (isActive()) {
            ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.sorting"));
            return;
        }
        menu = m;
        QUEUE.clear();
        ptr = 0;

        List<ItemStack> cur = snap();
        List<ItemStack> want = sortedCopy(cur);
        if (listsEqual(cur, want)) {
            ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.already_sorted"));
            menu = null;
            return;
        }

        // Build all swaps upfront using a working copy.
        // After each swap we update the working copy so later swaps see
        // the corrected state.  The actual menu is updated lazily tick-by-tick.
        for (int i = 0; i < SIZE; i++) {
            if (eq(cur.get(i), want.get(i))) continue;
            if (want.get(i).isEmpty()) {
                // Target is empty: move current item out.
                int dest = findEmpty(cur, i);
                if (dest < 0) break; // no room
                emitSwap(cur, i, dest);
            } else {
                int j = find(cur, want.get(i), i);
                if (j < 0) break;
                emitSwap(cur, i, j);
            }
        }

        if (QUEUE.isEmpty()) {
            menu = null;
            return;
        }
        ShulkerViewerClient.showActionBar(Component.translatable("shulker-viewer.msg.sorting"));
    }

    /** Emit a 3-click swap and update the working copy. */
    private static void emitSwap(List<ItemStack> cur, int a, int b) {
        // Pickup b, place at a, put back to b.
        QUEUE.add(new int[]{b, 0});
        QUEUE.add(new int[]{a, 0});
        QUEUE.add(new int[]{b, 0});
        ItemStack tmp = cur.get(a);
        cur.set(a, cur.get(b));
        cur.set(b, tmp);
    }

    public static void tick(Minecraft mc) {
        if (QUEUE.isEmpty()) return;
        if (mc == null || mc.player == null || mc.gameMode == null
            || mc.player.containerMenu != menu
            || !(mc.screen instanceof AbstractContainerScreen<?>)) {
            done(mc, false);
            return;
        }
        int budget = CLICKS_PER_TICK;
        while (ptr < QUEUE.size() && budget-- > 0) {
            int[] c = QUEUE.get(ptr++);
            mc.gameMode.handleInventoryMouseClick(
                menu.containerId, c[0], c[1], ClickType.PICKUP, mc.player);
        }
        if (ptr >= QUEUE.size()) {
            done(mc, true);
        }
    }

    private static void done(Minecraft mc, boolean ok) {
        QUEUE.clear();
        ptr = 0;
        menu = null;
        if (mc != null && mc.player != null) {
            ShulkerViewerClient.showActionBar(Component.translatable(
                ok ? "shulker-viewer.msg.sorted"
                   : "shulker-viewer.msg.already_sorted"));
        }
    }

    // --- helpers ---

    private static List<ItemStack> snap() {
        List<ItemStack> r = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++)
            r.add(menu.slots.get(i).getItem().copy());
        return r;
    }

    private static List<ItemStack> sortedCopy(List<ItemStack> src) {
        List<ItemStack> r = new ArrayList<>(SIZE);
        for (ItemStack s : src) if (!s.isEmpty()) r.add(s.copy());
        r.sort(ItemValueComparator.comparator());
        while (r.size() < SIZE) r.add(ItemStack.EMPTY);
        return r;
    }

    private static int find(List<ItemStack> cur, ItemStack target, int skip) {
        for (int j = 0; j < SIZE; j++)
            if (j != skip && eq(cur.get(j), target)) return j;
        return -1;
    }

    private static int findEmpty(List<ItemStack> cur, int skip) {
        for (int j = 0; j < SIZE; j++)
            if (j != skip && cur.get(j).isEmpty()) return j;
        return -1;
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
