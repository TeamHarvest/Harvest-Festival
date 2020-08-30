package joshie.harvest.quests.player.meetings;

import com.google.common.collect.Sets;
import joshie.harvest.api.HFApi;
import joshie.harvest.api.calendar.Season;
import joshie.harvest.api.knowledge.Note;
import joshie.harvest.api.npc.NPC;
import joshie.harvest.api.npc.NPCEntity;
import joshie.harvest.api.quests.HFQuest;
import joshie.harvest.api.quests.Quest;
import joshie.harvest.api.quests.QuestQuestion;
import joshie.harvest.api.quests.Selection;
import joshie.harvest.buildings.HFBuildings;
import joshie.harvest.core.helpers.InventoryHelper;
import joshie.harvest.core.helpers.InventoryHelper.SearchType;
import joshie.harvest.crops.HFCrops;
import joshie.harvest.knowledge.HFNotes;
import joshie.harvest.quests.Quests;
import joshie.harvest.quests.selection.TutorialSelection;
import joshie.harvest.tools.HFTools;
import joshie.harvest.town.TownHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.Set;

import static joshie.harvest.api.calendar.Season.AUTUMN;
import static joshie.harvest.api.calendar.Season.SUMMER;
import static joshie.harvest.api.core.ITiered.ToolTier.BASIC;
import static joshie.harvest.core.helpers.InventoryHelper.ITEM_STACK;
import static joshie.harvest.core.helpers.InventoryHelper.SPECIAL;
import static joshie.harvest.npcs.HFNPCs.FLOWER_GIRL;

@HFQuest("tutorial.crops")
public class QuestMeetJade extends QuestQuestion {
    private static final int INTRO = 0;
    private static final int START = 1;
    private static final int TURNIPS = 2;
    private boolean attempted;

    public QuestMeetJade() {
        super(new TutorialSelection("crops"));
        setNPCs(FLOWER_GIRL);
    }

    @Override
    public boolean canStartQuest(Set<Quest> active, Set<Quest> finished) {
        return finished.contains(Quests.YULIF_MEET);
    }

    @Override
    public Selection getSelection(EntityPlayer player, NPCEntity entity) {
        if (!entity.getTown().hasBuilding(HFBuildings.CARPENTER) || isCompletedEarly()) return null;
        return quest_stage <= 0 ? selection : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        if (quest_stage == INTRO) return I18n.translateToLocal(getRegistryName().getNamespace() + ".quest." + getRegistryName().getPath() + ".title.what");
        else return I18n.translateToLocal(getRegistryName().getNamespace() + ".quest." + getRegistryName().getPath() + ".title");
    }

    @Override
    public String getDescription(World world, EntityPlayer player) {
        if (quest_stage == INTRO) return getLocalized("description.jade");
        else if (quest_stage == TURNIPS) return getLocalized("description.turnips");
        else return super.getDescription(world, player);
    }

