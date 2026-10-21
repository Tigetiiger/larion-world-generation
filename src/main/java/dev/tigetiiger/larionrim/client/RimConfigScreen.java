package dev.tigetiiger.larionrim.client;

import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Edits the default diameter in config/larion_rim.json. Existing worlds keep their own value. */
public class RimConfigScreen extends Screen {
    private final Screen parent;
    private EditBox diameter;
    private RimSettings.Values values;

    public RimConfigScreen(Screen parent) {
        super(Component.literal("Larion: Rimworld"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        values = RimSettings.readConfig();
        int cx = width / 2;
        MultiLineTextWidget help = new MultiLineTextWidget(Component.literal(
                "Disc diameter, wall to wall, in blocks (2000 - 200000).\n"
                        + "Applies to NEW worlds only. Existing worlds keep the size they were created with."), font);
        help.setMaxWidth(Math.min(360, width - 32));
        help.setPosition(cx - help.getWidth() / 2, height / 2 - 60);
        addRenderableWidget(help);

        diameter = new EditBox(font, cx - 60, height / 2 - 10, 120, 20, Component.literal("Diameter"));
        diameter.setValue(Integer.toString(values.diameter));
        diameter.setMaxLength(6);
        addRenderableWidget(diameter);

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - 60, height / 2 + 30, 120, 20).build());
    }

    @Override
    public void onClose() {
        try {
            values.diameter = Integer.parseInt(diameter.getValue());
            RimSettings.writeConfig(values);
        } catch (NumberFormatException ignored) {
            // keep the previous value
        }
        minecraft.setScreenAndShow(parent);
    }
}
