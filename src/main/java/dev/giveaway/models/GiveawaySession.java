package dev.giveaway.models;

import java.util.*;

public class GiveawaySession {

    private final Set<UUID> validParticipants = new LinkedHashSet<>();
    private final List<String> invalidParticipants = new ArrayList<>();
    private final List<UUID> winners = new ArrayList<>();

    public boolean addParticipant(UUID uuid, String playerName, String invalidReason) {
        if (invalidReason != null) {
            invalidParticipants.add(playerName);
            return false;
        }
        if (validParticipants.contains(uuid)) return false;
        validParticipants.add(uuid);
        return true;
    }

    public boolean hasJoined(UUID uuid) { return validParticipants.contains(uuid); }

    public Set<UUID> getValidParticipants()    { return Collections.unmodifiableSet(validParticipants); }
    public List<String> getInvalidParticipants(){ return Collections.unmodifiableList(invalidParticipants); }
    public int getTotalAttempts()              { return validParticipants.size() + invalidParticipants.size(); }

    public List<UUID> selectWinners(int count) {
        if (validParticipants.isEmpty()) return Collections.emptyList();
        List<UUID> pool = new ArrayList<>(validParticipants);
        Collections.shuffle(pool, new Random());
        winners.clear();
        int take = Math.min(count, pool.size());
        for (int i = 0; i < take; i++) winners.add(pool.get(i));
        return Collections.unmodifiableList(winners);
    }

    public List<UUID> getWinners() { return Collections.unmodifiableList(winners); }
}