    @Override
    public String getLocalizedScript(EntityPlayer player, NPC npc) {
        if (!TownHelper.getClosestTownToEntity(player, false).hasBuilding(HFBuildings.CARPENTER)) return null;
        if (isCompletedEarly()) {
            return getLocalized("completed");
        } else if (quest_stage == INTRO) {
         /* Jade says hello to the player, and asks them if they know how to farm */
            return getLocalized("intro");
        } else if (quest_stage == START) {
         /* Jade explains farming, and how you need to hoe the ground, then plant seeds, and then water them everyday
            She asks that you take this hoe, this watering can and three turnip seeds, and return to her when you have 9 turnips
            She also explains that you right click the ground to hoe it
            She explains that you must fill the watering can with water before you can use it
            She also explains that if you wish to speed up growth time, you can sleep, crops will grow as the day ticks over
            She also explains that crops will only grow in very specific seasons */
            return getLocalized("start");
        } else if (quest_stage == TURNIPS) {
            if (attempted) {
                if (InventoryHelper.getHandItemIsIn(player, SPECIAL, SearchType.FLOWER, 5) != null) {
                    /* Jade thanks the player for the flowers, and gives them turnip seeds */
                    return getLocalized("thanks.flowers");
                } else if (InventoryHelper.getHandItemIsIn(player, SPECIAL, SearchType.HOE) != null) {
                    /* Jade thanks the player for the hoe and gives them a hf hoe */
                    return getLocalized("thanks.hoe");
                } else if (InventoryHelper.getHandItemIsIn(player, SPECIAL, SearchType.BUCKET) != null) {
                    /* Jade thanks the player for the bucket and gives them a watering can */
                    return getLocalized("thanks.bucket");
                }
            }

            /* Jade thanks the player for the turnips, She tells the player you know what
               Thanks but you can keep them, She tells the player she doesn't want you to have wasted your time
               to make her happy, she then informs you that you can sell your crops for money
               To do this you just need to get a shipping bin, you can right click to place the crops inside
               Or you can use a hopper if you don't feel like spending all day placing them in
               She explains that you can purchase shipping bins directly from yulif
               She also informs the player that they can come visit her anytime
               She will gladly give them a bag of seeds for 10 flowers
               She however also explains that her variety is limited, and suggests that you build a supermarket
               She also informs the player that the harvest goddess has heard of your great work
               And that she would really like to hear from you, in fact she would love to see a turnip!*/
            if (InventoryHelper.getHandItemIsIn(player, ITEM_STACK, HFCrops.TUTORIAL.getCropStack(9), 9) != null) {
                return getLocalized("complete");
            }

            /*Jade reminds the player that to continue she is after 9 turnips
            She also tells the player that if they've lost their hoe, then they can bring her
            a hoe from the vanilla world, and she will gladly trade for it
            She also tells the player she will trade a watering can for a bucket
            And that she will give them a bag of turnips for 10 flowers*/
            attempted = true;
            return getLocalized("reminder.turnips");
        }

        return null;
    }

    @Override
    public void onChatClosed(EntityPlayer player, NPC npc) {
        if (!TownHelper.getClosestTownToEntity(player, false).hasBuilding(HFBuildings.CARPENTER)) return;
        if (quest_stage == START) {
            rewardItem(player, HFTools.HOES.get(BASIC).getStack());
            rewardItem(player, HFTools.WATERING_CANS.get(BASIC).getStack());
            HFApi.player.getTrackingForPlayer(player).learnNote(HFNotes.CROP_FARMING);
            rewardItem(player, HFCrops.TUTORIAL.getSeedStack(3));
            increaseStage(player);
        } else if (quest_stage == TURNIPS) {
            if (InventoryHelper.getHandItemIsIn(player, ITEM_STACK, HFCrops.TUTORIAL.getCropStack(9), 9) != null) {
                complete(player);
            }

            if (attempted) {
                if (InventoryHelper.takeItemsIfHeld(player, SPECIAL, SearchType.FLOWER, 5) != null) {
                    rewardItem(player, HFCrops.TUTORIAL.getSeedStack(1));
                } else if (InventoryHelper.takeItemsIfHeld(player, SPECIAL, SearchType.HOE) != null) {
                    rewardItem(player, HFTools.HOES.get(BASIC).getStack());
                } else if (InventoryHelper.takeItemsIfHeld(player, SPECIAL, SearchType.BUCKET) != null) {
                    rewardItem(player, HFTools.WATERING_CANS.get(BASIC).getStack());
                }
            }

            attempted = true;
        }
    }

    @Override
    public Set<Note> getNotes() {
        return Sets.newHashSet(HFNotes.CROP_FARMING, HFNotes.SICKLE);
    }

    @Override
    public void onQuestCompleted(EntityPlayer player) {
        //If we finished early
        if (isCompletedEarly()) {
            rewardItem(player, HFTools.HOES.get(BASIC).getStack());
            rewardItem(player, HFTools.WATERING_CANS.get(BASIC).getStack());
        }

        rewardItem(player, HFTools.SICKLES.get(BASIC).getStack());
        Season season = HFApi.calendar.getDate(player.world).getSeason();
        if (season == SUMMER) rewardItem(player, HFCrops.ONION.getSeedStack(3));
        else if (season == AUTUMN) rewardItem(player, HFCrops.SPINACH.getSeedStack(3));
        else rewardItem(player, HFCrops.TURNIP.getSeedStack(3));
    }
}