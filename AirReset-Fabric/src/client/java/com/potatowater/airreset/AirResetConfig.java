package com.potatowater.airreset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AirResetConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("airreset.json");

    public boolean enabled = true;
    public float minPitch = 80.0F;
    public double requiredDownwardVelocity = 0.8D;
    public int settleTicks = 5;
    /** Only these servers are allowed. Empty list = no servers (always off). */
    public List<String> serverWhitelist = new ArrayList<>();

    public static AirResetConfig INSTANCE = new AirResetConfig();

    private AirResetConfig() {
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            INSTANCE = new AirResetConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            AirResetConfig loaded = GSON.fromJson(reader, AirResetConfig.class);
            if (loaded != null) {
                if (loaded.serverWhitelist == null) {
                    loaded.serverWhitelist = new ArrayList<>();
                }
                INSTANCE = loaded;
            }
        } catch (IOException exception) {
            AirResetClient.LOGGER.error("Failed to load airreset.json", exception);
            INSTANCE = new AirResetConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException exception) {
            AirResetClient.LOGGER.error("Failed to save airreset.json", exception);
        }
    }
}
