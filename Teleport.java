package com.sharedwaypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

/** Teleportacja do waypointa - tylko w singleplayerze (przez wbudowany serwer). */
public final class Teleport {
    private Teleport() {}

    public static boolean available() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.isInSingleplayer() && mc.getServer() != null && mc.player != null;
    }

    public static void to(Waypoint w) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!available()) return;
        MinecraftServer server = mc.getServer();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(mc.player.getUuid());
        if (player == null) return;

        String command = String.format(Locale.ROOT, "execute in %s run tp @s %d.5 %d %d.5", w.dim, w.x, w.y, w.z);
        server.execute(() -> {
            // Własne uprawnienia (poziom 4), żeby działało także bez włączonych cheatów.
            ServerCommandSource source = player.getCommandSource().withLevel(4).withSilent();
            server.getCommandManager().executeWithPrefix(source, command);
        });
    }
}
