package dev.giveaway.managers;

import dev.giveaway.GiveawayPlugin;
import dev.giveaway.models.GiveawayRecord;
import dev.giveaway.models.GiveawaySession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GiveawayManager {

    private final GiveawayPlugin plugin;
    private GiveawaySession session;
    private BukkitTask countdownTask;
    private BukkitTask preAnnounceTask;
    private boolean running = false;
    private int timeLeft;
    private long lastGiveawayEnd = 0;
    private int historyCounter = 0;

    private List<ItemStack> prizeItems = new ArrayList<>();
    private final LinkedList<GiveawayRecord> history = new LinkedList<>();

    public GiveawayManager(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning()          { return running; }
    public GiveawaySession getSession() { return session; }
    public int getTimeLeft()            { return timeLeft; }
    public List<GiveawayRecord> getHistory() { return Collections.unmodifiableList(history); }

    public void setPrizeItems(List<ItemStack> items) { this.prizeItems = items; }

    // ─── Pre-announce ─────────────────────────────────────────────────────────

    public void schedulePreAnnounce(int secondsBefore) {
        if (secondsBefore <= 0) return;
        preAnnounceTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String msg = plugin.getConfigManager().getMessage("pre-announce")
                    .replace("{time}", String.valueOf(secondsBefore));
            broadcast(msg);
        }, 20L); // fires immediately (caller delays if needed — used for manual trigger)
    }

    // ─── Cooldown ─────────────────────────────────────────────────────────────

    public long getCooldownRemaining() {
        long cooldown = plugin.getConfigManager().getCooldownSeconds() * 1000L;
        if (cooldown <= 0 || lastGiveawayEnd == 0) return 0;
        long remaining = cooldown - (System.currentTimeMillis() - lastGiveawayEnd);
        return Math.max(0, remaining / 1000);
    }

    // ─── Start ────────────────────────────────────────────────────────────────

    public StartResult start() {
        long cd = getCooldownRemaining();
        if (cd > 0) return StartResult.ON_COOLDOWN;
        if (running) return StartResult.ALREADY_RUNNING;

        running = true;
        session = new GiveawaySession();
        timeLeft = plugin.getConfigManager().getDuration();

        // Broadcast start with clickable JOIN button
        broadcastStartMessage();

        List<Integer> announceAt = plugin.getConfigManager().getAnnounceAt();

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (announceAt.contains(timeLeft)) {
                String msg = plugin.getConfigManager().getMessage("countdown")
                        .replace("{time}", String.valueOf(timeLeft));
                broadcast(msg);
            }
            if (timeLeft <= 0) { finish(); return; }
            timeLeft--;
        }, 0L, 20L);

        return StartResult.SUCCESS;
    }

    private void broadcastStartMessage() {
        ConfigManager cm = plugin.getConfigManager();

        Component startLine = LegacyComponentSerializer.legacySection()
                .deserialize(cm.getMessage("start"));

        Component joinButton = LegacyComponentSerializer.legacySection()
                .deserialize(cm.getRawMessage("start-join-button"))
                .clickEvent(ClickEvent.runCommand("/g join"))
                .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection()
                                .deserialize(cm.getRawMessage("start-join-hover"))
                ));

        Component full = startLine.append(Component.text(" ")).append(joinButton);
        Bukkit.getServer().sendMessage(full);
    }

    // ─── Finish ───────────────────────────────────────────────────────────────

    private void finish() {
        countdownTask.cancel();
        running = false;
        lastGiveawayEnd = System.currentTimeMillis();

        ConfigManager cm = plugin.getConfigManager();

        // Min participants check
        int validCount = session.getValidParticipants().size();
        int minRequired = cm.getMinParticipants();
        if (validCount < minRequired) {
            String msg = cm.getMessage("not-enough")
                    .replace("{count}", String.valueOf(validCount))
                    .replace("{min}", String.valueOf(minRequired));
            broadcast(msg);
            prizeItems.clear();
            return;
        }

        // Results
        broadcast(cm.getRawMessage("results-header"));
        broadcast(cm.getRawMessage("results-total")
                .replace("{total}", String.valueOf(session.getTotalAttempts())));

        List<UUID> validList = new ArrayList<>(session.getValidParticipants());
        broadcast(cm.getRawMessage("results-valid").replace("{count}", String.valueOf(validList.size())));
        if (!validList.isEmpty()) {
            StringBuilder sb = new StringBuilder("§a");
            for (int i = 0; i < validList.size(); i++) {
                Player p = Bukkit.getPlayer(validList.get(i));
                sb.append(p != null ? p.getName() : validList.get(i).toString());
                if (i < validList.size() - 1) sb.append("§7, §a");
            }
            broadcast(sb.toString());
        }

        List<String> invalidList = session.getInvalidParticipants();
        broadcast(cm.getRawMessage("results-invalid").replace("{count}", String.valueOf(invalidList.size())));
        if (!invalidList.isEmpty()) {
            StringBuilder sb = new StringBuilder("§c§m");
            for (int i = 0; i < invalidList.size(); i++) {
                sb.append(invalidList.get(i));
                if (i < invalidList.size() - 1) sb.append("§r§7, §c§m");
            }
            broadcast(sb.toString());
        }
        broadcast(cm.getRawMessage("results-footer"));

        // Select winners
        List<UUID> winners = session.selectWinners(cm.getWinnerCount());
        if (winners.isEmpty()) {
            broadcast(cm.getMessage("no-winner"));
            prizeItems.clear();
            return;
        }

        List<String> winnerNames = new ArrayList<>();
        for (UUID wid : winners) {
            Player w = Bukkit.getPlayer(wid);
            String wName = w != null ? w.getName() : wid.toString();
            winnerNames.add(wName);

            broadcast(cm.getMessage("winner").replace("{winner}", wName));
            broadcast(cm.getMessage("winner-prize"));

            // Personal message
            if (w != null) {
                w.sendMessage(cm.getMessage("winner-personal"));
                // Give items
                List<ItemStack> itemCopies = new ArrayList<>(prizeItems);
                for (ItemStack item : itemCopies) {
                    if (item == null || item.getType().isAir()) continue;
                    w.getInventory().addItem(item.clone()).values()
                            .forEach(leftover -> w.getWorld().dropItemNaturally(w.getLocation(), leftover));
                }
                // Firework
                if (cm.isFireworkEnabled()) spawnFirework(w);
                // Sound
                playWinnerSound(w);
            }
        }

        // Save history
        String itemsSummary = buildItemsSummary();
        GiveawayRecord record = new GiveawayRecord(++historyCounter, winnerNames, itemsSummary);
        history.addFirst(record);
        int maxHistory = cm.getHistorySize();
        while (history.size() > maxHistory) history.removeLast();

        prizeItems.clear();
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    public void cancel() {
        if (countdownTask != null) countdownTask.cancel();
        running = false;
        prizeItems.clear();
        broadcast(plugin.getConfigManager().getMessage("cancelled"));
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    public JoinResult join(Player player) {
        if (!running || session == null) return JoinResult.NO_GIVEAWAY;
        if (session.hasJoined(player.getUniqueId())) return JoinResult.ALREADY_JOINED;
        if (isMuted(player)) {
            session.addParticipant(player.getUniqueId(), player.getName(), "muted");
            return JoinResult.MUTED;
        }
        session.addParticipant(player.getUniqueId(), player.getName(), null);
        return JoinResult.SUCCESS;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isMuted(Player player) {
        // Only check metadata set by mute plugins (Essentials, CMI, etc.)
        if (player.hasMetadata("muted"))
            return player.getMetadata("muted").stream().anyMatch(m -> m.asBoolean());
        return false;
    }

    private void spawnFirework(Player player) {
        Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.YELLOW, Color.ORANGE, Color.WHITE)
                .withFade(Color.ORANGE)
                .withTrail()
                .withFlicker()
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private void playWinnerSound(Player player) {
        String soundName = plugin.getConfigManager().getWinnerSound();
        if (soundName == null || soundName.isBlank()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid winner-sound: " + soundName);
        }
    }

    private String buildItemsSummary() {
        if (prizeItems.isEmpty()) return "None";
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack item : prizeItems) {
            if (item == null || item.getType().isAir()) continue;
            String name = item.getType().name();
            counts.merge(name, item.getAmount(), Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        counts.forEach((mat, amt) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(amt).append("x ").append(mat);
        });
        return sb.toString();
    }

    public void forceStop() {
        if (countdownTask != null) countdownTask.cancel();
        running = false;
        prizeItems.clear();
    }

    private void broadcast(String message) {
        if (message == null || message.isBlank()) return;
        Bukkit.getServer().sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public enum JoinResult   { SUCCESS, NO_GIVEAWAY, ALREADY_JOINED, MUTED }
    public enum StartResult  { SUCCESS, ALREADY_RUNNING, ON_COOLDOWN }
}
