package com.eriksonn.createaeronautics.contraptions;

import com.eriksonn.createaeronautics.blocks.airship_assembler.AirshipAssemblerTileEntity;
import com.eriksonn.createaeronautics.dimension.AirshipDimensionManager;
import com.eriksonn.createaeronautics.index.CAEntityTypes;
import com.eriksonn.createaeronautics.mixins.ContraptionHolderAccessor;
import com.eriksonn.createaeronautics.mixins.ItemUseContextMixin;
import com.eriksonn.createaeronautics.mixins.SUpdateTileEntityPacketMixin;
import com.eriksonn.createaeronautics.network.NetworkMain;
import com.eriksonn.createaeronautics.network.packet.*;
import com.eriksonn.createaeronautics.physics.SimulatedContraptionRigidbody;
import com.eriksonn.createaeronautics.physics.api.ContraptionEntityPhysicsAdapter;
import com.eriksonn.createaeronautics.utils.AbstractContraptionEntityExtension;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.eriksonn.createaeronautics.utils.Matrix3dExtension;
import com.eriksonn.createaeronautics.utils.Transform;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.components.structureMovement.*;
import com.simibubi.create.content.curiosities.tools.ExtendoGripItem;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;


public class AirshipContraptionEntity extends AbstractContraptionEntity {

    public ClientAirshipEntityManager helper = level.isClientSide ? new ClientAirshipEntityManager(this) : null;
    float time = 0;

    public boolean playPhysics = true;
    public Quaternionf quat = Quaternionf.ONE;
    public Vector3d velocity;
    public AirshipContraption airshipContraption;
    public int plotId = -1;
    public SimulatedContraptionRigidbody simulatedRigidbody;

    public Transform renderTransform = new Transform(Vector3d.ZERO, Quaternionf.ONE);
    public Transform previousRenderTransform = renderTransform;
    public Transform smoothedRenderTransform = renderTransform;

    public Map<UUID, ControlledContraptionEntity> subContraptions = new HashMap<>();
    public Vector3d centerOfMassOffset = Vector3d.ZERO;

    public AirshipContraptionEntity(EntityType<?> type, World world) {
        super(type, world);
        simulatedRigidbody = new SimulatedContraptionRigidbody((AirshipContraption) getContraption(), new ContraptionEntityPhysicsAdapter(this));
        // testing
//        this.simulatedRigidbody.angularMomentum = new Vector3d(0, 0, 40);

        airshipContraption = (AirshipContraption) contraption;
        System.out.println("New airship entity");
    }

    public static AirshipContraptionEntity create(World world, AirshipContraption contraption) {
        AirshipContraptionEntity entity = new AirshipContraptionEntity((EntityType) CAEntityTypes.AIRSHIP_CONTRAPTION.get(), world);
        entity.setContraption(contraption);
        entity.airshipContraption = contraption;
        entity.simulatedRigidbody = new SimulatedContraptionRigidbody((AirshipContraption) entity.airshipContraption, new ContraptionEntityPhysicsAdapter(entity));
        int id = AirshipManager.INSTANCE.getNextId();
        AirshipManager.INSTANCE.tryAddEntity(id, entity);

        entity.plotId = id;
        entity.simulatedRigidbody.tryInit();
        return entity;

    }

    // Whether or not this contraption has been airshipInitialized
    public boolean airshipInitialized = false;

    public boolean syncNextTick = false;

    HashSet<BlockPos> blocksToUpdate = new HashSet<>();

    ContraptionSmoother smoother = new ContraptionSmoother(this, 0.5);

