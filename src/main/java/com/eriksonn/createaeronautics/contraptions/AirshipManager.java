package com.eriksonn.createaeronautics.contraptions;

import appeng.me.helpers.GenericInterestManager;
import com.eriksonn.createaeronautics.CreateAeronautics;
import com.eriksonn.createaeronautics.dimension.AirshipDimensionManager;
import com.eriksonn.createaeronautics.mixins.ContraptionHolderAccessor;
import com.eriksonn.createaeronautics.physics.SimulatedContraptionRigidbody;
import com.eriksonn.createaeronautics.utils.AbstractContraptionEntityExtension;
import com.eriksonn.createaeronautics.utils.Transform;
import com.eriksonn.createaeronautics.world.FakeAirshipClientWorld;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.components.structureMovement.*;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.SailBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.components.turntable.TurntableTileEntity;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import org.apache.logging.log4j.core.jmx.Server;


import java.lang.reflect.Field;
import java.util.*;

public class AirshipManager {
    public static final AirshipManager INSTANCE = new AirshipManager();

    public Map<Integer,AirshipContraptionEntity> AllAirships;
    public Map<Integer, AirshipContraptionData> AirshipData;
    public Map<Integer,AirshipContraptionEntity> AllClientAirships = new HashMap<>();

    class AirshipContraptionData
    {
        public boolean needsUpdating=false;
        public List<AbstractContraptionEntity> addedContraptions=new ArrayList<>();
        public Map<BlockPos,BlockState> ClientBlockStateChanges=new HashMap<>();
        public Map<BlockPos,TileEntity> presentTileEntities =new HashMap<>();
        public List<TileEntity> specialRenderedTileEntitiesChanges=new ArrayList<>();
        public List<TileEntity> maybeInstancedTileEntitiesChanges=new ArrayList<>();
        public Map<BlockPos,BlockState> sails=new HashMap<>();
        public Map<BlockPos,ControlledContraptionEntity> subContraptions = new HashMap<>();
        public int removeTimer=0;//need to wait abit to prevent client render crash
    }
    AirshipManager()
    {
        AllAirships=new HashMap<>();
        AirshipData =new HashMap<>();
        MinecraftForge.EVENT_BUS.register(AirshipEventHandler.class);
    }
    public void tryAddEntity(int index ,AirshipContraptionEntity E)
    {
        AllAirships.putIfAbsent(index,E);
        AirshipData.putIfAbsent(index,new AirshipContraptionData());
    }
    public void tick() {
        int plotToRemove=-1;
        for (Map.Entry<Integer,AirshipContraptionEntity> entry: AllAirships.entrySet())
        {
            ServerWorld world = AirshipDimensionManager.INSTANCE.getWorld();
            AirshipContraptionEntity entity=entry.getValue();
            if(!entity.isAlive())
            {
                plotToRemove=entry.getKey();
            }
            AirshipContraptionData data = AirshipData.get(entry.getKey());
            BlockPos pos=getPlotPosFromId(entry.getKey());
            ChunkPos chunkPos = new ChunkPos(pos);
            if(entity.level.isLoaded(entity.blockPosition()))
            {
                ForgeChunkManager.forceChunk(world, CreateAeronautics.MODID, pos, chunkPos.x, chunkPos.z, true, true);
            }else
            {
                ForgeChunkManager.forceChunk(world, CreateAeronautics.MODID, pos, chunkPos.x, chunkPos.z, false, true);
            }
        }
        if(plotToRemove!=-1) {
            int a = AirshipData.get(plotToRemove).removeTimer++;
            if(a>5)
                removePlot(plotToRemove);
        }

    }
    public void removePlot(int id)
    {
        AirshipContraptionEntity entity = AllAirships.get(id);
        if(entity!=null) {
            System.out.println("plot remove");
            ServerWorld world = AirshipDimensionManager.INSTANCE.getWorld();
            BlockPos anchor = getPlotPosFromId(id);
            ChunkPos chunkPos = new ChunkPos(anchor);
            removeBlocksFromWorld(world, anchor, entity.airshipContraption.getBlocks());
            ForgeChunkManager.forceChunk(world, CreateAeronautics.MODID, anchor, chunkPos.x, chunkPos.z, false, true);
            AllAirships.remove(id);
            AirshipData.remove(id);
        }
    }
    public void removeBlocksFromWorld(World world, BlockPos anchor,Map<BlockPos, Template.BlockInfo> blocks) {
        //storage.values()
        //        .forEach(MountedStorage::removeStorageFromWorld);
        //fluidStorage.values()
        //        .forEach(MountedFluidStorage::removeStorageFromWorld);
        //glueToRemove.forEach(SuperGlueEntity::remove);

        for (boolean brittles : Iterate.trueAndFalse) {
            for (Iterator<Template.BlockInfo> iterator = blocks.values()
                    .iterator(); iterator.hasNext();) {
                Template.BlockInfo block = iterator.next();
                if (brittles != BlockMovementChecks.isBrittle(block.state))
                    continue;

                BlockPos add = block.pos.offset(anchor);
                BlockState oldState = world.getBlockState(add);
                Block blockIn = oldState.getBlock();
                if (block.state.getBlock() != blockIn)
                    iterator.remove();
                world.removeBlockEntity(add);
                int flags = Constants.BlockFlags.IS_MOVING | Constants.BlockFlags.NO_NEIGHBOR_DROPS | Constants.BlockFlags.UPDATE_NEIGHBORS
                        | Constants.BlockFlags.BLOCK_UPDATE | Constants.BlockFlags.RERENDER_MAIN_THREAD;
                if (blockIn instanceof IWaterLoggable && oldState.hasProperty(BlockStateProperties.WATERLOGGED)
                        && oldState.getValue(BlockStateProperties.WATERLOGGED)) {
                    world.setBlock(add, Blocks.WATER.defaultBlockState(), flags);
                    continue;
                }
                world.setBlock(add, Blocks.AIR.defaultBlockState(), flags);
            }
        }
        for (Template.BlockInfo block : blocks.values()) {
            BlockPos add = block.pos.offset(anchor);
//			if (!shouldUpdateAfterMovement(block))
//				continue;

            int flags = Constants.BlockFlags.IS_MOVING | Constants.BlockFlags.DEFAULT;
            world.sendBlockUpdated(add, block.state, Blocks.AIR.defaultBlockState(), flags);

            // when the blockstate is set to air, the block's POI data is removed, but markAndNotifyBlock tries to
            // remove it again, so to prevent an error from being logged by double-removal we add the POI data back now
            // (code copied from ServerWorld.onBlockStateChange)
            ServerWorld serverWorld = (ServerWorld) world;
            PointOfInterestType.forState(block.state)
                    .ifPresent(poiType -> {
                        world.getServer()
                                .execute(() -> {
                                    serverWorld.getPoiManager()
                                            .add(add, poiType);
                                    DebugPacketSender.sendPoiAddedPacket(serverWorld, add);
                                });
                    });

            world.markAndNotifyBlock(add, world.getChunkAt(add), block.state, Blocks.AIR.defaultBlockState(), flags,
                    512);
            block.state.updateIndirectNeighbourShapes(world, add, flags & -2);
        }
    }
    public AirshipOrientedInfo getInfo(World world, BlockPos pos) {

        Vector3d vecPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, 0.5, 0.5);
        if(world instanceof FakeAirshipClientWorld) {
            FakeAirshipClientWorld fakeWorld = (FakeAirshipClientWorld) world;
            AirshipContraptionEntity airship = fakeWorld.airship;

            return new AirshipOrientedInfo(airship,airship.toGlobalVector(vecPos.subtract(0, airship.getPlotPos().getY(), 0), 0f), airship.quat, airship.level, true);
        } else if (world == AirshipDimensionManager.INSTANCE.getWorld()) {
            int airshipID = getIdFromPlotPos(pos);

            AirshipContraptionEntity airship = AllAirships.get(airshipID);

            BlockPos plotpos = getPlotPosFromId(airshipID);
            return new AirshipOrientedInfo(airship,airship.toGlobalVector(vecPos.subtract(plotpos.getX(), plotpos.getY(), plotpos.getZ()), 0f), airship.quat, airship.level, true);
        } else {
            return new AirshipOrientedInfo(null,vecPos, Quaternion.ONE.copy(), world, false);
        }

    }
    @OnlyIn(Dist.CLIENT)
    protected void invalidate(Contraption contraption) {
        ContraptionRenderDispatcher.invalidate(contraption);
    }
    // TODO: fix all of this
    static final int PlotWidth=128;
    static final int PlotCenterHeight=64;
    public static BlockPos getPlotPosFromId(int id)
    {
        return new BlockPos(64, 64, 64);
    }
    public static int getIdFromPlotPos(BlockPos pos)
    {
        return 0;
    }
    private static int airshipID = 0;
    public int getNextId()
    {
        return 0;
    }
    public static class AirshipEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void tickEvent(TickEvent.ServerTickEvent e)
        {
            if(e.phase == TickEvent.Phase.START)
            {
                INSTANCE.tick();
            }
        }
        @SubscribeEvent
        public static void renderStartEvent(TickEvent.RenderTickEvent e)
        {
            float partialTicks = AnimationTickHolder.getPartialTicks();

            Minecraft mc = Minecraft.getInstance();
            if(mc.player == null) return;

            for (Map.Entry<Integer, AirshipContraptionEntity> entry : AirshipManager.INSTANCE.AllClientAirships.entrySet()) {
                AirshipContraptionEntity airship = entry.getValue();

                // if we're at the very start of a tick, no need for interpolation
                if(partialTicks == 0.0) airship.smoothedRenderTransform = airship.previousRenderTransform;

                // same for the end of the tick
                if(partialTicks == 1.0) airship.previousRenderTransform = airship.smoothedRenderTransform;


                Quaternion smoothieRotation = ContraptionSmoother.slerp(airship.previousRenderTransform.orientation, airship.renderTransform.orientation, partialTicks);
                Vector3d smoothiePos = ContraptionSmoother.lerp(airship.previousRenderTransform.position, airship.renderTransform.position, partialTicks);

                airship.smoothedRenderTransform = new Transform(smoothiePos, smoothieRotation);
                // entry.getValue().smoothedRenderTransform
            }

            BlockPos pos = mc.player.blockPosition();

            if (!mc.player.isOnGround())
                return;
            if (mc.isPaused())
                return;

            List<AirshipContraptionEntity> possibleContraptions = mc.level.getEntitiesOfClass(AirshipContraptionEntity.class, mc.player.getBoundingBox().inflate(10.0));

            for (AirshipContraptionEntity contraption : possibleContraptions) {
                if(contraption.collidingEntities.containsKey(mc.player)) {
                    float speed = (float) (contraption.simulatedRigidbody.rotate(contraption.simulatedRigidbody.getAngularVelocity()).y * (180.0 / Math.PI));
                    mc.player.yRot = mc.player.yRotO + speed * partialTicks * 0.05f;
                    mc.player.yBodyRot = mc.player.yRot;
                }
            }


        }
    }
    public class AirshipOrientedInfo {
        public AirshipContraptionEntity airship;
        public Vector3d position;
        public Quaternion orientation;
        public World level;
        public boolean onAirship;


        public AirshipOrientedInfo(AirshipContraptionEntity airship,Vector3d vector3d, Quaternion orientation, World world, boolean onAirship) {
            this.airship = airship;
            this.position = vector3d;
            this.orientation = orientation;
            this.level = world;
            this.onAirship = onAirship;
        }
    }
}
