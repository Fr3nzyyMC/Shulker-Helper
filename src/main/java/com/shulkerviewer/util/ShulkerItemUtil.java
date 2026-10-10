package com.shulkerviewer.util;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class ShulkerItemUtil {

    public static final int PREVIEW_COLUMNS = 9;
    public static final int PREVIEW_CELLS = 27;

    public record ContainerPreview(List<ItemStack> slots, int hiddenCount) {
    }

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.get(DataComponents.CONTAINER) != null) return true;
        // Proxy/ViaVersion servers may strip the container component: fall
        // back to the item type (needs no components at all).
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
            && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
            return true;
        }
        // Last resort: registry path (immune to class-hierarchy changes).
        Object key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.toString().endsWith("shulker_box");
    }

    public static boolean isBundle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.get(DataComponents.BUNDLE_CONTENTS) != null) return true;
        if (stack.getItem() instanceof net.minecraft.world.item.BundleItem) return true;
        Object key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.toString().endsWith("bundle");
    }

    public static boolean isContainerItem(ItemStack stack) {
        return isShulkerBox(stack) || isBundle(stack);
    }

    /** Debug helper: stable item description (id + count), never throws. */
    public static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        try {
            Object k = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return String.valueOf(k) + " x" + stack.getCount();
        } catch (Throwable t) {
            return stack.getItem().toString();
        }
    }

    public static List<ItemStack> getShulkerContents(ItemStack stack) {
        if (!isShulkerBox(stack)) return List.of();
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        NonNullList<ItemStack> slots = NonNullList.withSize(27, ItemStack.EMPTY);
        if (container != null) {
            container.copyInto(slots);
        }
        return List.copyOf(slots);
    }

    public static ContainerPreview getPreviewContents(ItemStack stack) {
        List<ItemStack> slots = new ArrayList<>(getShulkerContents(stack));
        while (slots.size() < PREVIEW_CELLS) {
            slots.add(ItemStack.EMPTY);
        }
        int hidden = 0;
        if (isBundle(stack)) {
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            slots.clear();
            int i = 0;
            if (bundle != null) {
                for (ItemStack item : bundleItemCopies(bundle)) {
                    if (i < PREVIEW_CELLS) {
                        slots.add(item);
                    } else {
                        hidden++;
                    }
                    i++;
                }
            }
            while (slots.size() < PREVIEW_CELLS) {
                slots.add(ItemStack.EMPTY);
            }
        }
        return new ContainerPreview(List.copyOf(slots), hidden);
    }

    /**
     * Version-bridged bundle contents: {@code itemCopyStream()} up to 26.2,
     * {@code itemCopies()} on 26.3+. Resolved once via reflection so shared
     * code compiles against every version.
     */
    private static volatile java.lang.reflect.Method bundleCopiesMethod;

    @SuppressWarnings("unchecked")
    private static List<ItemStack> bundleItemCopies(BundleContents bundle) {
        java.lang.reflect.Method m = bundleCopiesMethod;
        if (m == null) {
            for (String name : new String[]{"itemCopies", "itemCopyStream"}) {
                try {
                    m = BundleContents.class.getMethod(name);
                    bundleCopiesMethod = m;
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (m == null) return List.of();
        }
        try {
            Object r = m.invoke(bundle);
            if (r instanceof java.util.stream.Stream) {
                return ((java.util.stream.Stream<ItemStack>) r).toList();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return List.of();
    }

    public static int getItemCount(ItemStack stack) {
        return (int) getShulkerContents(stack).stream().filter(item -> !item.isEmpty()).count();
    }

    /**
     * Writes the given 27-slot contents back into the shulker stack.
     * Used by singleplayer editing (sort button, portable open).
     */
    public static void setShulkerContents(ItemStack shulker, List<ItemStack> contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(PREVIEW_CELLS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(PREVIEW_CELLS, contents.size()); i++) {
            ItemStack s = contents.get(i);
            slots.set(i, s == null ? ItemStack.EMPTY : s.copy());
        }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
    }

    /**
     * Sorts shulker contents: merges identical stacks (up to max stack size),
     * then orders by item id with bigger stacks first. Result is always 27 slots.
     */
    public static List<ItemStack> sortAndCompact(List<ItemStack> contents) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : contents) {
            if (s == null || s.isEmpty()) continue;
            ItemStack rest = s.copy();
            for (ItemStack m : merged) {
                if (rest.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(m, rest) && m.getCount() < m.getMaxStackSize()) {
                    int move = Math.min(rest.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    rest.shrink(move);
                }
            }
            if (!rest.isEmpty()) merged.add(rest);
        }
        merged.sort((a, b) -> {
            String ka = BuiltInRegistries.ITEM.getKey(a.getItem()).toString();
            String kb = BuiltInRegistries.ITEM.getKey(b.getItem()).toString();
            int c = ka.compareTo(kb);
            if (c != 0) return c;
            return Integer.compare(b.getCount(), a.getCount());
        });
        while (merged.size() < PREVIEW_CELLS) {
            merged.add(ItemStack.EMPTY);
        }
        return List.copyOf(merged);
    }
}