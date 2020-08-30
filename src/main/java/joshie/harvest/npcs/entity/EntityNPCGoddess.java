package joshie.harvest.npcs.entity;

import io.netty.buffer.ByteBuf;
import joshie.harvest.api.npc.NPC;
import joshie.harvest.core.HFCore;
import joshie.harvest.core.block.BlockFlower.FlowerType;
import joshie.harvest.core.helpers.SpawnItemHelper;
import joshie.harvest.core.lib.HFSounds;
import joshie.harvest.npcs.HFNPCs;
import joshie.harvest.npcs.entity.ai.EntityAISwim;
import joshie.harvest.npcs.entity.ai.EntityAITalkingTo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class EntityNPCGoddess extends EntityNPC<EntityNPCGoddess> {
    private boolean flower;
    private int lastTalk = 1200;

    @SuppressWarnings("unused")
    public EntityNPCGoddess(World world) {
        this(world, HFNPCs.GODDESS);
    }

    public EntityNPCGoddess(World world, NPC npc) {
        super(world, npc);
        setSize(0.6F, (2F * npc.getHeight()));
        setEntityInvulnerable(true);
    }

    private EntityNPCGoddess(EntityNPCGoddess entity) {
        this(entity.world, entity.npc);
        npc = entity.getNPC();
        lover = entity.lover;
    }

    @Override
    protected EntityNPCGoddess getNewEntity(EntityNPCGoddess entity) {
        return new EntityNPCGoddess(entity);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {}

    @Override
    protected void updateFallState(double y, boolean onGroundIn, @Nonnull IBlockState state, @Nonnull BlockPos pos) {}

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwim(this));
        tasks.addTask(1, new EntityAITalkingTo(this));
        tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 3.0F, 1.0F));
        tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
    }

    @Override
    public void travel(float strafe, float up, float forward) {
        if (isInWater()) {
            moveRelative(strafe, up, forward, 0.02F);
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.800000011920929D;
            motionY *= 0.800000011920929D;
            motionZ *= 0.800000011920929D;
        } else if (isInLava()) {
            moveRelative(strafe, up, forward, 0.02F);
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.5D;
            motionY *= 0.5D;
            motionZ *= 0.5D;
        } else {
            float f = 0.91F;

            if (onGround) {
                f = world.getBlockState(new BlockPos(MathHelper.floor(posX), MathHelper.floor(getEntityBoundingBox().minY) - 1, MathHelper.floor(posZ))).getBlock().slipperiness * 0.91F;
            }

            float f1 = 0.16277136F / (f * f * f);
            moveRelative(strafe, up, forward, onGround ? 0.1F * f1 : 0.02F);
            f = 0.91F;

            if (onGround) {
                f = world.getBlockState(new BlockPos(MathHelper.floor(posX), MathHelper.floor(getEntityBoundingBox().minY) - 1, MathHelper.floor(posZ))).getBlock().slipperiness * 0.91F;
            }

            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= (double) f;
            motionY *= (double) f;
            motionZ *= (double) f;
        }

        prevLimbSwingAmount = limbSwingAmount;
        limbSwingAmount = 0F;
        limbSwing = 0F;
    }

    public void setFlower() {
        flower = true;
    }

    @Override
    public void setTalking(EntityPlayer player) {
        super.setTalking(player);
        lastTalk = 600;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        //Spawn Particles around the goddess
        for (int i = 0; i < 16; i++) {
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, posX + 0.1 * rand.nextFloat(), posY + 0.2 * rand.nextFloat(), posZ + 0.1 * rand.nextFloat(), 0, -0.05, 0);
        }

        if (!world.isRemote) {
            if (!isTalking() && lastTalk > 0) {
                lastTalk--;

                if (lastTalk <= 0) {
                    if (flower) {
                        SpawnItemHelper.spawnByEntity(this, HFCore.FLOWERS.getStackFromEnum(FlowerType.GODDESS));
                    }

                    setDead();
                }
            }
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        flower = nbt.getBoolean("Flower");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Flower", flower);
    }

    @Override
    public void writeSpawnData(ByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeBoolean(flower);
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        super.readSpawnData(buf);
        flower = buf.readBoolean();

        for (int i = 0; i < 16; i++) {
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, posX * 0.2 * rand.nextFloat(), posY + 0.5 + 0.2 * rand.nextFloat(), posZ + 0.2 * rand.nextFloat(), 0, 0, 0);
        }

        world.playSound(posX, posY, posZ, HFSounds.GODDESS_SPAWN, SoundCategory.NEUTRAL, 0.5F, 1.1F, true);
    }
}