    @Override
    public void tickContraption() {
        contraption.getActors().clear();
        airshipContraption = (AirshipContraption) contraption;
        simulatedRigidbody.contraption = airshipContraption;
        AirshipAssemblerTileEntity controller = getController();
        airshipContraption = (AirshipContraption) contraption;

        if (controller != null)
            controller.attach(this);

        if(playPhysics) {
            if (level.isClientSide) {
                simulatedRigidbody.tryInit();
            } else {
                simulatedRigidbody.tick();
                this.centerOfMassOffset = simulatedRigidbody.getCenterOfMass();
                quat = simulatedRigidbody.orientation.copy();
                double deltaTime = 0.05;
                velocity = simulatedRigidbody.globalVelocity.scale(deltaTime);
                move(simulatedRigidbody.globalVelocity.x * deltaTime, simulatedRigidbody.globalVelocity.y * deltaTime, simulatedRigidbody.globalVelocity.z * deltaTime);
            }
        }


        // smoothy stuff woo
        smoother.latestPosition = position();
        smoother.latestOrientation = quat;
        smoother.tick();
        previousRenderTransform = renderTransform;
        renderTransform = new Transform(smoother.smoothedPosition, smoother.smoothedOrientation);


        if (!airshipInitialized) {
            if(helper != null) helper.initFakeClientWorld();

            renderTransform = new Transform(position(), quat);
            previousRenderTransform = renderTransform;
            smoother.smoothedPosition = renderTransform.position;
            smoother.smoothedOrientation = renderTransform.orientation;
        }


        if(!level.isClientSide) {
            smoothedRenderTransform = renderTransform;
        }

        if (level.isClientSide) {
            if(helper != null)
                helper.tick();
        }

        if (!airshipInitialized) {
            airshipInitialized = true;
            syncNextTick = true;
        }

        if (syncNextTick) {
            syncPacket();
            syncPacket();
            syncNextTick = false;
        }

        contraption.getContraptionWorld().tickBlockEntities();

        if (!level.isClientSide) {
            serverUpdate();
        }
    }

    public BlockPos getPlotPos() {
        return AirshipManager.getPlotPosFromId(plotId);
    }



    private void putDoubleArray(CompoundNBT tag, String key, double[] array) {
        ListNBT list = new ListNBT();
        for (double d : array) {
            list.add(DoubleNBT.valueOf(d));
        }
        tag.put(key, list);
    }

    private double[] readDoubleArray(CompoundNBT tag, String key) {
        INBT[] boxed = tag.getList(key, Constants.NBT.TAG_DOUBLE).toArray(new INBT[0]);
        double[] unboxed = new double[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            unboxed[i] = ((DoubleNBT) boxed[i]).getAsDouble();
        }
        return unboxed;
    }



    public void serverUpdate() {
        // stcDestroySubContraption and remove from the hashmap all subcontraptions that arent alive
        Set<UUID> keyset = new HashSet<>(subContraptions.keySet());

        for (UUID uuid : keyset) {
            ControlledContraptionEntity subContraption = subContraptions.get(uuid);
            if (!subContraption.isAlive()) {
                subContraptions.remove(uuid);
                simulatedRigidbody.removeSubContraption(uuid);
                serverDestroySubContraption(subContraption);
            } else {
                serverUpdateSubContraption(subContraption);
            }
        }

        // for everything in the hashmap, update the client
        for (BlockPos pos : blocksToUpdate) {
            stcHandleBlockUpdate(pos);
        }

        blocksToUpdate.clear();

        notifyClients(new PhysicsUpdatePacket(
                plotId,
                simulatedRigidbody.centerOfMass,
                simulatedRigidbody.momentum,
                simulatedRigidbody.angularMomentum,
                simulatedRigidbody.angularVelocity,
                simulatedRigidbody.orientation
        ));
    }

    private void serverDestroySubContraption(ControlledContraptionEntity subContraption) {
        notifyClients(new AirshipDestroySubcontraptionPacket(plotId, subContraption.getUUID()));
    }

    public void serverUpdateSubContraption(ControlledContraptionEntity subContraption) {
        notifyClients(new AirshipUpdateSubcontraptionPacket(plotId, subContraption.serializeNBT(), subContraption.getUUID()));
    }



    public void syncPacket() {
        if (!level.isClientSide) {

            // plot pos
            BlockPos plotPos = getPlotPos();

            // for every block
            for (Map.Entry<BlockPos, Template.BlockInfo> blockStateEntry : contraption.getBlocks().entrySet()) {
                int x = blockStateEntry.getKey().getX() + plotPos.getX();
                int y = blockStateEntry.getKey().getY() + plotPos.getY();
                int z = blockStateEntry.getKey().getZ() + plotPos.getZ();

                BlockPos pos = new BlockPos(x, y, z);
                ServerWorld serverLevel = AirshipDimensionManager.INSTANCE.getWorld();

                BlockState state = serverLevel.getBlockState(pos.offset(getPlotPos()));
                if (!state.getBlock().is(Blocks.AIR)) {
                    TileEntity te = serverLevel.getBlockEntity(pos.offset(getPlotPos()));
                    if (te instanceof ITickableTileEntity) {
                        ((ITickableTileEntity) te).tick();
                    }
                    stcQueueBlockUpdate(pos);
                }
            }
        }
    }

