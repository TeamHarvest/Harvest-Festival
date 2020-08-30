package joshie.harvest.player.relationships;

import joshie.harvest.api.calendar.CalendarDate;
import joshie.harvest.api.npc.NPC;
import joshie.harvest.api.npc.RelationStatus;
import joshie.harvest.api.player.RelationshipType;
import joshie.harvest.core.advancements.EventTrigger;
import joshie.harvest.core.network.PacketHandler;
import joshie.harvest.player.PlayerTrackerServer;
import joshie.harvest.player.packet.PacketSyncGifted;
import joshie.harvest.player.packet.PacketSyncRelationsConnect;
import joshie.harvest.player.packet.PacketSyncRelationship;
import joshie.harvest.player.packet.PacketSyncStatusReset;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collection;

public class RelationshipDataServer extends RelationshipData {
    private final PlayerTrackerServer master;

    public RelationshipDataServer(PlayerTrackerServer master) {
        this.master = master;
    }

    public boolean hasGivenGift(NPC npc, RelationStatus statusType) {
        return status.get(npc).contains(statusType);
    }

    public void setHasGivenGift(NPC npc, RelationStatus statusType) {
        status.get(npc).add(statusType);
    }

    public void talkTo(EntityPlayer player, NPC npc) {
        Collection<RelationStatus> statuses = status.get(npc);
        if (!statuses.contains(RelationStatus.TALKED)) {
            statuses.add(RelationStatus.TALKED);
            affectRelationship(npc, 100);
            syncStatus((EntityPlayerMP) player, npc, RelationStatus.TALKED, true);
        }

        //Add this so we will always have a key for something
        if (!statuses.contains(RelationStatus.MET)) {
            statuses.add(RelationStatus.MET);
            syncStatus((EntityPlayerMP) player, npc, RelationStatus.MET, true);
        }
    }

    @Override
    public boolean gift(EntityPlayer player, NPC npc, int amount) {
        Collection<RelationStatus> statuses = status.get(npc);
        if (!statuses.contains(RelationStatus.GIFTED)) {
            if (amount == 0) return true;
            affectRelationship(npc, amount);
            statuses.add(RelationStatus.GIFTED);
            syncStatus((EntityPlayerMP) player, npc, RelationStatus.GIFTED, true);
            master.getTracking().addGift();
            return true;
        }

        return false;
    }

    @Override
    public void affectRelationship(NPC npc, int amount) {
        int newValue = Math.max(0, Math.min(RelationshipType.NPC.getMaximumRP(), getRelationship(npc) + amount));
        relationships.put(npc, newValue);
        EntityPlayerMP player = master.getAndCreatePlayer();
        if (player != null) {
            if (newValue >= 5000) EventTrigger.INSTANCE.trigger(player, "make_friend");
            PacketHandler.sendToClient(new PacketSyncRelationship(npc, newValue), player);
        }
    }

    public void sync(EntityPlayerMP player) {
        PacketHandler.sendToClient(new PacketSyncRelationsConnect(writeToNBT(new NBTTagCompound())), player);
    }

    private void syncStatus(EntityPlayerMP player, NPC npc, RelationStatus status, boolean value) {
        PacketHandler.sendToClient(new PacketSyncGifted(npc, status, value), player);
    }

    public void resetStatus(CalendarDate yesterday, EntityPlayerMP player) {
        PacketHandler.sendToClient(new PacketSyncStatusReset(yesterday), player);
    }
}