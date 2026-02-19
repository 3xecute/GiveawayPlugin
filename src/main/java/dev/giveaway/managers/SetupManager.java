package dev.giveaway.managers;

import dev.giveaway.GiveawayPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SetupManager {

    private final GiveawayPlugin plugin;
    private UUID pendingAdmin = null;
    private Inventory prizeInventory = null;
    private List<ItemStack> pendingItems = null;

    public SetupManager(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isInSetup(UUID uuid)       { return uuid.equals(pendingAdmin); }
    public boolean hasPendingConfirm(UUID uuid){ return uuid.equals(pendingAdmin) && pendingItems != null; }
    public Inventory getPrizeInventory()       { return prizeInventory; }

    public void openChest(Player admin) {
        pendingAdmin = admin.getUniqueId();
        pendingItems = null;
        prizeInventory = Bukkit.createInventory(null, 27,
                LegacyComponentSerializer.legacySection().deserialize(
                        plugin.getConfigManager().getChestTitle()));
        admin.openInventory(prizeInventory);
        admin.sendMessage(plugin.getConfigManager().getMessage("chest-open"));
    }

    public void onChestClose(Player admin, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) items.add(item.clone());
        }

        if (items.isEmpty()) {
            admin.sendMessage(plugin.getConfigManager().getMessage("no-items"));
            cancel();
            return;
        }

        pendingItems = items;

        // Send pre-announce if configured
        int preAnnounce = plugin.getConfigManager().getPreAnnounceBefore();
        if (preAnnounce > 0) {
            String msg = plugin.getConfigManager().getMessage("pre-announce")
                    .replace("{time}", String.valueOf(preAnnounce));
            Bukkit.broadcastMessage(msg);
        }

        // Clickable confirm button
        Component confirmMsg = LegacyComponentSerializer.legacySection()
                .deserialize(plugin.getConfigManager().getRawMessage("confirm-text"))
                .clickEvent(ClickEvent.runCommand("/g confirm"))
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection()
                                .deserialize(plugin.getConfigManager().getRawMessage("confirm-hover"))
                ));
        admin.sendMessage(confirmMsg);
    }

    public void confirm(Player admin) {
        if (!hasPendingConfirm(admin.getUniqueId())) {
            admin.sendMessage(plugin.getConfigManager().getMessage("no-giveaway"));
            return;
        }
        List<ItemStack> items = new ArrayList<>(pendingItems);
        cancel();
        plugin.getGiveawayManager().setPrizeItems(items);

        GiveawayManager.StartResult result = plugin.getGiveawayManager().start();
        if (result == GiveawayManager.StartResult.ON_COOLDOWN) {
            long cd = plugin.getGiveawayManager().getCooldownRemaining();
            admin.sendMessage(plugin.getConfigManager().getMessage("on-cooldown")
                    .replace("{time}", String.valueOf(cd)));
        }
    }

    public void cancel() {
        pendingAdmin = null;
        pendingItems = null;
        prizeInventory = null;
    }
}
