package com.shulkerviewer.portable;

import net.minecraft.client.Minecraft;

/**
 * Version-agnostic registry: the concrete portable-open implementation lives
 * in the active version set (menu APIs differ between versions), the triggers
 * (RMB polling, mixins) live in shared code and call through here.
 */
public final class PortableOpener {

    public static final int SECTION_MAIN = 0;
    public static final int SECTION_OFFHAND = 1;

    public interface Handler {
        /** Opens the shulker at the given player-inventory location. Returns true if opened. */
        boolean open(Minecraft mc, int section, int index);
    }

    private static volatile Handler handler;

    private PortableOpener() {
    }

    public static void setHandler(Handler h) {
        handler = h;
    }

    public static boolean hasHandler() {
        return handler != null;
    }

    public static boolean open(Minecraft mc, int section, int index) {
        Handler h = handler;
        return h != null && h.open(mc, section, index);
    }
}
