package dev.tigetiiger.larionrim.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu entry point (only loaded on clients that have Mod Menu). */
public class RimModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RimConfigScreen::new;
    }
}
