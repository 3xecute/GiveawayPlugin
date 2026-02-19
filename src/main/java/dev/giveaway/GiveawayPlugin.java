package dev.giveaway;

import dev.giveaway.commands.GiveawayCommand;
import dev.giveaway.listeners.ChatListener;
import dev.giveaway.listeners.ChestListener;
import dev.giveaway.managers.ConfigManager;
import dev.giveaway.managers.GiveawayManager;
import dev.giveaway.managers.SetupManager;
import dev.giveaway.managers.VaultManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GiveawayPlugin extends JavaPlugin {

    private static GiveawayPlugin instance;
    private ConfigManager configManager;
    private GiveawayManager giveawayManager;
    private SetupManager setupManager;
    private VaultManager vaultManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        vaultManager = new VaultManager(this);
        giveawayManager = new GiveawayManager(this);
        setupManager = new SetupManager(this);

        GiveawayCommand cmd = new GiveawayCommand(this);
        getCommand("g").setExecutor(cmd);
        getCommand("g").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);

        getLogger().info("Giveaway v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (giveawayManager != null && giveawayManager.isRunning()) {
            giveawayManager.forceStop();
        }
    }

    public static GiveawayPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager()    { return configManager; }
    public GiveawayManager getGiveawayManager(){ return giveawayManager; }
    public SetupManager getSetupManager()      { return setupManager; }
    public VaultManager getVaultManager()      { return vaultManager; }
}
