package joshie.harvest.animals.stats;

import joshie.harvest.animals.HFAnimals;
import joshie.harvest.animals.item.ItemAnimalTreat.Treat;
import joshie.harvest.animals.packet.PacketSyncAnimal;
import joshie.harvest.animals.packet.PacketSyncHappiness;
import joshie.harvest.api.HFApi;
import joshie.harvest.api.animals.AnimalAction;
import joshie.harvest.api.animals.AnimalStats;
import joshie.harvest.api.animals.AnimalTest;
import joshie.harvest.api.animals.IAnimalType;
import joshie.harvest.api.calendar.Season;
import joshie.harvest.api.player.RelationshipType;
import joshie.harvest.core.network.PacketHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static joshie.harvest.core.network.PacketHandler.sendToEveryone;

public class AnimalStatsHF implements AnimalStats<NBTTagCompound> {
    protected static final Random rand = new Random();
    private WeakReference<EntityPlayer> owner;
    protected EntityAnimal animal;
    protected IAnimalType type;
    private int currentLifespan = 0; //How many days this animal has lived for
    private int daysNotFed; //How many subsequent days that this animal has not been fed
    private boolean beenLoved; //If the animal has received love today
    private int happiness; //How happy this animal is

    private boolean isSick; //Whether the animal is sick or not
    private boolean wasSick; //Whether the animal was previously sick
    private boolean hasDied; //Whether this animal is classed as dead

    //Product based stuff
    private int daysPassed; //How many days have passed so far
    private int productsPerDay = 1; //The maximum number of products this animal can produce a day
    private int producedProducts; //Whether the animal has produced products this day
    private boolean golden; //If this animal has won a contest
    private boolean treated; //Whether this animal has had it's treat for today
    private int genericTreats; //Number of generic treats this animal had
    private int typeTreats; //Number of specific treats this animal had
    private boolean wasOutsideInSun;

    public AnimalStatsHF() {
        this.type = HFAnimals.CHICKENS;
    }

    /** Set the animal type
     *  @param type the animal type **/
    public AnimalStatsHF setType(IAnimalType type) {
        this.type = type;
        return this;
    }

    /** Set the animal entity
     *  @param animal the entity **/
    public AnimalStatsHF setEntity(EntityAnimal animal) {
        this.animal = animal;
        return this;
    }

    @Override
    public IAnimalType getType() {
        return type;
    }

    @Override
    public EntityAnimal getAnimal() {
        return animal;
    }

    private int getDeathChance() {
        //If the animal has not been fed, give it a fix changed of dying
        if (daysNotFed > 0) {
            return Math.max(1, 45 - daysNotFed * 3);
        }

        //Gets the adjusted relationship, 0-35k
        double chance = (happiness / (double) RelationshipType.ANIMAL.getMaximumRP()) * 200;
        if (chance <= 1) {
            chance = 1D;
        }

        return (int) chance;
    }

    public void setDead() {
        this.hasDied = true;
    }

    @Override
    public int getProductsPerDay() {
        return productsPerDay;
    }

    @Override
    public void onBihourlyTick() {
        World world = animal.world;
        boolean dayTime = world.isDaytime();
        boolean isRaining = world.isRaining();
        boolean isOutside = world.canBlockSeeSky(new BlockPos(animal));
        boolean isOutsideInSun = !isRaining && isOutside && dayTime && HFApi.calendar.getDate(world).getSeason() != Season.WINTER;
        if (isOutsideInSun && wasOutsideInSun) {
            affectHappiness(type.getRelationshipBonus(AnimalAction.OUTSIDE));
        }

        //Mark the past value
        wasOutsideInSun = isOutsideInSun;
    }

    protected void preStress() {}
    protected void postStress() {}

    protected void updateStats() {
        //Update the maximum produced products
        if (treated && productsPerDay < 5) {
            int requiredGeneric = type.getGenericTreatCount();
            int requiredType = type.getTypeTreatCount();
            if (genericTreats >= requiredGeneric && requiredType >= typeTreats) {
                genericTreats -= requiredGeneric;
                typeTreats -= requiredType;
                productsPerDay++;
            }
        }

        treated = false;
    }

