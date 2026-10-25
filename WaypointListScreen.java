package com.sharedwaypoints.screen;

import com.sharedwaypoints.Sharing;
import com.sharedwaypoints.Teleport;
import com.sharedwaypoints.Waypoint;
import com.sharedwaypoints.WaypointStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lista waypointów (klawisz G): ukrywanie, edycja, teleport (singleplayer), usuwanie. */
public class WaypointListScreen extends Screen {
    private static final int TOP = 34;
    private static final int ROW_H = 26;

    private final Screen parent;
    private int page;
    private String pendingDeleteId;
    private final List<Waypoint> pageItems = new ArrayList<>();
    private int left, panelW, perPage, pages;

    public WaypointListScreen(Screen parent) {
        super(Text.literal("Waypointy"));
        this.parent = parent;
    }

    private List<Waypoint> sorted() {
        List<Waypoint> all = new ArrayList<>(WaypointStore.all());
        all.sort(Comparator.comparing((Waypoint w) -> w.isRemote()).thenComparing(w -> w.name.toLowerCase()));
        return all;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 400);
        left = (width - panelW) / 2;
        perPage = Math.max(1, (height - TOP - 66) / ROW_H);

        List<Waypoint> all = sorted();
        pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        pageItems.clear();
        for (int i = page * perPage; i < Math.min(all.size(), (page + 1) * perPage); i++) {
            pageItems.add(all.get(i));
        }

        for (int i = 0; i < pageItems.size(); i++) {
            final Waypoint w = pageItems.get(i);
            int y = TOP + i * ROW_H;
            int right = left + panelW - 4;

            // przyciski od prawej: Usuń, TP, Edytuj, Pokaż/Ukryj
            boolean confirming = w.id.equals(pendingDeleteId);
            addDrawableChild(ButtonWidget.builder(Text.literal(confirming ? "Na pewno?" : "Usuń"), b -> {
                if (w.id.equals(pendingDeleteId)) {
                    WaypointStore.remove(w);
                    WaypointStore.save();
                    if (w.shared && !w.isRemote()) Sharing.sendDelete(w);
                    pendingDeleteId = null;
                } else {
                    pendingDeleteId = w.id;
                }
                clearAndInit();
            }).dimensions(right - 52, y, 52, 20).build());

            ButtonWidget tp = ButtonWidget.builder(Text.literal("TP"), b -> {
                Teleport.to(w);
                close();
            }).dimensions(right - 52 - 2 - 30, y, 30, 20).build();
            tp.active = Teleport.available();
            tp.setTooltip(Tooltip.of(Text.literal(tp.active
                    ? "Teleportuj do waypointa"
                    : "Teleportacja działa tylko w singleplayerze")));
            addDrawableChild(tp);

            ButtonWidget edit = ButtonWidget.builder(Text.literal("Edytuj"), b ->
                    client.setScreen(new WaypointEditScreen(this, w)))
                    .dimensions(right - 52 - 2 - 30 - 2 - 44, y, 44, 20).build();
            edit.active = !w.isRemote();
            if (w.isRemote()) edit.setTooltip(Tooltip.of(Text.literal("To waypoint od innego gracza - możesz go tylko ukryć lub usunąć.")));
            addDrawableChild(edit);

            addDrawableChild(ButtonWidget.builder(Text.literal(w.visible ? "Ukryj" : "Pokaż"), b -> {
                w.visible = !w.visible;
                WaypointStore.save();
                clearAndInit();
            }).dimensions(right - 52 - 2 - 30 - 2 - 44 - 2 - 46, y, 46, 20).build());
        }

        int y1 = height - 56;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            clearAndInit();
        }).dimensions(left, y1, 20, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            clearAndInit();
        }).dimensions(left + 22, y1, 20, 20).build()).active = page < pages - 1;
        addDrawableChild(ButtonWidget.builder(Text.literal("Nowy waypoint (I)"), b ->
                client.setScreen(new WaypointEditScreen(this, null)))
                .dimensions(left + panelW - 150, y1, 150, 20).build());

        int y2 = height - 30;
        int bw = (panelW - 6) / 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("Znajomi"), b ->
                client.setScreen(new FriendsScreen(this)))
                .dimensions(left, y2, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wyślij teraz"), b -> {
            int n = Sharing.sendAllNow();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[Waypointy] Zakolejkowano " + n
                        + " udostępnionych waypointów dla " + Sharing.targets().size() + " graczy."), false);
            }
        }).dimensions(left + bw + 2, y2, bw, 20)
                .tooltip(Tooltip.of(Text.literal("Ponownie wyślij wszystkie udostępnione waypointy do odbiorców online.")))
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ustawienia"), b ->
                client.setScreen(new SettingsScreen(this)))
                .dimensions(left + 2 * (bw + 2), y2, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                .dimensions(left + 3 * (bw + 2), y2, bw, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left - 4, TOP - 6, left + panelW + 4, height - 62, 0x80000000);
        for (int i = 0; i < pageItems.size(); i++) {
            int y = TOP + i * ROW_H;
            ctx.fill(left, y - 2, left + panelW, y + ROW_H - 4, (i % 2 == 0) ? 0x30FFFFFF : 0x18FFFFFF);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        int textW = panelW - 4 - (52 + 2 + 30 + 2 + 44 + 2 + 46) - 22;
        for (int i = 0; i < pageItems.size(); i++) {
            Waypoint w = pageItems.get(i);
            int y = TOP + i * ROW_H;
            ctx.fill(left + 4, y + 3, left + 14, y + 13, 0xFF000000);
            ctx.fill(left + 5, y + 4, left + 13, y + 12, 0xFF000000 | w.color);

            String name = textRenderer.trimToWidth(w.displayName() + (w.shared ? "  ⇄" : ""), textW);
            ctx.drawTextWithShadow(textRenderer, name, left + 20, y, w.visible ? 0xFFFFFFFF : 0xFF777777);
            String info = w.x + ", " + w.y + ", " + w.z + "  ·  " + w.shortDim();
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(info, textW), left + 20, y + 10, 0xFFAAAAAA);
        }

        if (WaypointStore.all().isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Brak waypointów. Naciśnij I, aby dodać pierwszy.",
                    width / 2, TOP + 20, 0xFFAAAAAA);
        }
        ctx.drawTextWithShadow(textRenderer, (page + 1) + " / " + pages, left + 48, height - 50, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
