package com.sharedwaypoints;

import java.util.UUID;

/** Pojedynczy waypoint. Pola publiczne, bo zapisujemy go przez Gson. */
public class Waypoint {
    public String id;
    public String name = "Waypoint";
    public int x, y, z;
    /** Identyfikator wymiaru, np. minecraft:overworld */
    public String dim = "minecraft:overworld";
    /** Kolor RGB (bez alfy). */
    public int color = 0x3498DB;
    public boolean visible = true;
    /** Czy własny waypoint ma być wysyłany do innych graczy z modem. */
    public boolean shared;
    /** null = mój własny waypoint, inaczej nick gracza, który mi go udostępnił. */
    public String owner;

    public boolean isRemote() {
        return owner != null;
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String shortDim() {
        return switch (dim) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dim;
        };
    }

    public String displayName() {
        return isRemote() ? name + " (" + owner + ")" : name;
    }
}
