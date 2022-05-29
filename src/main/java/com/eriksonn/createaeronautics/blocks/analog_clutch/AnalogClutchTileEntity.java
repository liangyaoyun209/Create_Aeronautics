package com.eriksonn.createaeronautics.blocks.analog_clutch;

import com.simibubi.create.content.contraptions.relays.gearbox.GearboxBlock;
import com.simibubi.create.content.contraptions.relays.gearbox.GearshiftTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

public class AnalogClutchTileEntity extends GearshiftTileEntity {

    protected ScrollOptionBehaviour<RotationalGateMode> mode;

    int signal = 0;
    boolean signalChanged = false;

    public AnalogClutchTileEntity(TileEntityType<? extends GearshiftTileEntity> type) {
        super(type);
    }

    @Override
    public void write(CompoundNBT compound, boolean clientPacket) {
        compound.putInt("Signal", signal);
        super.write(compound, clientPacket);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        signal = compound.getInt("Signal");
        super.fromTag(state, compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        mode = new ScrollOptionBehaviour<>(RotationalGateMode.class, new StringTextComponent("Redstone Mode"),  this, getModeSlot());
        mode.setValue(1);
        mode.requiresWrench();
//        behaviours.add(mode);
    }

    private ValueBoxTransform getModeSlot() {

        return new CenteredSideValueBoxTransform((state, d) -> {
            Direction.Axis axis = d.getAxis();
            Direction.Axis bearingAxis = state.getValue(GearboxBlock.AXIS);
            return axis != bearingAxis;
        });
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource()) {
            if (face != getSourceFacing()/* && getBlockState().getValue(BlockStateProperties.POWERED)*/) {
                float power = level.getBestNeighborSignal(worldPosition) / 15.0f;

                if (mode.get().equals(RotationalGateMode.ALLOW_ROTATION)) {
                    return power;
                } else {
                    return 1.0f - power;
                }
            }
        }
        return mode.get().equals(RotationalGateMode.ALLOW_ROTATION) ? 0 : 1;
    }


    @Override
    public void lazyTick() {
        super.lazyTick();
        neighborChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;
        if (signalChanged) {
            signalChanged = false;
            analogSignalChanged(level.getBestNeighborSignal(worldPosition));
        }
    }

    protected void analogSignalChanged(int newSignal) {
        detachKinetics();
        removeSource();
        signal = newSignal;
        attachKinetics();
    }


    public void neighborChanged() {
        if (!hasLevel())
            return;
        int power = level.getBestNeighborSignal(worldPosition);
        if (power != signal)
            signalChanged = true;
    }
}
