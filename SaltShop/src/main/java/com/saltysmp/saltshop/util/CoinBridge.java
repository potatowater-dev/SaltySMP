package com.saltysmp.saltshop.util;

import com.saltysmp.saltcoins.api.CoinService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class CoinBridge {

    private CoinBridge() {
    }

    public static CoinService get() {
        RegisteredServiceProvider<CoinService> provider =
                Bukkit.getServicesManager().getRegistration(CoinService.class);
        return provider == null ? null : provider.getProvider();
    }
}
