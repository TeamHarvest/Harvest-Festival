package uk.joshiejack.penguinlib.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerPenguinInventory extends ContainerPenguin {
    private final int inventorySize;
    public ContainerPenguinInventory(int inventorySize) {
        this.inventorySize = inventorySize;
    }

    protected void bindPlayerInventory(InventoryPlayer inventory) {
        bindPlayerInventory(inventory, 0);
    }

    protected void bindPlayerInventory(InventoryPlayer inventory, int yOffset) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(createSlot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + yOffset));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(createSlot(inventory, i, 8 + i * 18, 142 + yOffset));
        }
    }

    protected Slot createSlot(InventoryPlayer inventory, int id, int x, int y) {
        return new Slot(inventory, id, x, y);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        int size = inventorySize;
        int low = size + 27;
        int high = low + 9;
        ItemStack newStack = ItemStack.EMPTY;
        final Slot slot = inventorySlots.get(slotID);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            newStack = stack.copy();
            if (slotID < size) {
                if (!mergeItemStack(stack, size, high, true)) return ItemStack.EMPTY;
            } else if (slot.isItemValid(stack)) {
                if (!mergeItemStack(stack, 0, size, false)) return ItemStack.EMPTY;
            } else if (slotID >= size && slotID < low) {
                if (!mergeItemStack(stack, low, high, false)) return ItemStack.EMPTY;
            } else if (slotID >= low && slotID < high && !mergeItemStack(stack, size, low, false)) return ItemStack.EMPTY;

            if (stack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (stack.getCount() == newStack.getCount()) return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return newStack;
    }
}

