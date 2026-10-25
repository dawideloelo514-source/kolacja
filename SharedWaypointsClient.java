package com.sharedwaypoints;

import com.sharedwaypoints.screen.WaypointEditScreen;
import com.sharedwaypoints.screen.WaypointListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SharedWaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "sharedwaypoints";

    private static KeyBinding createKey;
    private static KeyBinding listKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        createKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sharedwaypoints.create", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "category.sharedwaypoints"));
        listKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sharedwaypoints.list", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.sharedwaypoints"));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WaypointStore.load(worldKey(client, handler));
            Sharing.reset();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WaypointStore.unload();
            Sharing.reset();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (createKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new WaypointEditScreen(null, null));
                }
            }
            while (listKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new WaypointListScreen(null));
                }
            }
            Sharing.tick(client);
        });

        HudRenderCallback.EVENT.register(HudMarkers::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BeamRenderer::render);
        Sharing.registerReceivers();
    }

    /** Klucz pliku z waypointami: osobny dla każdego świata singleplayer i każdego serwera. */
    private static String worldKey(MinecraftClient client, ClientPlayNetworkHandler handler) {
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "sp_" + client.getServer().getSaveProperties().getLevelName();
        }
        ServerInfo info = handler.getServerInfo();
        return "mp_" + (info != null ? info.address : "unknown");
    }
}
