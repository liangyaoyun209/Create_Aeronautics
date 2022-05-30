package com.eriksonn.createaeronautics.blocks.compass_table;

import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.physics.PhysicsUtils;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.jozufozu.flywheel.util.vec.Vec3;
import com.simibubi.create.content.contraptions.components.crafter.MechanicalCrafterTileEntity;
import com.simibubi.create.content.logistics.block.inventories.AdjustableCrateTileEntity;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class CompassTableTileEntity extends SmartTileEntity {

    public float getCompassRotation() {
        Direction value = this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        if(!hasCompass()) return 0.0f;

        AirshipManager.AirshipOrientedInfo info = AirshipManager.INSTANCE.getInfo(level, getBlockPos());

        // goal of compass
        ItemStack stack = inventory.getStackInSlot(0);
        CompoundNBT tag = stack.getTag();
        boolean inSameDimension = info.level.dimension().equals(CompassItem.getLodestoneDimension(tag).orElse(null));

        if(inSameDimension && tag.contains("LodestonePos")) {
            BlockPos trackingPosition = NBTUtil.readBlockPos(tag.getCompound("LodestonePos"));
            Vector3d trackingVector = new Vector3d(trackingPosition.getX(), trackingPosition.getY(), trackingPosition.getZ()).add(0.5, 0.5, 0.5);

            // current position
            Vector3d position = info.position;

            // difference between goal and current position
            Vector3d difference = trackingVector.subtract(position);

            // normalized vector
            Vector3d directionToLodestone = difference.normalize();

            directionToLodestone = MathUtils.rotateQuatReverse(directionToLodestone, info.orientation);

            directionToLodestone = new Vector3d(directionToLodestone.x, 0, directionToLodestone.z);

            // get angle from 0,0 to directionToLodestone
            float angle = (float) Math.atan2(directionToLodestone.z, directionToLodestone.x);

//            angle += Math.toRadians(value.toYRot() + 90);

            return -angle;
        }

        return 0.0f;
    }

    public class Inv extends ItemStackHandler {
        public Inv() {
            super(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem().equals(Items.COMPASS) && CompassItem.isLodestoneCompass(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            sendData();
        }
    }

    public Inv inventory;
    protected LazyOptional<IItemHandler> invHandler;

    public CompassTableTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        inventory = new Inv();
        invHandler = LazyOptional.of(() -> inventory);
    }
    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {

    }


    /**
     * Returns whether or not this compass table has a valid compass in it
     */
    public boolean hasCompass() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        super.fromTag(state, compound, clientPacket);
        inventory.deserializeNBT(compound.getCompound("inventory"));

    }

    @Override
    protected void write(CompoundNBT compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("inventory", inventory.serializeNBT());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invHandler.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability.equals(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
            return invHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void tick() {
        level.setBlock(getBlockPos(), getBlockState(), 2);
        level.updateNeighborsAt(getBlockPos(), this.getBlockState().getBlock());
    }
}
