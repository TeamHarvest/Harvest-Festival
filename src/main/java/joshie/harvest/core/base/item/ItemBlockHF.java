package joshie.harvest.core.base.item;

import joshie.harvest.core.base.block.BlockHFBase;
import joshie.harvest.core.base.block.BlockHFEnum;
import joshie.harvest.core.util.interfaces.ICreativeSorted;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;

import static joshie.harvest.core.lib.HFModInfo.MODID;

public class ItemBlockHF<B extends BlockHFBase> extends ItemBlock implements ICreativeSorted {
    private final B block;

    public ItemBlockHF(B block) {
        super(block);
        this.block = block;
        setHasSubtypes(true);
        if (block instanceof BlockHFEnum) {
            ((BlockHFEnum)block).registerSellables(this);
        }
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        return block.getItemStackDisplayName(stack);
    }

    @Override
    @Nonnull
    public B getBlock() {
        return block;
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public int getEntityLifespan(@Nonnull ItemStack itemStack, World world)  {
        return block.getEntityLifeSpan(itemStack, world);
    }

    @Override
    @Nonnull
    public String getTranslationKey(@Nonnull ItemStack stack) {
        return block.getTranslationKey(stack);
    }

    @Override
    public int getSortValue(@Nonnull ItemStack stack) {
        return block.getSortValue(stack);
    }

    public void register(String name) {
        setTranslationKey(name.replace("_", "."));
        setRegistryName(new ResourceLocation(MODID, name));
        GameRegistry.register(this);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            block.registerModels(this, name);
        }
    }
}