package com.sharedwaypoints;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Udostępnianie waypointów między klientami BEZ moda serwerowego.
 *
 * Vanilla serwer przekaże tylko czat, więc waypoint jest szyfrowany do krótkiego ciągu
 * [SWP1]xxxx i wysyłany szeptem (/msg) do wybranych graczy. Klient z modem przechwytuje
 * taką wiadomość, ukrywa ją w czacie i dodaje waypoint. Gracze bez moda zobaczyliby
 * surowy ciąg, dlatego domyślnie wysyłamy tylko do znajomych.
 */
public final class Sharing {
    private static final Pattern MARKER = Pattern.compile("\\[SWP1\\]([A-Za-z0-9_-]{8,})");
    private static final int MAX_FROM_ONE_SENDER = 50;
    private static final int MAX_QUEUE = 300;

    private static final ArrayDeque<String> QUEUE = new ArrayDeque<>();
    private static final Set<String> KNOWN_ONLINE = new HashSet<>();
    private static int ticks;

    private Sharing() {}

    public static void reset() {
        QUEUE.clear();
        KNOWN_ONLINE.clear();
        ticks = 0;
    }

    // ---------------------------------------------------------------- odbiór

    public static void registerReceivers() {
        // Szept przychodzi jako wiadomość czatu (podpisana) - znamy nadawcę z profilu.
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                !consume(message.getString(), profileName(sender), message));
        // Serwery z pluginami czatu mogą wysłać to jako wiadomość systemową.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !consume(message.getString(), senderFromSystemMessage(message), message));
    }

    private static String senderFromSystemMessage(Text message) {
        if (message.getContent() instanceof TranslatableTextContent t
                && t.getKey().equals("commands.message.display.incoming")
                && t.getArgs().length > 0) {
            Object a = t.getArgs()[0];
            return (a instanceof Text tx ? tx.getString() : String.valueOf(a)).trim();
        }
        return null;
    }

    /** Zwraca true, jeśli wiadomość zawierała nasz znacznik (wtedy ją ukrywamy). */
    private static boolean consume(String text, String sender, Text original) {
        Matcher m = MARKER.matcher(text);
        if (!m.find()) return false;

        // Echo naszej własnej wiadomości ("Szepczesz do X: ...") - tylko ukryj.
        if (original != null && original.getContent() instanceof TranslatableTextContent t
                && t.getKey().contains("outgoing")) {
            return true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String me = mc.player != null ? mc.player.getName().getString() : "";
        if (sender == null) sender = "?";
        if (sender.equalsIgnoreCase(me)) return true;
        if (!Config.data.acceptFromAnyone && !Config.isFriend(sender)) return true;

        try {
            apply(m.group(1), sender);
        } catch (Exception ignored) {
            // zepsute dane od kogoś - po prostu ignorujemy
        }
        return true;
    }

    private static void apply(String b64, String sender) {
        String[] p = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8).split("\\|", -1);
        if (p.length < 8) return;
        String op = p[0];
        String id = p[1];
        if (!id.matches("[0-9a-f]{32}")) return;

        Waypoint existing = WaypointStore.find(id);

        if (op.equals("D")) {
            if (existing != null && existing.isRemote() && sender.equalsIgnoreCase(existing.owner)) {
                WaypointStore.remove(existing);
                WaypointStore.save();
            }
            return;
        }
        if (!op.equals("P")) return;

        // Nie pozwalamy nadpisać własnych waypointów ani cudzych od innego nadawcy.
        if (existing != null && (!existing.isRemote() || !sender.equalsIgnoreCase(existing.owner))) return;
        if (existing == null && WaypointStore.countFrom(sender) >= MAX_FROM_ONE_SENDER) return;

        String name = p[2].replaceAll("[\\p{Cntrl}§]", "").trim();
        if (name.isEmpty()) name = "Waypoint";
        if (name.length() > 20) name = name.substring(0, 20);

        int x = clamp(Integer.parseInt(p[3]));
        int y = clamp(Integer.parseInt(p[4]));
        int z = clamp(Integer.parseInt(p[5]));
        String dim = switch (p[6]) {
            case "o" -> "minecraft:overworld";
            case "n" -> "minecraft:the_nether";
            case "e" -> "minecraft:the_end";
            default -> p[6];
        };
        if (!dim.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return;
        int color = Integer.parseInt(p[7], 16) & 0xFFFFFF;

        boolean isNew = existing == null;
        Waypoint w = isNew ? new Waypoint() : existing;
        w.id = id;
        w.name = name;
        w.x = x;
        w.y = y;
        w.z = z;
        w.dim = dim;
        w.color = color;
        w.owner = sender;
        w.shared = false;
        if (isNew) {
            w.visible = true;
            WaypointStore.add(w);
        }
        WaypointStore.save();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (isNew && mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("[Waypointy] ").formatted(Formatting.AQUA)
                            .append(Text.literal(sender + " udostępnił(a): " + name + " (" + x + ", " + y + ", " + z + ")")
                                    .formatted(Formatting.WHITE)),
                    false);
        }
    }

    private static int clamp(int v) {
        return Math.max(-30_000_000, Math.min(30_000_000, v));
    }

    // ---------------------------------------------------------------- wysyłanie

    private static String encode(String op, Waypoint w) {
        String name = w.name.replace("|", "/");
        String d = switch (w.dim) {
            case "minecraft:overworld" -> "o";
            case "minecraft:the_nether" -> "n";
            case "minecraft:the_end" -> "e";
            default -> w.dim;
        };
        String raw = String.join("|", op, w.id, name,
                Integer.toString(w.x), Integer.toString(w.y), Integer.toString(w.z),
                d, String.format(Locale.ROOT, "%06x", w.color & 0xFFFFFF));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static void enqueue(String command) {
        if (QUEUE.size() < MAX_QUEUE) QUEUE.add(command);
    }

    public static void sendPut(Waypoint w) {
        if (w.isRemote()) return;
        String data = encode("P", w);
        for (String target : targets()) {
            enqueue(Config.data.whisperCommand + " " + target + " [SWP1]" + data);
        }
    }

    public static void sendDelete(Waypoint w) {
        if (w.isRemote()) return;
        String data = encode("D", w);
        for (String target : targets()) {
            enqueue(Config.data.whisperCommand + " " + target + " [SWP1]" + data);
        }
    }

    /** Wysyła wszystkie własne, udostępnione waypointy do aktualnych odbiorców. */
    public static int sendAllNow() {
        int count = 0;
        for (Waypoint w : new ArrayList<>(WaypointStore.all())) {
            if (!w.isRemote() && w.shared) {
                sendPut(w);
                count++;
            }
        }
        return count;
    }

    private static boolean eligible(String name) {
        return Config.data.shareTarget == Config.ShareTarget.ALL_ONLINE || Config.isFriend(name);
    }

    public static List<String> targets() {
        List<String> res = new ArrayList<>();
        for (String n : onlineNames()) {
            if (eligible(n)) res.add(n);
        }
        return res;
    }

    /** Nicki graczy z listy graczy serwera (bez nas samych). */
    public static List<String> onlineNames() {
        List<String> res = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler h = mc.getNetworkHandler();
        if (h == null || mc.player == null) return res;
        String me = mc.player.getName().getString();
        for (PlayerListEntry e : h.getPlayerList()) {
            String n = profileName(e.getProfile());
            if (n != null && !n.equalsIgnoreCase(me)) res.add(n);
        }
        return res;
    }

    /** Nick z GameProfile - przez refleksję, bo authlib zmieniał API (getName() vs name()). */
    private static String profileName(Object profile) {
        if (profile == null) return null;
        for (String method : new String[] {"name", "getName"}) {
            try {
                Object r = profile.getClass().getMethod(method).invoke(profile);
                if (r instanceof String s) return s;
            } catch (ReflectiveOperationException ignored) {
                // próbujemy następną nazwę
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- tick

    public static void tick(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (mc.player == null || handler == null) return;
        ticks++;

        // Maks. jedna wiadomość na 1,5 s, żeby serwer nie wyrzucił nas za spam.
        if (ticks % 30 == 0 && !QUEUE.isEmpty()) {
            handler.sendChatCommand(QUEUE.poll());
        }

        // Co 2 s sprawdzamy, kto nowy pojawił się online, i wysyłamy mu nasze waypointy.
        if (ticks >= 100 && ticks % 40 == 0) {
            List<String> online = onlineNames();
            Set<String> lower = new HashSet<>();
            for (String n : online) lower.add(n.toLowerCase(Locale.ROOT));
            KNOWN_ONLINE.retainAll(lower);

            for (String n : online) {
                if (KNOWN_ONLINE.add(n.toLowerCase(Locale.ROOT)) && eligible(n)) {
                    for (Waypoint w : WaypointStore.all()) {
                        if (!w.isRemote() && w.shared) {
                            enqueue(Config.data.whisperCommand + " " + n + " [SWP1]" + encode("P", w));
                        }
                    }
                }
            }
        }
    }
}
