package org.icraftplugins.icraft2plugins;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final IcraftPlugins plugin;
    private Economy econ = null;

    public EconomyManager(IcraftPlugins plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasMoney(Player player, double amount) {
        if (econ == null) return false;
        return econ.has(player, amount);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (econ == null) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}