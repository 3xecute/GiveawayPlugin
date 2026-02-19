package dev.giveaway.commands;

import dev.giveaway.GiveawayPlugin;
import dev.giveaway.managers.GiveawayManager;
import dev.giveaway.models.GiveawayRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GiveawayCommand implements CommandExecutor, TabCompleter {

    private final GiveawayPlugin plugin;

    public GiveawayCommand(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // /g giveaway — open chest setup
        if (args.length == 1 && args[0].equalsIgnoreCase("giveaway")) {
            if (!sender.hasPermission("giveaway.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            if (plugin.getGiveawayManager().isRunning()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("already-running")); return true;
            }
            long cd = plugin.getGiveawayManager().getCooldownRemaining();
            if (cd > 0) {
                sender.sendMessage(plugin.getConfigManager().getMessage("on-cooldown")
                        .replace("{time}", String.valueOf(cd))); return true;
            }
            if (!(sender instanceof Player admin)) {
                sender.sendMessage("§cOnly players can start a giveaway."); return true;
            }
            plugin.getSetupManager().openChest(admin);
            return true;
        }

        // /g confirm — confirm start (triggered by chat click)
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player player)) return true;
            if (!player.hasPermission("giveaway.admin")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            plugin.getSetupManager().confirm(player);
            return true;
        }

        // /g cancel — cancel running giveaway
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("giveaway.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            if (!plugin.getGiveawayManager().isRunning()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-giveaway")); return true;
            }
            plugin.getGiveawayManager().cancel();
            return true;
        }

        // /g info — current giveaway status
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("giveaway.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            if (!plugin.getGiveawayManager().isRunning()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-giveaway")); return true;
            }
            int participants = plugin.getGiveawayManager().getSession().getValidParticipants().size();
            int timeLeft = plugin.getGiveawayManager().getTimeLeft();
            sender.sendMessage(plugin.getConfigManager().getMessage("info")
                    .replace("{participants}", String.valueOf(participants))
                    .replace("{time}", String.valueOf(timeLeft)));
            return true;
        }

        // /g history — show past giveaways
        if (args.length == 1 && args[0].equalsIgnoreCase("history")) {
            if (!sender.hasPermission("giveaway.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            List<GiveawayRecord> history = plugin.getGiveawayManager().getHistory();
            sender.sendMessage(plugin.getConfigManager().getRawMessage("history-header"));
            if (history.isEmpty()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("history-empty"));
            } else {
                for (GiveawayRecord r : history) {
                    sender.sendMessage(plugin.getConfigManager().getRawMessage("history-entry")
                            .replace("{id}", String.valueOf(r.getId()))
                            .replace("{date}", r.getDate())
                            .replace("{winners}", r.getWinnersString())
                            .replace("{items}", r.getItemsSummary()));
                }
            }
            sender.sendMessage(plugin.getConfigManager().getRawMessage("history-footer"));
            return true;
        }

        // /g reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("giveaway.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload"));
            return true;
        }

        // /g join
        if (args.length == 1 && args[0].equalsIgnoreCase("join")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can join."); return true;
            }
            handleJoin(player);
            return true;
        }

        // /g (no args) — join shortcut
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /g giveaway | /g cancel | /g info | /g history | /g reload");
                return true;
            }
            handleJoin(player);
            return true;
        }

        return true;
    }

    private void handleJoin(Player player) {
        GiveawayManager.JoinResult result = plugin.getGiveawayManager().join(player);
        switch (result) {
            case SUCCESS        -> player.sendMessage(plugin.getConfigManager().getMessage("joined"));
            case NO_GIVEAWAY    -> player.sendMessage(plugin.getConfigManager().getMessage("no-giveaway"));
            case ALREADY_JOINED -> player.sendMessage(plugin.getConfigManager().getMessage("already-joined"));
            case MUTED          -> player.sendMessage(plugin.getConfigManager().getMessage("muted"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("join");
            if (sender.hasPermission("giveaway.admin")) {
                opts.addAll(List.of("giveaway", "cancel", "info", "history", "reload"));
            }
            String partial = args[0].toLowerCase();
            return opts.stream().filter(o -> o.startsWith(partial)).toList();
        }
        return List.of();
    }
}
