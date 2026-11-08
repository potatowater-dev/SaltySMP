package com.saltysmp.saltplots;

import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.config.PlotStorage;
import com.saltysmp.saltplots.model.Plot;
import com.saltysmp.saltplots.service.PlotGenerationService;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

final class PlotDataMigration {

    static final int CURRENT_VERSION = 2;

    private PlotDataMigration() {
    }

    static void migrateIfNeeded(SaltPlotsPlugin plugin, PluginConfig config, PlotStorage storage) {
        int stored = config.getPlotDataVersion();
        if (stored >= CURRENT_VERSION) {
            return;
        }

        PlotGenerationService generationService = new PlotGenerationService(config);
        World world = Bukkit.getWorld(config.getPlotWorld());
        List<Plot> existing = new ArrayList<>(storage.getAllPlots());

        if (world != null) {
            for (Plot plot : existing) {
                generationService.clearPlatform(world, plot);
            }
        } else {
            plugin.getLogger().warning(
                    "Plot world '" + config.getPlotWorld() + "' not loaded — cleared plot data only."
            );
        }

        storage.clearAllPlots();
        config.setPlotDataVersion(CURRENT_VERSION);
        plugin.getLogger().info(
                "SaltPlots v" + CURRENT_VERSION + ": wiped " + existing.size()
                        + " plot(s) for new spacing (max plot + one max plot gap). Players can /plot create again."
        );
    }
}
