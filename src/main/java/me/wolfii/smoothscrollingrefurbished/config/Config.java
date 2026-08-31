package me.wolfii.smoothscrollingrefurbished.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("SmoothScrollingRefurbished");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("smoothscrollingrefurbished.json");

    public static final Config INSTANCE = new Config();

    public static final double MAX_PUSHBACK_STRENGTH = 2.0;

    public double scrollStrength;
    public double scrollbarFriction;
    public double pushBackStrength;

    private Config() {
        this.resetToDefaults();
    }

    public boolean isPushbackDisabled() {
        return this.pushBackStrength > Config.MAX_PUSHBACK_STRENGTH;
    }

    public void resetToDefaults() {
        this.scrollStrength = 0.5;
        this.scrollbarFriction = 0.025;
        this.pushBackStrength = 1.0;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            Config.save();
            return;
        }
        try {
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
            if (loaded != null) {
                for (Field field : Config.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.set(INSTANCE, field.get(loaded));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
