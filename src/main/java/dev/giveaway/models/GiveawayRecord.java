package dev.giveaway.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GiveawayRecord {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final int id;
    private final String date;
    private final List<String> winnerNames;
    private final String itemsSummary;

    public GiveawayRecord(int id, List<String> winnerNames, String itemsSummary) {
        this.id = id;
        this.date = LocalDateTime.now().format(FMT);
        this.winnerNames = winnerNames;
        this.itemsSummary = itemsSummary;
    }

    public int getId()               { return id; }
    public String getDate()          { return date; }
    public List<String> getWinnerNames() { return winnerNames; }
    public String getItemsSummary()  { return itemsSummary; }

    public String getWinnersString() {
        return String.join(", ", winnerNames);
    }
}
