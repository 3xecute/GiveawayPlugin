package dev.giveaway.managers;

import dev.giveaway.GiveawayPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final GiveawayPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(GiveawayPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getDuration()         { return config.getInt("duration", 60); }
    public int getWinnerCount()      { return Math.max(1, config.getInt("winner-count", 1)); }
    public int getMinParticipants()  { return config.getInt("min-participants", 2); }
    public long getCooldownSeconds() { return config.getLong("cooldown", 300); }
    public int getPreAnnounceBefore(){ return config.getInt("pre-announce-before", 0); }
    public List<Integer> getAnnounceAt() { return config.getIntegerList("announce-at"); }
    public String getChestTitle()    { return colorize(config.getString("chest-title", "&6&lGiveaway Prize Chest")); }
    public boolean isFireworkEnabled(){ return config.getBoolean("firework", true); }
    public String getWinnerSound()   { return config.getString("winner-sound", "ENTITY_FIREWORK_ROCKET_BLAST"); }
    public int getHistorySize()      { return config.getInt("history-size", 10); }

    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "&8[&6Giveaway&8] &r");
        String msg = config.getString("messages." + key, key);
        return colorize(prefix + msg);
    }

    public String getRawMessage(String key) {
        return colorize(config.getString("messages." + key, key));
    }

    public String colorize(String text) {
        if (text == null) return "";
        Pattern hex = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher m = hex.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            StringBuilder r = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) r.append("§").append(c);
            m.appendReplacement(sb, r.toString());
        }
        m.appendTail(sb);
        return sb.toString().replace("&", "§");
    }
}