    public void stcQueueBlockUpdate(BlockPos localPos) {
        blocksToUpdate.add(localPos);
    }

    public void stcHandleBlockUpdate(BlockPos localPos) {

        if (!airshipInitialized) return;
        BlockPos plotPos = getPlotPos();

        // Server level!
        ServerWorld serverLevel = AirshipDimensionManager.INSTANCE.getWorld();

        // get block state
        BlockPos pos = plotPos.offset(localPos);
        BlockState state = serverLevel.getBlockState(pos);

        CompoundNBT thisBlockNBT = new CompoundNBT();

        thisBlockNBT.putInt("x", pos.getX() - plotPos.getX());
        thisBlockNBT.putInt("y", pos.getY());
        thisBlockNBT.putInt("z", pos.getZ() - plotPos.getZ());
        thisBlockNBT.put("state", NBTUtil.writeBlockState(state));

        TileEntity blockEntity = state.hasTileEntity() ? serverLevel.getBlockEntity(pos) : null;
        SUpdateTileEntityPacket updatePacket = null;
        if (blockEntity != null) {
            thisBlockNBT.put("be", blockEntity.serializeNBT());
            updatePacket = blockEntity.getUpdatePacket();

            addTileData(blockEntity, pos.offset(-plotPos.getX(), -plotPos.getY(), -plotPos.getZ()), state);
            handleControllingSubcontraption(blockEntity, pos);
        }

        thisBlockNBT.putInt("plotId", plotId);

        AirshipContraptionBlockUpdatePacket packet = new AirshipContraptionBlockUpdatePacket(thisBlockNBT);
        notifyClients(packet);
        SUpdateTileEntityPacketMixin mixinUpdatePacket = (SUpdateTileEntityPacketMixin) updatePacket;
        if(mixinUpdatePacket != null) {
            notifyClients(new AirshipBEUpdatePacket(mixinUpdatePacket.getType(), mixinUpdatePacket.getTag(), new BlockPos(pos.getX() - plotPos.getX(), pos.getY(),pos.getZ() - plotPos.getZ()), plotId));
        }

        airshipContraption.setBlockState(localPos, state, blockEntity);
    }

