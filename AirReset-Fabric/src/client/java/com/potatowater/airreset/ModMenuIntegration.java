package com.potatowater.airreset;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            AirResetConfig cfg = AirResetConfig.INSTANCE;
            AtomicReference<String> whitelistRaw = new AtomicReference<>(String.join(", ", cfg.serverWhitelist));
            AtomicBoolean addCurrentServer = new AtomicBoolean(false);

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("AirReset"))
                    .setSavingRunnable(() -> {
                        cfg.serverWhitelist = parseServerList(whitelistRaw.get());
                        if (addCurrentServer.get()) {
                            String current = currentServerHint();
                            if (!current.isBlank()) {
                                boolean exists = cfg.serverWhitelist.stream()
                                        .anyMatch(entry -> entry.equalsIgnoreCase(current));
                                if (!exists) {
                                    cfg.serverWhitelist.add(current);
                                }
                            }
                        }
                        AirResetConfig.save();
                        Minecraft client = Minecraft.getInstance();
                        if (client.player != null) {
                            AirResetClient.evaluateServerGate(client, true);
                        }
                    });

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            general.addEntry(entries.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Master toggle. Still requires a server on the whitelist."))
                    .setSaveConsumer(value -> cfg.enabled = value)
                    .build());

            general.addEntry(entries.startFloatField(Component.literal("Min pitch"), cfg.minPitch)
                    .setDefaultValue(80.0F)
                    .setMin(0.0F)
                    .setMax(90.0F)
                    .setTooltip(Component.literal("How far down you must look (90 = straight down)."))
                    .setSaveConsumer(value -> cfg.minPitch = value)
                    .build());

            general.addEntry(entries.startDoubleField(Component.literal("Required downward velocity"), cfg.requiredDownwardVelocity)
                    .setDefaultValue(0.8D)
                    .setMin(0.01D)
                    .setMax(10.0D)
                    .setTooltip(Component.literal("Minimum fall speed to trigger (threshold)."))
                    .setSaveConsumer(value -> cfg.requiredDownwardVelocity = value)
                    .build());

            general.addEntry(entries.startIntField(Component.literal("Settle ticks"), cfg.settleTicks)
                    .setDefaultValue(5)
                    .setMin(1)
                    .setMax(40)
                    .setTooltip(Component.literal("How long to keep cancelling knock after the reset."))
                    .setSaveConsumer(value -> cfg.settleTicks = value)
                    .build());

            ConfigCategory servers = builder.getOrCreateCategory(Component.literal("Servers"));

            servers.addEntry(entries.startTextDescription(Component.literal(
                    "Empty whitelist = OFF on every server. Type hosts separated by commas. "
                            + "Example: play.example.com, localhost — singleplayer: singleplayer"
            )).build());

            servers.addEntry(entries.startStrField(
                            Component.literal("Server whitelist"),
                            whitelistRaw.get()
                    )
                    .setDefaultValue("")
                    .setTooltip(Component.literal(
                            "Empty = no servers. Example: hypixel.net, play.mysmp.com, singleplayer"
                    ))
                    .setSaveConsumer(whitelistRaw::set)
                    .build());

            String current = currentServerHint();
            servers.addEntry(entries.startBooleanToggle(
                            Component.literal("Add current server (" + (current.isBlank() ? "none" : current) + ")"),
                            false
                    )
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("When you hit Done, also add the server you are currently on."))
                    .setSaveConsumer(addCurrentServer::set)
                    .build());

            return builder.build();
        };
    }

    private static List<String> parseServerList(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String currentServerHint() {
        Minecraft client = Minecraft.getInstance();
        if (client.isSingleplayer()) {
            return "singleplayer";
        }
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return client.getCurrentServer().ip;
        }
        return "";
    }
}
