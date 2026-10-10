package com.shulkerviewer.portable;

import com.shulkerviewer.util.ShulkerItemUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side menu for a shulker opened without placing it (singleplayer).
 * A vanilla {@link ShulkerBoxMenu} over a {@link SimpleContainer} prefilled
 * from the shulker item: the client sees an ordinary vanilla shulker screen,
 * so every interaction (left/right/shift-click, drag, Q drop, F to offhand)
 * works exactly like a placed shulker — no client hacks, no dupes.
 *
 * 1.3: nesting is impossible by construction — vanilla
 * {@code ShulkerBoxSlot.mayPlace} rejects every shulker box (including the
 * opened one itself), for clicks, drags and shift-clicks alike.
 *
 * Contents are written back into the source shulker item on every change and
 * on close. {@code removed()} deliberately does NOT call super: vanilla
 * would return the whole container into the player inventory (dupe).
 */
public class PortableShulkerMenu extends ShulkerBoxMenu {

    private final SimpleContainer box;
    private final int section;
    private final int index;
    private ServerPlayer owner;

    public PortableShulkerMenu(int containerId, Inventory playerInventory,
                               SimpleContainer box, int section, int index) {
        super(containerId, playerInventory, box);
        this.box = box;
        this.section = section;
        this.index = index;
    }

    void setOwner(ServerPlayer owner) {
        this.owner = owner;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (owner != null && container == box) {
            saveBox(owner);
        }
    }

    @Override
    public void removed(Player player) {
        // NOTE: no super.removed() — it would clearContainer() the box back
        // into the player inventory on top of the saved shulker (dupe).
        if (player instanceof ServerPlayer sp) {
            saveBox(sp);
            // The edited item lives in the player inventory, outside this
            // menu's synced slots: push a full refresh so the client tooltip
            // shows fresh contents immediately.
            sp.inventoryMenu.broadcastFullState();
        }
    }

    private void saveBox(ServerPlayer sp) {
        Inventory inv = sp.getInventory();
        ItemStack shulker;
        if (section == PortableOpener.SECTION_OFFHAND) {
            shulker = sp.getOffhandItem();
            if (shulker.isEmpty()) {
                dropAll(sp);
                return;
            }
        } else {
            if (index < 0 || index >= ((com.shulkerviewer.mixin.InventoryAccessor) inv).shulkerViewer_getItems().size()) {
                dropAll(sp);
                return;
            }
            shulker = inv.getItem(index);
        }
        if (!ShulkerItemUtil.isShulkerBox(shulker)) {
            // Source slot no longer holds the shulker (died/cheated): don't
            // void the items, drop them at the player instead.
            dropAll(sp);
            return;
        }
        List<ItemStack> cur = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) {
            cur.add(box.getItem(i));
        }
        ShulkerItemUtil.setShulkerContents(shulker, cur);
        inv.setChanged();
    }

    private void dropAll(ServerPlayer sp) {
        for (int i = 0; i < 27; i++) {
            ItemStack s = box.getItem(i);
            if (!s.isEmpty()) {
                sp.drop(s.copy(), false);
                box.setItem(i, ItemStack.EMPTY);
            }
        }
        box.setChanged();
    }
}
