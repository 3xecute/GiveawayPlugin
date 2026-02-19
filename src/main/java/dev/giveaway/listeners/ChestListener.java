package dev.giveaway.listeners;

import dev.giveaway.GiveawayPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class ChestListener implements Listener {

    private final GiveawayPlugin plugin;

    public ChestListener(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Check if this player is in setup mode
        if (!plugin.getSetupManager().isInSetup(player.getUniqueId())) return;

        // Check if this is the prize inventory (not some other inventory they opened)
        Inventory prizeInv = plugin.getSetupManager().getPrizeInventory();
        if (prizeInv == null) return;

        // Compare titles to make sure it's our chest
        String closedTitle = LegacyComponentSerializer.legacySection()
                .serialize(event.getView().title());
        String expectedTitle = plugin.getConfigManager().getChestTitle();

        if (!closedTitle.equals(expectedTitle)) return;

        // Process the closed inventory
        plugin.getSetupManager().onChestClose(player, event.getInventory());
    }
}
