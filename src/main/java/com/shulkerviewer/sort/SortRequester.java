package com.shulkerviewer.sort;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Version-agnostic registry: click-based sorting needs version-specific input
 * APIs, so the implementation lives in the version set and registers here.
 */
public final class SortRequester {

    public interface Handler {
        void request(Minecraft mc, AbstractContainerMenu menu);
    }

    private static volatile Handler handler;

    private SortRequester() {
    }

    public static void setHandler(Handler h) {
        handler = h;
    }

    public static boolean hasHandler() {
        return handler != null;
    }

    public static void request(Minecraft mc, AbstractContainerMenu menu) {
        Handler h = handler;
        if (h != null) h.request(mc, menu);
    }
}