    private void notifyClients(Object packet) {
        NetworkMain.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(
                new BlockPos(position())
        )), packet);
    }

    public AirshipAssemblerTileEntity getController() {
        if(level.isClientSide) return null;
        BlockPos controllerPos = getPlotPos();
        World w = AirshipDimensionManager.INSTANCE.getWorld();
        if (!w.isLoaded(controllerPos))
            return null;
        TileEntity te = w.getBlockEntity(controllerPos);
        if (!(te instanceof AirshipAssemblerTileEntity))
            return null;
        return (AirshipAssemblerTileEntity) te;
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        readAdditional(additionalData.readNbt(), true);
    }

    @Override
    protected void readAdditional(CompoundNBT compound, boolean spawnPacket) {
        super.readAdditional(compound, spawnPacket);
        int previousPlotID = plotId;
        plotId = compound.getInt("PlotId");
        if(previousPlotID == -1) {
            AirshipManager.INSTANCE.tryAddEntity(plotId, this);
        }
        simulatedRigidbody.globalVelocity = simulatedRigidbody.arrayToVec(readDoubleArray(compound, "velocity"));
        //simulatedRigidbody.angularVelocity = simulatedRigidbody.arrayToVec(readDoubleArray(compound, "angularVelocity"));
        simulatedRigidbody.orientation = simulatedRigidbody.arrayToQuat(readDoubleArray(compound, "orientation"));
    }

    @Override
    protected void writeAdditional(CompoundNBT compound, boolean spawnPacket) {
        super.writeAdditional(compound, spawnPacket);
        compound.putInt("PlotId", plotId);
        putDoubleArray(compound, "velocity", simulatedRigidbody.vecToArray(simulatedRigidbody.globalVelocity));
        //putDoubleArray(compound, "angularVelocity", simulatedRigidbody.vecToArray(simulatedRigidbody.angularVelocity));
        putDoubleArray(compound, "orientation", simulatedRigidbody.quatToArray(simulatedRigidbody.orientation));
    }

    @Override
    public AirshipRotationState getRotationState() {
        AirshipRotationState crs = new AirshipRotationState();
        crs.matrix = new Matrix3d();
        Vector3d I = MathUtils.rotateQuatReverse(new Vector3d(1, 0, 0), quat);
        Vector3d J = MathUtils.rotateQuatReverse(new Vector3d(0, 1, 0), quat);
        Vector3d K = MathUtils.rotateQuatReverse(new Vector3d(0, 0, 1), quat);
        ((Matrix3dExtension) crs.matrix).createaeronautics$set(I, J, K);
        crs.matrix.transpose();
        return crs;
    }

    public Vector3d reverseRotation(Vector3d localPos, float partialTicks) {
        return MathUtils.rotateQuatReverse(localPos, simulatedRigidbody.getPartialOrientation(partialTicks));
    }

    public Vector3d applyRotation(Vector3d localPos, float partialTicks) {
        return MathUtils.rotateQuat(localPos, simulatedRigidbody.getPartialOrientation(partialTicks));
    }

    public Vector3d toGlobalVector(Vector3d localVec, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, xOld, getX());
        double y = MathHelper.lerp(partialTicks, yOld, getY());
        double z = MathHelper.lerp(partialTicks, zOld, getZ());
        Vector3d anchorVec = new Vector3d(x, y, z);

        Vector3d rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        localVec = localVec.subtract(rotationOffset).subtract(centerOfMassOffset);
        //localVec = localVec.subtract(rotationOffset);
        localVec = applyRotation(localVec, partialTicks);
        localVec = localVec.add(rotationOffset)
                .add(anchorVec);
        return localVec;
    }

    public Vector3d toLocalVector(Vector3d globalVec, float partialTicks) {
        Vector3d rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        globalVec = globalVec.subtract(getAnchorVec())
                .subtract(rotationOffset);
        globalVec = reverseRotation(globalVec, partialTicks);
        globalVec = globalVec.add(rotationOffset);
        //return globalVec;
        return globalVec.add(centerOfMassOffset);
    }

    public Vector3d toLocalVectorSmoothed(Vector3d globalVec, float partialTick) {
        Vector3d rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        globalVec = globalVec.subtract(smoothedRenderTransform.position)
                .subtract(rotationOffset);
        globalVec = MathUtils.rotateQuatReverse(globalVec, smoothedRenderTransform.orientation);
        globalVec = globalVec.add(rotationOffset);
        //return globalVec;
        return globalVec.add(centerOfMassOffset);
    }

    protected StructureTransform makeStructureTransform() {
        BlockPos offset = new BlockPos(this.getAnchorVec().subtract(centerOfMassOffset)).offset(0,1,0);
        return new StructureTransform(offset, 0.0F, 0, 0.0F);
    }

    @Override
    public Vector3d getAnchorVec() {
        return position();
    }

    protected float getStalledAngle() {
        return 0.0f;
    }

    protected void handleStallInformation(float x, float y, float z, float angle) {

    }

    public boolean handlePlayerInteraction2(PlayerEntity player, BlockPos localPos, Direction side,
                                            Hand interactionHand) {
        return true;
    }

    public boolean handlePlayerInteraction(PlayerEntity player, BlockPos localPos, Direction side,
                                           Hand interactionHand) {


        if(player.getItemInHand(interactionHand).getItem() instanceof ExtendoGripItem){
            simulatedRigidbody.addGlobalForce(player.getLookAngle().scale(3000.0), new Vector3d(localPos.getX(), localPos.getY(), localPos.getZ()));
            return true;
        }

        int indexOfSeat = contraption.getSeats()
                .indexOf(localPos);
        if (indexOfSeat == -1 && player instanceof ServerPlayerEntity) {
            BlockPos dimensionPos = localPos.offset(getPlotPos());
            World worldIn = AirshipDimensionManager.INSTANCE.getWorld();
            BlockState state = worldIn.getBlockState(dimensionPos);
            if(localPos.equals(BlockPos.ZERO) && state.isAir())
            {
                disassemble();
            }
            try {
                BlockRayTraceResult pResult = new BlockRayTraceResult(
                        Vector3d.atBottomCenterOf(dimensionPos), side, dimensionPos, false
                );
                if(state.use(worldIn, player, interactionHand, pResult).consumesAction()) return true;
                ItemStack itemstack = player.getItemInHand(interactionHand);


                if(true) return false;

                // TODO : Fix this lmaoo
                if (itemstack.isEmpty() || player.getCooldowns().isOnCooldown(itemstack.getItem())) return false;

                ItemUseContext itemusecontext = new ItemUseContext(player, interactionHand, pResult);
                ((ItemUseContextMixin) itemusecontext).setLevel(AirshipDimensionManager.INSTANCE.getWorld());
                ActionResultType actionresulttype1 = null;
//                if (Minecraft.getInstance().player.isCreative()) {
//                    int i = itemstack.getCount();
//                    actionresulttype1 = itemstack.useOn(itemusecontext);
//                    itemstack.setCount(i);
//                } else {
//                    actionresulttype1 = itemstack.useOn(itemusecontext);
//                }

//                return actionresulttype1.consumesAction();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        // Eject potential existing passenger
        Entity toDismount = null;
        for (Map.Entry<UUID, Integer> entry : contraption.getSeatMapping()
                .entrySet()) {
            if (entry.getValue() != indexOfSeat)
                continue;
            for (Entity entity : getPassengers()) {
                if (!entry.getKey()
                        .equals(entity.getUUID()))
                    continue;
                if (entity instanceof PlayerEntity)
                    return false;
                toDismount = entity;
            }
        }

        if (toDismount != null && !level.isClientSide) {
            Vector3d transformedVector = getPassengerPosition(toDismount, 1);
            toDismount.stopRiding();
            if (transformedVector != null)
                toDismount.teleportTo(transformedVector.x, transformedVector.y, transformedVector.z);
        }

        if (level.isClientSide)
            return true;
        addSittingPassenger(player, indexOfSeat);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void doLocalTransforms(float partialTicks, MatrixStack[] matrixStacks) {
        float angleInitialYaw = 0.0f;
        MatrixStack[] var6 = matrixStacks;
        int var7 = matrixStacks.length;

        int var8;
        Quaternionf Q = smoothedRenderTransform.orientation;
        Vector3d partialPosition = getPartialPosition(partialTicks);
        Vector3d position = position();
        Q.conj();
        for (var8 = 0; var8 < var7; ++var8) {
            MatrixStack stack = var6[var8];
            stack.translate(-position.x, -position.y, -position.z);
            stack.translate(0.5, 0.5, 0.5);
            stack.translate(position.x - partialPosition.x, position.y - partialPosition.y, position.z - partialPosition.z);
            stack.translate(smoothedRenderTransform.position.x, smoothedRenderTransform.position.y, smoothedRenderTransform.position.z);

            stack.mulPose(Q.toMojangQuaternion());
            stack.translate(-centerOfMassOffset.x, -centerOfMassOffset.y, -centerOfMassOffset.z);
            stack.translate(-0.5, -0.5, -0.5);


            //stack.translate(-0.5D, 0.0D, -0.5D);
        }

        MatrixStack[] var12 = matrixStacks;
        var8 = matrixStacks.length;
        //Quaternion conj = currentQuaternion.copy();
        //conj.conj();
        for (int var13 = 0; var13 < var8; ++var13) {
            MatrixStack stack = var12[var13];

            //MatrixTransformStack.of(stack).nudge(this.getId()).centre().rotateY((double)angleYaw).rotateZ((double)anglePitch).rotateY((double)angleInitialYaw).multiply(CurrentAxis,Math.toDegrees(CurrentAxisAngle)).unCentre();
            MatrixTransformStack.of(stack).nudge(this.getId()).centre().unCentre();
        }

    }


    public void addTileData(TileEntity te, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        MovementBehaviour movementBehaviour = AllMovementBehaviours.of(block);

        if (te == null)
            return;

        te.getBlockState();

        if (movementBehaviour == null || !movementBehaviour.hasSpecialInstancedRendering()) {
            if (!airshipContraption.maybeInstancedTileEntities.contains(te)) {
                for (int i = 0; i < airshipContraption.maybeInstancedTileEntities.size(); i++) {
                    if (airshipContraption.maybeInstancedTileEntities.get(i).getBlockPos().offset(0, -getPlotPos().getY(), 0).equals(pos)) {
                        airshipContraption.maybeInstancedTileEntities.remove(i);
                        i--;
                    }
                }
                airshipContraption.maybeInstancedTileEntities.add(te);
            }
        }

        airshipContraption.presentTileEntities.put(pos, te);
        if (!airshipContraption.specialRenderedTileEntities.contains(te)) {
            for (int i = 0; i < airshipContraption.specialRenderedTileEntities.size(); i++) {
                if (airshipContraption.specialRenderedTileEntities.get(i).getBlockPos().offset(0, -getPlotPos().getY(), 0).equals(pos)) {
                    airshipContraption.specialRenderedTileEntities.remove(i);
                    i--;
                }
            }
            airshipContraption.specialRenderedTileEntities.add(te);
        }
    }

    public void handleControllingSubcontraption(TileEntity be, BlockPos pos) {

        if (!(be instanceof IControlContraption)) return;

        IControlContraption controllingContraption = (IControlContraption) be;

        if (controllingContraption instanceof ContraptionHolderAccessor) {
            ControlledContraptionEntity contraptionEntity = ((ContraptionHolderAccessor) be).getMovedContraption();

            if (contraptionEntity != null) {
                if (!subContraptions.containsKey(contraptionEntity.getUUID())) {
                    stcSubContraptionAddition(contraptionEntity, pos, contraptionEntity.getUUID());
                }

                subContraptions.put(contraptionEntity.getUUID(), contraptionEntity);
                simulatedRigidbody.addSubContraption(contraptionEntity.getUUID(), contraptionEntity);
                ((AbstractContraptionEntityExtension) contraptionEntity).createAeronautics$setOriginalPosition(contraptionEntity.position());
            }
        }
    }

    private void stcSubContraptionAddition(ControlledContraptionEntity contraptionEntity, BlockPos pos, UUID uuid) {
        notifyClients(new AirshipAddSubcontraptionPacket(plotId, contraptionEntity.serializeNBT(), pos, uuid));
    }

    public Vector3d getPartialPosition(float partialTicks) {
        double x = MathHelper.lerp(partialTicks, xOld, getX());
        double y = MathHelper.lerp(partialTicks, yOld, getY());
        double z = MathHelper.lerp(partialTicks, zOld, getZ());
        Vector3d anchorVec = new Vector3d(x, y, z);

//        Vector3d anchorVec = position().add(physicsManager.globalVelocity.scale(partialTicks).scale(0.05));
        return anchorVec;
    }



    public static class AirshipRotationState extends ContraptionRotationState {
        public static final ContraptionRotationState NONE = new ContraptionRotationState();

        float xRotation = 0;
        float yRotation = 0;
        float zRotation = 0;
        float secondYRotation = 0;
        Matrix3d matrix;

        public Matrix3d asMatrix() {
            if (matrix != null)
                return matrix;

            matrix = new Matrix3d().asIdentity();
            if (xRotation != 0)
                matrix.multiply(new Matrix3d().asXRotation(AngleHelper.rad(-xRotation)));
            if (yRotation != 0)
                matrix.multiply(new Matrix3d().asYRotation(AngleHelper.rad(yRotation)));
            if (zRotation != 0)
                matrix.multiply(new Matrix3d().asZRotation(AngleHelper.rad(-zRotation)));
            return matrix;
        }

        public boolean hasVerticalRotation() {
            return true;
        }

        public float getYawOffset() {
            return secondYRotation;
        }
    }

    @Override
    public Vector3d getContactPointMotion(Vector3d globalContactPoint) {
        return simulatedRigidbody.getVelocityAtPoint(toLocalVector(globalContactPoint,0.0f).subtract(0.5, 0.5, 0.5)).scale(0.05f);
    }

    @Override
    public Vector3d getPassengerPosition(Entity passenger, float partialTicks) {

        UUID id = passenger.getUUID();
        if (passenger instanceof OrientedContraptionEntity) {
            BlockPos localPos = contraption.getBearingPosOf(id);
            if (localPos != null)
                return toGlobalVector(VecHelper.getCenterOf(localPos), partialTicks)
                        .add(VecHelper.getCenterOf(BlockPos.ZERO))
                        .subtract(.5f, 1, .5f);
        }

        AxisAlignedBB bb = passenger.getBoundingBox();
        double ySize = bb.getYsize();
        BlockPos seat = contraption.getSeatOf(id);
        if (seat == null)
            return null;

        return smoothedRenderTransform.position.add(MathUtils.rotateQuat(
                Vector3d.atLowerCornerOf(seat).add(new Vector3d(0.0, passenger.getMyRidingOffset() + ySize - .15f, 0.0)).subtract(centerOfMassOffset),
                smoothedRenderTransform.orientation)).add(0.5, 0.5, 0.5).subtract(new Vector3d(0.0, ySize, 0.0));

//        Vector3d transformedVector = local.add(VecHelper.getCenterOf(BlockPos.ZERO))
//                .subtract(0.5, ySize, 0.5);
    }

    @Override
    public void onRemovedFromWorld() {
        if(level.isClientSide) AirshipManager.INSTANCE.AllClientAirships.remove(plotId);
        super.onRemovedFromWorld();
    }
}