package com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing;

import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingTileEntity;
import com.eriksonn.createaeronautics.physics.AbstractContraptionRigidbody;
import com.eriksonn.createaeronautics.utils.BearingContraptionExtension;

import com.eriksonn.createaeronautics.utils.MathUtils;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;

import static net.minecraft.state.properties.BlockStateProperties.FACING;

public class GyroscopicPropellerBearingTileEntity extends PropellerBearingTileEntity{
    public Quaternion tiltQuat;
    public Vector3d blockNormal;
    public Vector3d tiltVector=new Vector3d(0,1,0);
    private Vector3d targetVector=new Vector3d(0,1,0);
    boolean powered=false;
    public GyroscopicPropellerBearingTileEntity(TileEntityType<? extends PropellerBearingTileEntity> type) {
        super(type);
        tiltQuat=new Quaternion(0,0,0,1);
        tiltQuat.normalize();
    }

    @Override
    public void assemble() {
        if (!(level.getBlockState(worldPosition)
                .getBlock() instanceof BearingBlock))
            return;

        Direction direction = getBlockState().getValue(FACING);
        BearingContraption contraption = new BearingContraption(isWindmill(), direction);
        try {
            if (isPropeller()) ((BearingContraptionExtension) contraption).setPropeller();
            if (!contraption.assemble(level, worldPosition))
                return;

            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        if (isWindmill())
            AllTriggers.triggerForNearbyPlayers(AllTriggers.WINDMILL, level, worldPosition, 5);
        if (contraption.getSailBlocks() >= 16 * 8)
            AllTriggers.triggerForNearbyPlayers(AllTriggers.MAXED_WINDMILL, level, worldPosition, 5);

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = GyroscopicControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        running = true;
        angle = 0;
        sendData();
        updateGeneratedRotation();
        rotationSpeed=0;
        contraptionInitialize();
    }
    public void tick() {
        super.tick();

        Direction facing = level.getBlockState(worldPosition).getValue(BlockStateProperties.FACING);
        blockNormal = new Vector3d(facing.getStepX(),facing.getStepY(),facing.getStepZ());

        if(movedContraption==null)
            return;
        ((GyroscopicControlledContraptionEntity)movedContraption).tiltQuat=new Quaternionf(tiltQuat);
        ((GyroscopicControlledContraptionEntity)movedContraption).direction=getBlockState().getValue(FACING);
    }
    @Override
    public void write(CompoundNBT compound, boolean clientPacket) {
        compound.putBoolean("IsPowered", powered);

        super.write(compound, clientPacket);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        powered = compound.getBoolean("IsPowered");
        super.fromTag(state, compound, clientPacket);
    }
    public void updateSignal() {
        powered = this.level.hasNeighborSignal(this.worldPosition);
    }
    public Vector3d getForce(BlockPos localPos, double airPressure, Vector3d velocity, AbstractContraptionRigidbody rigidbody)
    {
        Vector3d globalTarget = new Vector3d(0,-getDirectionScale(),0);
        setTilt(rigidbody.rotateInverse(globalTarget));
        return super.getForce(localPos,airPressure,velocity,rigidbody);
    }
    public void setTilt(Vector3d target)
    {

        if(powered)
            target=blockNormal;
        targetVector=target;
        Direction direction = getBlockState().getValue(FACING);
        blockNormal = new Vector3d(direction.getStepX(),direction.getStepY(),direction.getStepZ());


        target = MathUtils.clampIntoCone(target,blockNormal,Math.toRadians(12));
        float lerpDistance = 1f;
        double currentThrust = Math.abs(getThrust());
        if(currentThrust<0.5)
            lerpDistance*=currentThrust/0.5;
        if(disassemblySlowdown)
        {
            lerpDistance*=disassemblyTimer/disassemblyTimerTotal;
        }
        target=VecHelper.lerp(lerpDistance,blockNormal,target);

        double maxStep = 0.02;
        Vector3d difference = target.subtract(tiltVector);
        if(difference.lengthSqr()>maxStep*maxStep)
        {
            tiltVector = tiltVector.add(difference.normalize().scale(maxStep));
        }else
            tiltVector=target;
        tiltVector = tiltVector.normalize();

        tiltQuat=MathUtils.getQuaternionFromVectorRotation(blockNormal,tiltVector).toMojangQuaternion();

        thrustDirection=tiltVector;
    }
    @Override
    public boolean isPropeller() {
        return true;
    }

}
