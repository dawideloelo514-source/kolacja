package com.sharedwaypoints.screen;

import com.sharedwaypoints.Sharing;
import com.sharedwaypoints.Waypoint;
import com.sharedwaypoints.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Ekran tworzenia (klawisz I) i edycji waypointa. */
public class WaypointEditScreen extends Screen {
    private static final int[] COLORS = {
            0xE74C3C, 0xF39C12, 0xF1C40F, 0x2ECC71, 0x1ABC9C, 0x3498DB, 0x9B59B6, 0xFF6FB5, 0xFFFFFF
    };
    private static final String[] COLOR_NAMES = {
            "Czerwony", "Pomarańczowy", "Żółty", "Zielony", "Turkusowy", "Niebieski", "Fioletowy", "Różowy", "Biały"
    };

    private final Screen parent;
    private final Waypoint existing;

    private String nameV, xV, yV, zV;
    private int color;
    private boolean visible;
    private boolean shared;
    private final String dim;
    private String error;

    private TextFieldWidget nameField, xField, yField, zField;
    private ButtonWidget colorButton, visibleButton, sharedButton;

    /** @param existing null = nowy waypoint na pozycji gracza */
    public WaypointEditScreen(Screen parent, Waypoint existing) {
        super(Text.literal(existing == null ? "Nowy waypoint" : "Edycja waypointa"));
        this.parent = parent;
        this.existing = existing;

        if (existing == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            BlockPos pos = mc.player != null ? mc.player.getBlockPos() : BlockPos.ORIGIN;
            int count = WaypointStore.countOwn();
            this.nameV = "Waypoint " + (count + 1);
            this.xV = Integer.toString(pos.getX());
            this.yV = Integer.toString(pos.getY());
            this.zV = Integer.toString(pos.getZ());
            this.color = COLORS[count % COLORS.length];
            this.visible = true;
            this.shared = false;
            this.dim = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "minecraft:overworld";
        } else {
            this.nameV = existing.name;
            this.xV = Integer.toString(existing.x);
            this.yV = Integer.toString(existing.y);
            this.zV = Integer.toString(existing.z);
            this.color = existing.color;
            this.visible = existing.visible;
            this.shared = existing.shared;
            this.dim = existing.dim;
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 92;

        nameField = new TextFieldWidget(textRenderer, cx - 100, y + 14, 200, 20, Text.literal("Nazwa"));
        nameField.setMaxLength(20);
        nameField.setText(nameV);
        nameField.setChangedListener(s -> nameV = s);
        addDrawableChild(nameField);

        xField = coordField(cx - 100, y + 56, xV, s -> xV = s, "X");
        yField = coordField(cx - 31, y + 56, yV, s -> yV = s, "Y");
        zField = coordField(cx + 38, y + 56, zV, s -> zV = s, "Z");

        addDrawableChild(ButtonWidget.builder(Text.literal("Moja pozycja"), b -> {
            if (client != null && client.player != null) {
                BlockPos p = client.player.getBlockPos();
                xV = Integer.toString(p.getX());
                yV = Integer.toString(p.getY());
                zV = Integer.toString(p.getZ());
                xField.setText(xV);
                yField.setText(yV);
                zField.setText(zV);
            }
        }).dimensions(cx - 100, y + 82, 200, 20).build());

        colorButton = addDrawableChild(ButtonWidget.builder(colorLabel(), b -> {
            int idx = -1;
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i] == color) idx = i;
            }
            color = COLORS[(idx + 1) % COLORS.length];
            colorButton.setMessage(colorLabel());
        }).dimensions(cx - 100, y + 108, 200, 20).build());

        visibleButton = addDrawableChild(ButtonWidget.builder(visibleLabel(), b -> {
            visible = !visible;
            visibleButton.setMessage(visibleLabel());
        }).dimensions(cx - 100, y + 132, 98, 20).build());

        sharedButton = addDrawableChild(ButtonWidget.builder(sharedLabel(), b -> {
            shared = !shared;
            sharedButton.setMessage(sharedLabel());
        }).dimensions(cx + 2, y + 132, 98, 20)
                .tooltip(Tooltip.of(Text.literal(
                        "Wysyła ten waypoint graczom z tym modem (znajomym lub wszystkim online - zależnie od ustawień).")))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                .dimensions(cx - 100, y + 160, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx + 2, y + 160, 98, 20).build());

        setInitialFocus(nameField);
    }

    private TextFieldWidget coordField(int x, int y, String value, java.util.function.Consumer<String> onChange, String label) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 62, 20, Text.literal(label));
        f.setMaxLength(9);
        f.setText(value);
        f.setChangedListener(onChange);
        return addDrawableChild(f);
    }

    private Text colorLabel() {
        String name = "Własny";
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) name = COLOR_NAMES[i];
        }
        final int c = color;
        return Text.literal("Kolor: ")
                .append(Text.literal("■ ").styled(s -> s.withColor(c)))
                .append(Text.literal(name));
    }

    private Text visibleLabel() {
        return Text.literal("Widoczny: " + (visible ? "TAK" : "NIE"));
    }

    private Text sharedLabel() {
        return Text.literal("Udostępnij: " + (shared ? "TAK" : "NIE"));
    }

    private void save() {
        int x, y, z;
        try {
            x = Integer.parseInt(xV.trim());
            y = Integer.parseInt(yV.trim());
            z = Integer.parseInt(zV.trim());
        } catch (NumberFormatException e) {
            error = "Współrzędne muszą być liczbami całkowitymi.";
            return;
        }
        String name = nameV.trim();
        if (name.isEmpty()) name = "Waypoint";
        name = name.replace("|", "/");

        boolean wasShared = existing != null && existing.shared;
        Waypoint w = existing != null ? existing : new Waypoint();
        if (existing == null) {
            w.id = Waypoint.newId();
            w.dim = dim;
            WaypointStore.add(w);
        }
        w.name = name;
        w.x = x;
        w.y = y;
        w.z = z;
        w.color = color;
        w.visible = visible;
        w.shared = shared;
        WaypointStore.save();

        if (shared) {
            Sharing.sendPut(w);
        } else if (wasShared) {
            Sharing.sendDelete(w);
        }
        close();
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int y = height / 2 - 92;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, y - 14, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Nazwa", cx - 100, y + 4, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "X", cx - 100, y + 46, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Y", cx - 31, y + 46, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Z", cx + 38, y + 46, 0xFFAAAAAA);
        if (error != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, error, cx, y + 186, 0xFFFF5555);
        }
    }
}
