package com.sharedwaypoints.screen;

import com.sharedwaypoints.Config;
import com.sharedwaypoints.Sharing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/** Lista znajomych, którzy dostają Twoje udostępnione waypointy (online + zapisani). */
public class FriendsScreen extends Screen {
    private static final int TOP = 34;
    private static final int ROW_H = 24;

    private final Screen parent;
    private int page;
    private int left, panelW, perPage, pages;
    private final List<String> pageItems = new ArrayList<>();
    private Set<String> online = new HashSet<>();
    private String addValue = "";

    public FriendsScreen(Screen parent) {
        super(Text.literal("Znajomi z modem"));
        this.parent = parent;
    }

    private List<String> names() {
        TreeMap<String, String> map = new TreeMap<>();
        for (String f : Config.data.friends) map.put(f.toLowerCase(Locale.ROOT), f);
        for (String o : Sharing.onlineNames()) map.putIfAbsent(o.toLowerCase(Locale.ROOT), o);
        return new ArrayList<>(map.values());
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 320);
        left = (width - panelW) / 2;
        perPage = Math.max(1, (height - TOP - 80) / ROW_H);

        online = new HashSet<>();
        for (String o : Sharing.onlineNames()) online.add(o.toLowerCase(Locale.ROOT));

        List<String> all = names();
        pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        pageItems.clear();
        for (int i = page * perPage; i < Math.min(all.size(), (page + 1) * perPage); i++) {
            pageItems.add(all.get(i));
        }

        for (int i = 0; i < pageItems.size(); i++) {
            final String n = pageItems.get(i);
            int y = TOP + i * ROW_H;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(Config.isFriend(n) ? "Znajomy: TAK" : "Znajomy: NIE"), b -> {
                        Config.setFriend(n, !Config.isFriend(n));
                        clearAndInit();
                    }).dimensions(left + panelW - 100, y, 96, 20).build());
        }

        int y1 = height - 54;
        TextFieldWidget addField = new TextFieldWidget(textRenderer, left, y1, panelW - 64, 20, Text.literal("Nick"));
        addField.setMaxLength(16);
        addField.setText(addValue);
        addField.setPlaceholder(Text.literal("Nick gracza, np. Steve"));
        addField.setChangedListener(s -> addValue = s);
        addDrawableChild(addField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj"), b -> {
            String n = addValue.trim();
            if (n.matches("[A-Za-z0-9_]{1,16}")) {
                Config.setFriend(n, true);
                addValue = "";
                clearAndInit();
            }
        }).dimensions(left + panelW - 60, y1, 60, 20).build());

        int y2 = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            clearAndInit();
        }).dimensions(left, y2, 20, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            clearAndInit();
        }).dimensions(left + 22, y2, 20, 20).build()).active = page < pages - 1;
        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(left + panelW - 100, y2, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        String hint = Config.data.shareTarget == Config.ShareTarget.ALL_ONLINE
                ? "Tryb: wysyłanie do WSZYSTKICH online (lista znajomych ignorowana)"
                : "Waypointy są wysyłane tylko znajomym oznaczonym TAK";
        ctx.drawCenteredTextWithShadow(textRenderer, textRenderer.trimToWidth(hint, width - 10), width / 2, 22, 0xFFAAAAAA);

        for (int i = 0; i < pageItems.size(); i++) {
            String n = pageItems.get(i);
            int y = TOP + i * ROW_H;
            boolean on = online.contains(n.toLowerCase(Locale.ROOT));
            ctx.fill(left, y + 5, left + 8, y + 13, on ? 0xFF55FF55 : 0xFF777777);
            ctx.drawTextWithShadow(textRenderer, n, left + 14, y + 6, on ? 0xFFFFFFFF : 0xFF888888);
        }
        if (pageItems.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Nikogo tu nie ma - dodaj nick poniżej.",
                    width / 2, TOP + 16, 0xFFAAAAAA);
        }
        ctx.drawTextWithShadow(textRenderer, (page + 1) + " / " + pages, left + 48, height - 22, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
