package me.wolfii.smoothscrollingrefurbished.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (lastScreen) -> new ConfigScreen(lastScreen, Minecraft.getInstance().options);
    }
}
