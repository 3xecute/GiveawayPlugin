package dev.giveaway.listeners;

import dev.giveaway.GiveawayPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final GiveawayPlugin plugin;

    public ChatListener(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getGiveawayManager().isRunning()) return;

        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // Allow only /g or /g join — but those are commands, not chat messages.
        // Any regular chat message during giveaway is blocked.
        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("chat-frozen"));
    }
}
