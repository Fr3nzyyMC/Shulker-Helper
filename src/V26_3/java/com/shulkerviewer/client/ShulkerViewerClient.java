package com.shulkerviewer.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shulkerviewer.gui.ShulkerContentViewScreen;
import com.shulkerviewer.mixin.AbstractContainerScreenAccessor;
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
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShulkerViewerClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ShulkerHelper");

    private static final KeyMapping.Category KEY_CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath("shulker-viewer", "key.category"));

    public static final KeyMapping PREVIEW_BIND = new KeyMapping(
        "key.shulker-viewer.preview",
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_LALT,
        KEY_CATEGORY
    );

    public static final KeyMapping OPEN_BIND = new KeyMapping(
        "key.shulker-viewer.open",
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_LSHIFT,
        KEY_CATEGORY
    );

    private static ItemStack hoveredItem = ItemStack.EMPTY;
    private static boolean previewVisible = false;
    private static boolean wasOpenCombo = false;
    private static boolean wasHovering = false;
    private static boolean wasPreviewCombo = false;
    private static long lastHeartbeat = 0L;
    private static final long HEARTBEAT_INTERVAL_MS = 1000L;

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(PREVIEW_BIND);
        KeyMappingHelper.registerKeyMapping(OPEN_BIND);

        // Version-specific handlers (called from shared code via registry)
        PortableOpener.setHandler(ShulkerViewerClient::openPortableShulker);
        SortRequester.setHandler(ShulkerSortQueue::request);
        // RMB portable open: needs to tick EVERY frame even when no screen is open
        ClientTickEvents.END_CLIENT_TICK.register(ShulkerViewerClient::onClientTick);

        LOGGER.info("Shulker Helper 1.2.0 initialized");
    }

    public static void onExtract(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        try {
            Minecraft client = Minecraft.getInstance();
            int left = 0;
            int top = 0;
            if (client.gui.screen() instanceof AbstractContainerScreen acs
                && !(client.gui.screen() instanceof ShulkerContentViewScreen)) {
                AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) acs;
                left = accessor.shulkerViewer_getLeftPos();
                top = accessor.shulkerViewer_getTopPos();
            }
            // Update hover FIRST so poll/preview see current frame's state
            boolean hovering = updateHoverFromScreen(client, mouseX, mouseY, left, top);

            ShulkerSortQueue.tick(client);

            onScreenRender(client, hovering);

            // Sort button: vanilla extraction is done (TAIL), our fills land on top.
            if (screen instanceof AbstractContainerScreen<?> acs2
                && !(screen instanceof ShulkerContentViewScreen)) {
                ShulkerSortHook.render(acs2, graphics, mouseX, mouseY);
            }
        } catch (Throwable t) {
            LOGGER.error("ShulkerViewer afterRender handler error", t);
        }
    }

    public static boolean updateHoverFromScreen(Minecraft client, int mouseX, int mouseY, int leftPos, int topPos) {
        if (client == null || client.player == null || client.gui.screen() == null) {
            setHoveredItem(ItemStack.EMPTY);
            return false;
        }
        if (client.gui.screen() instanceof ShulkerContentViewScreen) {
            setHoveredItem(ItemStack.EMPTY);
            return false;
        }
        if (client.gui.screen() instanceof AbstractContainerScreen cs) {
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

    public static void onScreenRender(Minecraft client, boolean hoveringShulker) {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
            lastHeartbeat = now;
            String hov = hoveringShulker
                ? (hoveredItem.isEmpty() ? "slotOk" : hoveredItem.getHoverName().getString())
                : "none";
            String scr = client != null && client.player != null && client.gui.screen() != null
                ? client.gui.screen().getClass().getSimpleName() : "null";
            LOGGER.info("HB render-active screen={} hover={}", scr, hov);
        }

        if (client == null || client.player == null) {
            previewVisible = false;
            return;
        }
        if (client.gui.screen() instanceof ShulkerContentViewScreen) {
            previewVisible = false;
            return;
        }

        boolean previewCombo = PREVIEW_BIND.isDown() && client.hasShiftDown() && !client.hasControlDown();
        previewVisible = previewCombo && hoveringShulker;

        boolean openCombo = OPEN_BIND.isDown() && client.hasControlDown() && !client.hasAltDown();
        boolean canOpen = hoveringShulker && ShulkerItemUtil.isShulkerBox(hoveredItem);
        boolean shouldOpen = openCombo && canOpen && !(wasOpenCombo && wasHovering);
        if (openCombo != wasOpenCombo || hoveringShulker != wasHovering) {
            LOGGER.info("State: openCombo={}, hovering={}, previewCombo={}, shouldOpen={}",
                openCombo, hoveringShulker, previewCombo, shouldOpen);
        }
        if (shouldOpen) {
            LOGGER.info("Opening read-only shulker menu (openCombo={}, hovering={})", openCombo, hoveringShulker);
            try {
                client.gui.setScreen(ShulkerContentViewScreen.create(hoveredItem.copy(), client.gui.screen()));
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
        return bind.getTranslatedKeyMessage().getString();
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

        return lines;
    }

    /** Version-bridged action-bar message (overlay on 26.x). */
    public static void showActionBar(Component msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) mc.player.sendOverlayMessage(msg);
        } catch (Throwable ignored) {
        }
    }

    /** Version-bridged current screen (field on 1.21.x, gui stack on 26.x). */
    public static Screen currentScreen(Minecraft mc) {
        if (mc == null || mc.gui == null) return null;
        return mc.gui.screen();
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
        } catch (Throwable t) {
            LOGGER.error("ShulkerHelper tick error", t);
        }
    }
}
