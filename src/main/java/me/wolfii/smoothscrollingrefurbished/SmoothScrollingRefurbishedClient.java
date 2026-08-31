package me.wolfii.smoothscrollingrefurbished;

import me.wolfii.smoothscrollingrefurbished.config.Config;
import net.fabricmc.api.ClientModInitializer;

public class SmoothScrollingRefurbishedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
    }
}
