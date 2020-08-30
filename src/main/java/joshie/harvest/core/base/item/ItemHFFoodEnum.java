package joshie.harvest.core.base.item;

import joshie.harvest.api.HFApi;
import joshie.harvest.core.HFTab;
import joshie.harvest.core.helpers.TextHelper;
import joshie.harvest.core.lib.CreativeSort;
import joshie.harvest.core.util.interfaces.ICreativeSorted;
import joshie.harvest.core.util.interfaces.ISellable;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Locale;

public abstract class ItemHFFoodEnum<I extends ItemHFFoodEnum, E extends Enum<E> & IStringSerializable> extends ItemHFFood<I> implements ICreativeSorted {
    protected final Class<E> enumClass;
    protected final E[] values;
    protected final String prefix;

    public ItemHFFoodEnum(Class<E> clazz) {
        this(HFTab.FARMING, clazz);
    }

    public ItemHFFoodEnum(CreativeTabs tab, Class<E> clazz) {
        super(tab);
        enumClass = clazz;
        values = clazz.getEnumConstants();
        prefix = clazz.getSimpleName().toLowerCase(Locale.ENGLISH);
        setHasSubtypes(true);
        if (values[0] instanceof ISellable) {
            for (E e: values) {
                long value = ((ISellable)e).getSellValue();
                if (value > 0L) {
                    HFApi.shipping.registerSellable(getStackFromEnum(e), value);
                }
            }
        }
    }

    public E[] getValues() {
        return values;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nonnull
    public I setTranslationKey(@Nonnull String name) {
        super.setTranslationKey(name);
        return (I) this;
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    public E getEnumFromStack(@Nonnull ItemStack stack) {
        if (stack.getItem() != this) return null;

        return getEnumFromMeta(stack.getItemDamage());
    }

    public E getEnumFromMeta(int meta) {
        if (meta < 0 || meta >= values.length) {
            meta = 0;
        }

        return values[meta];
    }

    @Nonnull
    public ItemStack getStackFromEnum(E e) {
        return new ItemStack(this, 1, e.ordinal());
    }

    @Nonnull
    public ItemStack getStackFromEnum(E e, int size) {
        return new ItemStack(this, size, e.ordinal());
    }

    @Nonnull
    public ItemStack getStackFromEnumString(String name) {
        return getStackFromEnum(Enum.valueOf(enumClass, name.toUpperCase()));
    }

    @Override
    @Nonnull
    public String getTranslationKey(ItemStack stack) {
        return prefix + "_" + getEnumFromStack(stack).name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        return TextHelper.translate(getTranslationKey(stack).replaceAll("(.)([A-Z])", "$1$2").toLowerCase(Locale.ENGLISH).replace("_", "."));
    }

    @Override
    public int getSortValue(@Nonnull ItemStack stack) {
        return CreativeSort.NONE;
    }

    @Nonnull
    protected ItemStack getCreativeStack(E e) {
        return new ItemStack(this, 1, e.ordinal());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(@Nonnull Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
        for (E e: values) {
            ItemStack stack = getCreativeStack(e);
            if (!stack.isEmpty()) list.add(stack);
        }
    }

    protected String getPrefix(E e) {
        return e.getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(Item item, String name) {
        for (E e: values) {
            ModelLoader.setCustomModelResourceLocation(item, e.ordinal(), new ModelResourceLocation(getRegistryName(), e.getName()));
        }
    }
}