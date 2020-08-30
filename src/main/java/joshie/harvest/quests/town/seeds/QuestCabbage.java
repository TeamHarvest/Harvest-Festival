package joshie.harvest.quests.town.seeds;

import joshie.harvest.api.calendar.Season;
import joshie.harvest.api.quests.HFQuest;
import joshie.harvest.npcs.HFNPCs;

@HFQuest("seeds.cabbage")
public class QuestCabbage extends QuestShipping {
    public QuestCabbage() {
        super("cabbage", HFNPCs.GS_OWNER, Season.SPRING, 1000);
    }
}
