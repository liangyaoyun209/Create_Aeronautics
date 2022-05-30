package com.eriksonn.createaeronautics.network;

import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.index.CAEntityTypes;
import com.eriksonn.createaeronautics.mixins.ControlledContraptionEntityMixin;
import com.eriksonn.createaeronautics.network.packet.*;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.StructureTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.CommandBlockTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientPacketHandler {
    public HashMap<Integer, AirshipContraptionEntity> cache = new HashMap<>();

    public static void handlePacket(AirshipContraptionBlockUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        Map<Integer, AirshipContraptionEntity> allAirships = AirshipManager.INSTANCE.AllClientAirships;

        AirshipContraptionBlockUpdateInfo info = msg.getInfo();

        if (allAirships.containsKey(info.airshipID)) {
            AirshipContraptionEntity airship = allAirships.get(info.airshipID);

            airship.helper.getWorld().setBlock(
                    info.pos,
                    info.state,
                    1
            );

            if (info.tileEntityNBT != null) {
                TileEntity existingBE = airship.helper.getWorld().getBlockEntity(info.pos);
                if(existingBE != null) {
                    airship.addTileData(existingBE, info.pos.offset(0, -airship.getPlotPos().getY(), 0), info.state);
                } else {
                    TileEntityType<?> type = ForgeRegistries.TILE_ENTITIES.getValue(new ResourceLocation(info.tileEntityNBT.getString("id")));
                    if (type == null) return;
                    TileEntity te = type.create();
                    if (te == null) return;

                    te.setLevelAndPosition(airship.helper.getWorld(), info.pos);
                    te.handleUpdateTag(info.state, info.tileEntityNBT);
                    te.load(info.state, info.tileEntityNBT);

                    airship.helper.getWorld().setBlockEntity(info.pos, te);
                    airship.addTileData(te, info.pos.offset(0, -airship.getPlotPos().getY(), 0), info.state);
                }
            }
        }
    }

    public static void handlePacket(AirshipAddSubcontraptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Map<Integer, AirshipContraptionEntity> allAirships = AirshipManager.INSTANCE.AllClientAirships;

        if (allAirships.containsKey(msg.plotID)) {
            AirshipContraptionEntity airship = allAirships.get(msg.plotID);

            addSubcontraptionClient(airship, msg.nbt, msg.uuid, msg.pos);
        }
    }

    public static void addSubcontraptionClient(AirshipContraptionEntity airship, CompoundNBT nbt, UUID uuid, BlockPos pos) {

        BlockPos plotPos = airship.getPlotPos();
        CompoundNBT controllerTag = nbt.getCompound("Controller");
        controllerTag.put("X", DoubleNBT.valueOf(controllerTag.getDouble("X") - plotPos.getX()));
        controllerTag.put("Z", DoubleNBT.valueOf(controllerTag.getDouble("Z") - plotPos.getZ()));

        Entity entity = EntityType.create(nbt, airship.helper.getWorld()).orElse(null);
        if (entity == null) return;

        ControlledContraptionEntity contraptionEntity = (ControlledContraptionEntity) entity;

        contraptionEntity.move(-plotPos.getX(), 0, -plotPos.getZ());
        ContraptionHandler.addSpawnedContraptionsToCollisionList(contraptionEntity, airship.level);

        airship.helper.getWorld().addFreshEntity(contraptionEntity);
        airship.subContraptions.put(uuid, contraptionEntity);
        airship.simulatedRigidbody.addSubContraption(uuid, contraptionEntity);
    }

    public static void handlePacket(AirshipUpdateSubcontraptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Map<Integer, AirshipContraptionEntity> allAirships = AirshipManager.INSTANCE.AllClientAirships;

        if (allAirships.containsKey(msg.plotID)) {
            AirshipContraptionEntity airship = allAirships.get(msg.plotID);

            UUID uuid = msg.uuid;
            CompoundNBT nbt = msg.nbt;
            ControlledContraptionEntity contraptionEntity = airship.subContraptions.get(uuid);
            if (contraptionEntity == null) {
                addSubcontraptionClient(airship, nbt, uuid, null);
                contraptionEntity = airship.subContraptions.get(uuid);
            }

            BlockPos plotPos = airship.getPlotPos();
            ListNBT posList = nbt.getList("Pos", Constants.NBT.TAG_DOUBLE);
            posList.set(0, DoubleNBT.valueOf(posList.getDouble(0) - plotPos.getX()));
            posList.set(2, DoubleNBT.valueOf(posList.getDouble(2) - plotPos.getZ()));

            CompoundNBT controllerTag = nbt.getCompound("Controller");
            controllerTag.put("X", DoubleNBT.valueOf(controllerTag.getDouble("X") - plotPos.getX()));
            controllerTag.put("Z", DoubleNBT.valueOf(controllerTag.getDouble("Z") - plotPos.getZ()));

            contraptionEntity.deserializeNBT(nbt);
        }
    }

    public static void handlePacket(AirshipDestroySubcontraptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Map<Integer, AirshipContraptionEntity> allAirships = AirshipManager.INSTANCE.AllClientAirships;

        if (allAirships.containsKey(msg.plotID)) {
            AirshipContraptionEntity airship = allAirships.get(msg.plotID);

            UUID uuid = msg.uuid;

            ControlledContraptionEntity contraptionEntity = airship.subContraptions.get(uuid);
            if (contraptionEntity == null) return;

            StructureTransform transform = ((ControlledContraptionEntityMixin) contraptionEntity).invokeMakeStructureTransform();

//        contraptionEntity.disassemble();
            contraptionEntity.remove();
            contraptionEntity.getContraption().addBlocksToWorld(airship.helper.fakeClientWorld, transform);
            airship.subContraptions.remove(uuid);
            airship.simulatedRigidbody.removeSubContraption(uuid);
        }
    }

    public static void handlePacket(AirshipBEUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        Map<Integer, AirshipContraptionEntity> allAirships = AirshipManager.INSTANCE.AllClientAirships;

        if (allAirships.containsKey(msg.airshipID)) {
            AirshipContraptionEntity airship = allAirships.get(msg.airshipID);

            TileEntity tileEntity = airship.helper.getWorld().getBlockEntity(msg.pos);
            if (tileEntity == null) return;

            tileEntity.handleUpdateTag(airship.helper.getWorld().getBlockState(msg.pos), msg.nbt);
            tileEntity.onDataPacket(ctx.get().getNetworkManager(), new SUpdateTileEntityPacket(msg.pos, msg.type, msg.nbt));
            tileEntity.setLevelAndPosition(airship.helper.getWorld(), msg.pos);
        }
    }
}
