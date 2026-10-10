package com.shulkerviewer.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.gui.ShulkerContentViewScreen;
import com.shulkerviewer.gui.ShulkerMarkScreen;
import com.shulkerviewer.mark.ShulkerMarkBadge;
import com.shulkerviewer.mark.ShulkerMarkStore;
import com.shulkerviewer.mixin.AbstractContainerScreenAccessor;
import com.shulkerviewer.mixin.KeyMappingAccessor;
import com.shulkerviewer.portable.PortableOpenPoll;
import com.shulkerviewer.portable.PortableOpener;
import com.shulkerviewer.portable.PortableShulkerProvider;
import com.shulkerviewer.sort.ShulkerSortHook;
import com.shulkerviewer.sort.ShulkerSortQueue;
import com.shulkerviewer.sort.SortRequester;
import com.shulkerviewer.util.ShulkerEditUtil;
import com.shulkerviewer.util.ShulkerItemUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShulkerViewerClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ShulkerHelper");

    private static final String KEY_CATEGORY = "key.category.shulker-viewer";

    public static final KeyMapping PREVIEW_BIND = new KeyMapping(
        "key.shulker-viewer.preview",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        KEY_CATEGORY
    );

    public static final KeyMapping OPEN_BIND = new KeyMapping(
        "key.shulker-viewer.open",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_SHIFT,
        KEY_CATEGORY
    );

    public static final KeyMapping MARK_BIND = new KeyMapping(
        "key.shulker-viewer.mark",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_M,
        KEY_CATEGORY
    );

    private static ItemStack hoveredItem = ItemStack.EMPTY;
    private static boolean previewVisible = false;
    private static boolean wasOpenCombo = false;
    private static boolean wasHovering = false;
    private static boolean wasMarkDown = false;
    private static long lastHeartbeat = 0L;
    private static final long HEARTBEAT_INTERVAL_MS = 1000L;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(PREVIEW_BIND);
        KeyBindingHelper.registerKeyBinding(OPEN_BIND);
        KeyBindingHelper.registerKeyBinding(MARK_BIND);

        ShulkerMarkStore.load();

        // Version-specific handlers (called from shared code via registry)
        PortableOpener.setHandler(ShulkerViewerClient::openPortableShulker);
        SortRequester.setHandler(ShulkerSortQueue::request);

        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            ScreenEvents.afterRender(screen).register(ShulkerViewerClient::onAfterScreenRender);
        });

        // RMB portable open: needs to tick EVERY frame even when no screen is open
        ClientTickEvents.END_CLIENT_TICK.register(ShulkerViewerClient::onClientTick);

        LOGGER.info("Shulker Helper 1.3.0 initialized");
    }

    private static void onAfterScreenRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        try {
            Minecraft mc = Minecraft.getInstance();

            int left = 0;
            int top = 0;
            if (mc.screen instanceof AbstractContainerScreen acs
                && !(mc.screen instanceof ShulkerContentViewScreen)) {
                AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) acs;
                left = accessor.shulkerViewer_getLeftPos();
                top = accessor.shulkerViewer_getTopPos();
            }
            // Update hover FIRST so poll/preview see current frame's state
            boolean hovering = updateHoverFromScreen(mc, mouseX, mouseY, left, top);

            ShulkerSortQueue.tick(mc);

            onScreenRender(mc, hovering);

            // 1.3 mark badges (corner icons), drawn after vanilla.
            if (mc.screen instanceof AbstractContainerScreen<?> badgeScreen
                && !(mc.screen instanceof ShulkerContentViewScreen)) {
                try {
                    ShulkerMarkBadge.render(mc, badgeScreen, graphics, mouseX, mouseY);
                } catch (Throwable t) {
                    LOGGER.error("ShulkerViewer mark badge render error", t);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("ShulkerViewer afterRender handler error", t);
        }
    }

    public static boolean updateHoverFromScreen(Minecraft client, int mouseX, int mouseY, int leftPos, int topPos) {
        if (client == null || client.player == null || client.screen == null) {
            setHoveredItem(ItemStack.EMPTY);
            return false;
        }
        if (client.screen instanceof ShulkerContentViewScreen) {
            setHoveredItem(ItemStack.EMPTY);
            return false;
        }
        if (client.screen instanceof AbstractContainerScreen cs) {
            Slot hit = null;
            for (Slot slot : cs.getMenu().slots) {
                if (slot.isActive()
                    && mouseX >= leftPos + slot.x - 1 && mouseX < leftPos + slot.x + 17
                    && mouseY >= topPos + slot.y - 1 && mouseY < topPos + slot.y + 17) {
                    hit = slot;
                }
            }
            ItemStack stack = hit != null && hit.hasItem() ? hit.getItem() : ItemStack.EMPTY;
            boolean hovering = !stack.isEmpty() && ShulkerItemUtil.isContainerItem(stack);
            setHoveredItem(hovering ? stack : ItemStack.EMPTY);
            return hovering;
        }
        setHoveredItem(ItemStack.EMPTY);
        return false;
    }

    public static void setHoveredItem(ItemStack stack) {
        hoveredItem = stack;
        if (stack.isEmpty()) {
            previewVisible = false;
        }
    }

    private static boolean isBindDown(long window, KeyMapping bind) {
        InputConstants.Key key = ((KeyMappingAccessor) bind).shulkerViewer_getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return isEitherModifierDown(window, key.getValue())
                || GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
    }

    /** Left/right variants of a modifier both count (1.3 fix). */
    private static boolean isEitherModifierDown(long window, int keysym) {
        switch (keysym) {
            case GLFW.GLFW_KEY_LEFT_SHIFT:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
            case GLFW.GLFW_KEY_LEFT_CONTROL:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
            case GLFW.GLFW_KEY_LEFT_ALT:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
            default:
                return false;
        }
    }

    private static boolean isShiftDown(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isCtrlDown(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static boolean isAltDown(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    public static void onScreenRender(Minecraft client, boolean hoveringShulker) {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
            lastHeartbeat = now;
            String hov = hoveringShulker
                ? (hoveredItem.isEmpty() ? "slotOk" : hoveredItem.getHoverName().getString())
                : "none";
            String scr = client != null && client.player != null && client.screen != null
                ? client.screen.getClass().getSimpleName() : "null";
            LOGGER.info("HB render-active screen={} hover={}", scr, hov);
        }

        if (client == null || client.player == null) {
            previewVisible = false;
            return;
        }
        if (client.screen instanceof ShulkerContentViewScreen) {
            previewVisible = false;
            return;
        }

        long window = client.getWindow().getWindow();

        boolean alt = isAltDown(window);
        boolean shift = isShiftDown(window);
        boolean ctrl = isCtrlDown(window);

        boolean previewCombo = isBindDown(window, PREVIEW_BIND) && shift && !ctrl;
        previewVisible = previewCombo && hoveringShulker;

        boolean openCombo = isBindDown(window, OPEN_BIND) && ctrl && !alt;
        boolean canOpen = hoveringShulker && ShulkerItemUtil.isShulkerBox(hoveredItem);
        boolean shouldOpen = openCombo && canOpen && !(wasOpenCombo && wasHovering);
        if (openCombo != wasOpenCombo || hoveringShulker != wasHovering) {
            LOGGER.info("State: openCombo={}, hovering={}, previewCombo={}, shouldOpen={}",
                openCombo, hoveringShulker, previewCombo, shouldOpen);
        }
        if (shouldOpen) {
            LOGGER.info("Opening read-only shulker menu (openCombo={}, hovering={})", openCombo, hoveringShulker);
            try {
                client.setScreen(ShulkerContentViewScreen.create(hoveredItem.copy(), client.screen));
            } catch (Exception e) {
                LOGGER.error("Failed to open read-only shulker menu", e);
            }
        }
        wasOpenCombo = openCombo;
        wasHovering = hoveringShulker;
    }

    /** Portable opener: opens the real server-backed menu (singleplayer only). */
    private static boolean openPortableShulker(Minecraft mc, int section, int index) {
        if (mc == null || mc.player == null || mc.gameMode == null) return false;
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return false;
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return false;
        ItemStack shulker;
        if (section == PortableOpener.SECTION_OFFHAND) {
            shulker = mc.player.getOffhandItem();
        } else {
            if (index < 0 || index >= ((com.shulkerviewer.mixin.InventoryAccessor) mc.player.getInventory()).shulkerViewer_getItems().size()) return false;
            shulker = mc.player.getInventory().getItem(index);
        }
        if (shulker.isEmpty() || !ShulkerItemUtil.isShulkerBox(shulker)) return false;
        LOGGER.info("Portable open: section={}, index={}, item={}", section, index, shulker.getHoverName().getString());
        return sp.openMenu(new PortableShulkerProvider(shulker, section, index)).isPresent();
    }

    public static String getBindKeyName(KeyMapping bind) {
        InputConstants.Key key = ((KeyMappingAccessor) bind).shulkerViewer_getKey();
        return key.getDisplayName().getString();
    }

    public static List<Component> getKeyHintLines() {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty()
            .append(Component.translatable("shulker-viewer.hint.preview")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal(getBindKeyName(PREVIEW_BIND))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.translatable("shulker-viewer.hint.modifier.shift")
                .withStyle(ChatFormatting.YELLOW)));

        lines.add(Component.empty()
            .append(Component.translatable("shulker-viewer.hint.open")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal(getBindKeyName(OPEN_BIND))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.translatable("shulker-viewer.hint.modifier.ctrl")
                .withStyle(ChatFormatting.YELLOW)));

        lines.add(Component.empty()
            .append(Component.translatable("shulker-viewer.hint.mark")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal(getBindKeyName(MARK_BIND))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

        return lines;
    }

    /** Version-bridged action-bar message. */
    public static void showActionBar(Component msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) mc.player.displayClientMessage(msg, true);
        } catch (Throwable ignored) {
        }
    }

    /** Version-bridged current screen (field on 1.21.x, gui stack on 26.x). */
    public static Screen currentScreen(Minecraft mc) {
        return mc == null ? null : mc.screen;
    }

    public static boolean isPreviewVisible() {
        return previewVisible && !hoveredItem.isEmpty();
    }

    public static ItemStack getHoveredItem() {
        return hoveredItem;
    }

    private static void onClientTick(Minecraft mc) {
        try {
            PortableOpenPoll.tick(mc);
            ShulkerMarkStore.tick(mc);
            pollMarkKey(mc);
        } catch (Throwable t) {
            LOGGER.error("ShulkerHelper tick error", t);
        }
    }

    /** 1.3: hovering a shulker in a container + MARK_BIND opens the mark editor. */
    private static void pollMarkKey(Minecraft mc) {
        if (mc == null || mc.player == null) return;
        long window;
        try {
            window = mc.getWindow().getWindow();
        } catch (Throwable ignored) {
            return;
        }
        boolean down = isBindDown(window, MARK_BIND);
        boolean pressed = down && !wasMarkDown;
        wasMarkDown = down;
        if (!pressed) return;
        tryOpenMarkScreen();
    }

    /**
     * Opens the mark editor for the currently hovered shulker, if any.
     * Called both from the client tick (polling) and from the screen
     * keyPressed mixin (guaranteed delivery while a GUI is open).
     *
     * @return true if the editor was opened (key event should be swallowed).
     */
    public static boolean tryOpenMarkScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        if (!(mc.screen instanceof AbstractContainerScreen)
            || mc.screen instanceof ShulkerContentViewScreen
            || mc.screen instanceof ShulkerMarkScreen) {
            return false;
        }
        ItemStack hovered = getHoveredItem();
        if (hovered.isEmpty() || !ShulkerItemUtil.isShulkerBox(hovered)) return false;
        try {
            mc.setScreen(new ShulkerMarkScreen(hovered, mc.screen));
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to open shulker mark screen", e);
            return false;
        }
    }
}
