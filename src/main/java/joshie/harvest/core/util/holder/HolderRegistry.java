package joshie.harvest.core.util.holder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import joshie.harvest.api.core.Mod;
import joshie.harvest.api.core.Ore;
import joshie.harvest.api.crops.ICropProvider;
import joshie.harvest.core.HFCore;
import joshie.harvest.core.item.ItemSizeable;
import joshie.harvest.core.util.IFMLItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collection;
import java.util.HashMap;

public class HolderRegistry<R> {
    private final HashMap<AbstractItemHolder, R> registry = new HashMap<>();
    private final Multimap<Item, AbstractItemHolder> keyMap = HashMultimap.create();

    public void removeItem(Item item) {
        keyMap.removeAll(item);
    }

    private void registerHolder(Item item, AbstractItemHolder holder, R r) {
        keyMap.get(item).add(holder);
        registry.put(holder, r);
    }

    public void register(Object object, R r) {
        if (object instanceof Item) registerHolder((Item)object, ItemHolder.of((Item)object), r);
        if (object instanceof Block) {
            ItemStack stack = new ItemStack((Block)object);
            registerHolder(stack.getItem(), ItemHolder.of(stack.getItem()), r);
        } else if (object instanceof ItemStack) {
            registerHolder(((ItemStack)object).getItem(), getHolder((ItemStack)object), r);
        } else if (object instanceof Mod) {
            Mod mod = (Mod) object;
            ModHolder holder = ModHolder.of(mod.getMod());
            for (Item item: Item.REGISTRY) {
                if (item.getRegistryName().getResourceDomain().equals(mod.getMod())) {
                    registerHolder(item, holder, r);
                }
            }
        } else if (object instanceof Ore) {
            Ore ore = (Ore) object;
            OreHolder holder = OreHolder.of(ore.getOre());
            for (ItemStack stack: OreDictionary.getOres(ore.getOre())) {
                registerHolder(stack.getItem(), holder, r);
            }
        }
    }

    public boolean matches(ItemStack stack, R type) {
        Collection<AbstractItemHolder> holders = keyMap.get(stack.getItem());
        for (AbstractItemHolder holder: holders) {
            if (holder.matches(stack) && matches(registry.get(holder), type)) {
                return true;
            }
        }

        return false;
    }

    public R getValueOf(ItemStack stack) {
        Collection<AbstractItemHolder> holders = keyMap.get(stack.getItem());
        for (AbstractItemHolder holder: holders) {
            if (holder.matches(stack)) {
                return registry.get(holder);
            }
        }

        return null;
    }

    public boolean matches(R external, R internal) {
        return external == internal;
    }

    private AbstractItemHolder getHolder(ItemStack stack) {
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) return ItemHolder.of(stack.getItem());
        else if (stack.getItem() instanceof ItemSizeable) return SizeableHolder.of(HFCore.SIZEABLE.getObjectFromStack(stack));
        else if (stack.getItem() instanceof ICropProvider) return CropHolder.of(((ICropProvider)stack.getItem()).getCrop(stack));
        else if (stack.getItem() instanceof IFMLItem) return FMLHolder.of(stack);
        else return ItemStackHolder.of(stack);
    }
}
