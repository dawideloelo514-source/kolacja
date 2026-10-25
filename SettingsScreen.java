package com.sharedwaypoints.screen;

import com.sharedwaypoints.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Ustawienia moda: styl waypointa, odbiorcy, źródła, dystans. */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private ButtonWidget styleB, targetB, acceptB, distB;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Ustawienia waypointów"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 60;

        styleB = addDrawableChild(ButtonWidget.builder(styleLabel(), b -> {
            Config.data.style = Config.data.style == Config.Style.MARKER ? Config.Style.BEAM : Config.Style.MARKER;
            Config.save();
            styleB.setMessage(styleLabel());
        }).dimensions(cx - 130, y, 260, 20)
                .tooltip(Tooltip.of(Text.literal(
                        "Znacznik na ekranie: romb z nazwą, widoczny też przy krawędzi ekranu.\n"
                                + "Kolumna światła: pionowy snop w świecie + etykieta.")))
                .build());

        targetB = addDrawableChild(ButtonWidget.builder(targetLabel(), b -> {
            Config.data.shareTarget = Config.data.shareTarget == Config.ShareTarget.FRIENDS
                    ? Config.ShareTarget.ALL_ONLINE : Config.ShareTarget.FRIENDS;
            Config.save();
            targetB.setMessage(targetLabel());
        }).dimensions(cx - 130, y + 26, 260, 20)
                .tooltip(Tooltip.of(Text.literal(
                        "Uwaga: gracze BEZ moda zobaczą szept ze znacznikiem [SWP1]. "
                                + "Tryb \"Wszyscy online\" wysyła szepty każdemu na serwerze.")))
                .build());

        acceptB = addDrawableChild(ButtonWidget.builder(acceptLabel(), b -> {
            Config.data.acceptFromAnyone = !Config.data.acceptFromAnyone;
            Config.save();
            acceptB.setMessage(acceptLabel());
        }).dimensions(cx - 130, y + 52, 260, 20).build());

        distB = addDrawableChild(ButtonWidget.builder(distLabel(), b -> {
            Config.data.showDistance = !Config.data.showDistance;
            Config.save();
            distB.setMessage(distLabel());
        }).dimensions(cx - 130, y + 78, 260, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(cx - 100, y + 110, 200, 20).build());
    }

    private Text styleLabel() {
        return Text.literal("Wygląd: " + (Config.data.style == Config.Style.MARKER
                ? "Znacznik na ekranie" : "Kolumna światła w świecie"));
    }

    private Text targetLabel() {
        return Text.literal("Wysyłaj do: " + (Config.data.shareTarget == Config.ShareTarget.FRIENDS
                ? "Znajomi" : "Wszyscy online"));
    }

    private Text acceptLabel() {
        return Text.literal("Przyjmuj waypointy od: " + (Config.data.acceptFromAnyone
                ? "Każdego z modem" : "Tylko znajomych"));
    }

    private Text distLabel() {
        return Text.literal("Pokazuj dystans: " + (Config.data.showDistance ? "TAK" : "NIE"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 84, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
