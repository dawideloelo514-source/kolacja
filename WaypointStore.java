package com.sharedwaypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Waypointy aktualnego świata/serwera (osobny plik na każdy świat lub serwer). */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {}.getType();
    private static final List<Waypoint> LIST = new ArrayList<>();
    private static Path file;

    private WaypointStore() {}

    public static void load(String key) {
        LIST.clear();
        String safe = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        file = Config.dir().resolve("worlds").resolve(safe + ".json");
        try {
            if (Files.exists(file)) {
                List<Waypoint> loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), LIST_TYPE);
                if (loaded != null) {
                    for (Waypoint w : loaded) {
                        if (w != null && w.id != null) LIST.add(w);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SharedWaypoints] Nie udało się wczytać waypointów: " + e);
        }
    }

    public static void unload() {
        save();
        LIST.clear();
        file = null;
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(LIST, LIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SharedWaypoints] Nie udało się zapisać waypointów: " + e);
        }
    }

    public static List<Waypoint> all() {
        return LIST;
    }

    public static void add(Waypoint w) {
        LIST.add(w);
    }

    public static void remove(Waypoint w) {
        LIST.remove(w);
    }

    public static Waypoint find(String id) {
        for (Waypoint w : LIST) {
            if (w.id.equals(id)) return w;
        }
        return null;
    }

    public static int countFrom(String owner) {
        int n = 0;
        for (Waypoint w : LIST) {
            if (w.owner != null && w.owner.equalsIgnoreCase(owner)) n++;
        }
        return n;
    }

    public static int countOwn() {
        int n = 0;
        for (Waypoint w : LIST) {
            if (!w.isRemote()) n++;
        }
        return n;
    }
}
