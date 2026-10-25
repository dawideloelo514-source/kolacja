package com.sharedwaypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Config {
    public enum Style { MARKER, BEAM }

    public enum ShareTarget { FRIENDS, ALL_ONLINE }

    public static class Data {
        /** MARKER = znacznik na ekranie, BEAM = kolumna światła w świecie + etykieta. */
        public Style style = Style.MARKER;
        /** Komu wysyłać udostępnione waypointy. */
        public ShareTarget shareTarget = ShareTarget.FRIENDS;
        /** Czy przyjmować waypointy od każdego, kto je wyśle (inaczej tylko od znajomych). */
        public boolean acceptFromAnyone = true;
        public boolean showDistance = true;
        /** Komenda szeptu na serwerze (np. msg, tell, w). */
        public String whisperCommand = "msg";
        public List<String> friends = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data data = new Data();

    private Config() {}

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("sharedwaypoints");
    }

    private static Path file() {
        return dir().resolve("config.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Data d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
                if (d != null) {
                    data = d;
                    if (data.friends == null) data.friends = new ArrayList<>();
                    if (data.style == null) data.style = Style.MARKER;
                    if (data.shareTarget == null) data.shareTarget = ShareTarget.FRIENDS;
                    if (data.whisperCommand == null || data.whisperCommand.isBlank()) data.whisperCommand = "msg";
                }
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[SharedWaypoints] Nie udało się wczytać configu: " + e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(dir());
            Files.writeString(file(), GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SharedWaypoints] Nie udało się zapisać configu: " + e);
        }
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : data.friends) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static void setFriend(String name, boolean friend) {
        data.friends.removeIf(f -> f.equalsIgnoreCase(name));
        if (friend) data.friends.add(name);
        save();
    }
}