    protected void updatePregnancy() {}

    @Override
    public boolean newDay() {
        if (animal != null) {
            //Check if the animal is going to die
            if (hasDied) return false;
            if (currentLifespan > type.getMaxLifespan()) return false;
            if (currentLifespan > type.getMinLifespan()) {
                if (rand.nextInt(getDeathChance()) == 0) {
                    hasDied = true;
                    return false;
                }
            }

            //If the animal is not sick, check the healthiness
            if (!isSick && daysNotFed >= 0) {
                isSick = true; //Make the animal sick
            } else if (isSick && daysNotFed < 0) isSick = false;

            //Reset everything and increase where appropriate
            currentLifespan++;

            preStress();
            postStress();

            beenLoved = false;
            daysNotFed++;
            daysPassed++;
            updateStats();

            //Updating potion effects on the animal
            if (isSick) {
                wasSick = true;
                animal.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 1000000, 0));
                animal.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 1000000, 0));
                animal.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 1000000, 0));
            } else if (wasSick) {
                wasSick = false;
                animal.removePotionEffect(MobEffects.NAUSEA);
                animal.removePotionEffect(MobEffects.BLINDNESS);
                animal.removePotionEffect(MobEffects.SLOWNESS);
            }

            //Updating grabbing products between animals
            int daysBetween = type.getDaysBetweenProduction();
            if (daysBetween > 0) {
                if (daysPassed >= daysBetween) {
                    daysPassed = 0;
                    producedProducts = 0;
                    type.refreshProduct(this, animal);
                }
            }

            updatePregnancy();
            sendToEveryone(new PacketSyncAnimal(animal.getEntityId(), this));
            return true;
        } else return false;
    }

    @Override
    public boolean canProduce() {
        return !isSick && producedProducts < productsPerDay;
    }

    @Override
    public void setProduced(int amount) {
        producedProducts += amount;
        affectHappiness(getType().getRelationshipBonus(AnimalAction.CLAIM_PRODUCT));
        HFApi.animals.syncAnimalStats(animal);
    }

    @Override
    public boolean performTest(AnimalTest test) {
        if (test == AnimalTest.HAS_EATEN) return daysNotFed < 0;
        else if (test == AnimalTest.IS_SICK) return isSick;
        else if (test == AnimalTest.HAD_TREAT) return treated;
        else if (test == AnimalTest.BEEN_LOVED) return beenLoved;
        else return test == AnimalTest.WON_CONTEST && golden;
    }

    @Override
    public boolean performAction(@Nonnull World world, @Nonnull ItemStack stack, AnimalAction action) {
        if (action == AnimalAction.FEED) return feed(world);
        else if (action == AnimalAction.HEAL) return heal(world);
        else if (action == AnimalAction.MAKE_GOLDEN) return golden(world);
        else if (action == AnimalAction.PETTED) return pet(world);
        return (action == AnimalAction.TREAT_SPECIAL || action == AnimalAction.TREAT_GENERIC) && treat(world, stack);
    }

    private boolean pet(@Nonnull World world) {
        if (!beenLoved) {
            if (!world.isRemote) {
                beenLoved = true;
                affectHappiness(type.getRelationshipBonus(AnimalAction.PETTED));
                HFApi.animals.syncAnimalStats(animal);
            }

            return true;
        }

        return false;
    }

    private boolean golden(@Nonnull World world) {
        if (!golden) {
            if (!world.isRemote) {
                golden = true;
                HFApi.animals.syncAnimalStats(animal);
            }

            return true;
        }

        return false;
    }

    private boolean feed(@Nonnull World world) {
        if (daysNotFed >= 0) {
            if (!world.isRemote) {
                daysNotFed = -1;
                affectHappiness(type.getRelationshipBonus(AnimalAction.FEED));
                HFApi.animals.syncAnimalStats(animal);
            }

            return true;
        }

        return false;
    }

    private boolean heal(@Nonnull World world) {
        if (isSick) {
            animal.clearActivePotions();
            if (!world.isRemote) {
                isSick = false;
                affectHappiness(type.getRelationshipBonus(AnimalAction.HEAL));
                HFApi.animals.syncAnimalStats(animal);
            }

            return true;
        } else return false;
    }

    @Override
    public int getHappiness() {
        return happiness;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void affectHappiness(int amount) {
        if (amount != 0) {
            happiness = Math.max(0, Math.min(RelationshipType.ANIMAL.getMaximumRP(), happiness + amount));
            if (animal != null && !animal.world.isRemote) {
                if (amount < 0) {
                    try {
                        ReflectionHelper.findMethod(EntityLivingBase.class, "playHurtSound", "func_184581_c", DamageSource.class).invoke(animal, DamageSource.STARVE);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                }

                PacketHandler.sendToAllAround(new PacketSyncHappiness(animal.getEntityId(), amount), animal.dimension, animal.posX, animal.posY, animal.posZ, 178);
            }
        }
    }

    @Override
    public void copyHappiness(int parentHappiness, double percentage) {
        happiness = (int)(parentHappiness * (percentage / 100D));
        if (getAnimal() != null) {
            HFApi.animals.syncAnimalStats(getAnimal());
        }
    }

    private void treat(AnimalAction action) {
        treated = true;
        affectHappiness(type.getRelationshipBonus(action));
        HFApi.animals.syncAnimalStats(animal);
    }

    private boolean treat(@Nonnull World world, @Nonnull ItemStack stack) {
        if (!treated) {
            if (HFAnimals.TREATS.getEnumFromStack(stack) == Treat.GENERIC) {
                if (!world.isRemote) {
                    genericTreats++;
                    treat(AnimalAction.TREAT_GENERIC);
                }

                return true;
            } else if (HFAnimals.TREATS.getEnumFromStack(stack).getType() == type) {
                if (!world.isRemote) {
                    typeTreats++;
                    treat(AnimalAction.TREAT_SPECIAL);
                }

                return true;
            } else {
                if (!world.isRemote) {
                    treat(AnimalAction.TREAT_INCORRECT);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        currentLifespan = nbt.getShort("CurrentLifespan");
        daysNotFed = nbt.getByte("DaysNotFed");
        daysPassed = nbt.getByte("DaysPassed");
        treated = nbt.getBoolean("Treated");
        genericTreats = nbt.getShort("GenericTreats");
        typeTreats = nbt.getShort("TypeTreats");
        wasSick = nbt.getBoolean("WasSick");
        isSick = nbt.getBoolean("IsSick");
        hasDied = nbt.getBoolean("IsDead");
        wasOutsideInSun = nbt.getBoolean("WasOutsideInSun");
        golden = nbt.getBoolean("Golden");
        if (type.getDaysBetweenProduction() > 0) {
            productsPerDay = nbt.getByte("NumProducts");
            producedProducts = nbt.getByte("ProducedProducts");
        }

        beenLoved = nbt.getBoolean("BeenLoved");
        happiness = nbt.getShort("Happiness");
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort("CurrentLifespan", (short) currentLifespan);
        tag.setByte("DaysNotFed", (byte) daysNotFed);
        tag.setByte("DaysPassed", (byte) daysPassed);
        tag.setBoolean("Treated", treated);
        tag.setShort("GenericTreats", (short) genericTreats);
        tag.setShort("TypeTreats", (short) typeTreats);
        tag.setBoolean("WasSick", wasSick);
        tag.setBoolean("IsSick", isSick);
        tag.setBoolean("IsDead", hasDied);
        tag.setBoolean("WasOutsideInSun", wasOutsideInSun);
        tag.setBoolean("Golden", golden);

        if (type.getDaysBetweenProduction() > 0) {
            tag.setByte("NumProducts", (byte) productsPerDay);
            tag.setByte("ProducedProducts", (byte) producedProducts);
        }

        tag.setBoolean("BeenLoved", beenLoved);
        tag.setShort("Happiness", (short) happiness);
        return tag;
    }
}
