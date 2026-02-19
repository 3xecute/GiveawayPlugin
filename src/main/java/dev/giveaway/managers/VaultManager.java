package dev.giveaway.managers;

import dev.giveaway.GiveawayPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultManager {

    private final GiveawayPlugin plugin;
    private Economy economy;
    private boolean available = false;

    public VaultManager(GiveawayPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. Money rewards will be disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            available = true;
            plugin.getLogger().info("Vault hooked successfully.");
        } else {
            plugin.getLogger().warning("No economy plugin found. Money rewards will be disabled.");
        }
    }

    public boolean giveMoney(Player player, double amount) {
        if (!available || economy == null) return false;
        economy.depositPlayer(player, amount);
        return true;
    }

    public boolean isAvailable() {
        return available;
    }
}
