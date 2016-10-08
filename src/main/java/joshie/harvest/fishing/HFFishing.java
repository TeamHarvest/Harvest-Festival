package joshie.harvest.fishing;

import joshie.harvest.core.util.HFLoader;
import joshie.harvest.fishing.condition.ConditionDaytime;
import joshie.harvest.fishing.condition.ConditionLocation;
import joshie.harvest.fishing.condition.ConditionSeason;
import joshie.harvest.fishing.item.ItemFish;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;

@HFLoader
public class HFFishing {
    public static final ItemFish FISH = new ItemFish().register("fish");

    public static void preInit(){
        LootConditionManager.registerCondition(new ConditionDaytime.Serializer());
        LootConditionManager.registerCondition(new ConditionLocation.Serializer());
        LootConditionManager.registerCondition(new ConditionSeason.Serializer());
    }
}
