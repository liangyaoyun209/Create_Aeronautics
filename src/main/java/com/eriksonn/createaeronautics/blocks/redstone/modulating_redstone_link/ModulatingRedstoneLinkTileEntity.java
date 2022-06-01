package com.eriksonn.createaeronautics.blocks.redstone.modulating_redstone_link;

import com.eriksonn.createaeronautics.index.CABlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.IRedstoneLinkable;
import com.simibubi.create.content.logistics.block.redstone.RedstoneLinkBlock;
import com.simibubi.create.content.logistics.block.redstone.RedstoneLinkFrequencySlot;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

import static net.minecraft.state.properties.BlockStateProperties.POWERED;

public class ModulatingRedstoneLinkTileEntity  extends SmartTileEntity {

    private boolean receivedSignalChanged;
    private int receivedSignal;
    private int transmittedSignal;
    private LinkBehaviour link;
    boolean isInitialized=false;
    public BlockPos relativeReceiverPosition=BlockPos.ZERO;
    public ModulatingRedstoneLinkTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {

    }
    @Override
    public void addBehavioursDeferred(List<TileEntityBehaviour> behaviours) {
        createLink();
        behaviours.add(link);
    }

    protected void createLink() {
        Pair<ValueBoxTransform, ValueBoxTransform> slots =
                ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);

        link = LinkBehaviour.receiver(this, slots, this::setSignal);

    }
    public void setSignal(int power) {
        receivedSignalChanged = true;
    }
    @Override
    public void write(CompoundNBT compound, boolean clientPacket) {
        compound.putInt("Receive", getReceivedSignal());
        compound.putBoolean("ReceivedChanged", receivedSignalChanged);
        compound.putInt("Transmit", transmittedSignal);
        super.write(compound, clientPacket);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        super.fromTag(state, compound, clientPacket);

        receivedSignal = compound.getInt("Receive");
        receivedSignalChanged = compound.getBoolean("ReceivedChanged");
        if (level == null || level.isClientSide || !link.newPosition)
            transmittedSignal = compound.getInt("Transmit");
    }
    @Override
    public void tick() {
        super.tick();

        if (!isInitialized) {
            isInitialized=true;
            LinkBehaviour prevlink = link;
            removeBehaviour(LinkBehaviour.TYPE);
            createLink();
            link.copyItemsFrom(prevlink);
            attachBehaviourLate(link);
        }



        if (level.isClientSide)
            return;

        BlockState blockState = getBlockState();
        if (!CABlocks.MODULATING_REDSTONE_LINK.has(blockState))
            return;

        if (receivedSignalChanged) {
            Set<IRedstoneLinkable> networkOf = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(level,link);
            int power=0;
            for (IRedstoneLinkable otherLink : networkOf) {
                power = Math.max(power,getCustomNetworkPower((LinkBehaviour)otherLink));
            }
            receivedSignal=power;
        }

        if ((getReceivedSignal() > 0) != blockState.getValue(POWERED)) {
            receivedSignalChanged = true;
            level.setBlockAndUpdate(worldPosition, blockState.cycle(POWERED));
        }

        if (receivedSignalChanged) {
            Direction attachedFace = blockState.getValue(RedstoneLinkBlock.FACING).getOpposite();
            BlockPos attachedPos = worldPosition.relative(attachedFace);
            level.blockUpdated(worldPosition, level.getBlockState(worldPosition).getBlock());
            level.blockUpdated(attachedPos, level.getBlockState(attachedPos).getBlock());
            receivedSignalChanged = false;



        }
    }
    public int getReceivedSignal() {
        return receivedSignal;
    }
    public int getCustomNetworkPower(LinkBehaviour behaviour)
    {
        if(behaviour.isListening())
            return 0;
        double maxDist = 20;
        double minDist = 5;
        BlockPos relativeReceiverPosition = behaviour.getLocation().subtract(getBlockPos());
        double distance = Math.sqrt(relativeReceiverPosition.distSqr(Vector3i.ZERO));
        double fraction = 1-(distance-minDist)/(maxDist-minDist);
        fraction=Math.min(Math.max(fraction,0),1);

        int signal = (int)(fraction*15);
        signal = Math.min(signal,behaviour.getTransmittedStrength());

        return signal;
    }
    public void setTransmitterLink(LinkBehaviour behaviour)
    {
        relativeReceiverPosition = behaviour.getLocation().subtract(getBlockPos());
    }
    //public int getCustomTransmissionPower(LinkBehaviour other)
    //{
    //    relativeReceiverPosition = other.getLocation().subtract(getBlockPos());
    //    return getCustomNetworkPower(other.getTransmittedStrength());
    //}

}
