package me.ellbristow.mychunk;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyChunkVaultLink {
    
    private static MyChunk plugin;
    public static Economy economy = null;
    public boolean foundEconomy = false;
    public String economyName = "";
    
    public MyChunkVaultLink (MyChunk instance) {
        plugin = instance;
        initEconomy();
    }
    
    private void initEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        if (economy != null) {
            foundEconomy = true;
            economyName = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider().getName();
        }
    }
    
    public static Economy getEconomy() {
        return economy;
    }
}
