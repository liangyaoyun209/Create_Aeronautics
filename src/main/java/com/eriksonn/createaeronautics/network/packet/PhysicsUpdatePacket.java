package com.eriksonn.createaeronautics.network.packet;

import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.network.ClientPacketHandler;
import com.eriksonn.createaeronautics.utils.MathUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PhysicsUpdatePacket {
    public Vector3d centerOfMass, momentum, angularMomentum, angularVelocity;
    public Quaternion orientation;
    public int airshipID;

    public PhysicsUpdatePacket(int airshipID, Vector3d centerOfMass, Vector3d momentum, Vector3d angularMomentum, Vector3d angularVelocity, Quaternion orientation) {
        this.airshipID = airshipID;
        this.centerOfMass = centerOfMass;
        this.momentum = momentum;
        this.angularMomentum = angularMomentum;
        this.angularVelocity = angularVelocity;
        this.orientation = orientation;
    }

    public void writeVector(PacketBuffer buffer, Vector3d vector) {
        buffer.writeDouble(vector.x());
        buffer.writeDouble(vector.y());
        buffer.writeDouble(vector.z());
    }

    public Vector3d readVector(PacketBuffer buffer) {
        return new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public Quaternion readQuaternion(PacketBuffer buffer) {
        return new Quaternion(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public void writeQuaternion(PacketBuffer buffer, Quaternion quat) {
        buffer.writeFloat(quat.i());
        buffer.writeFloat(quat.j());
        buffer.writeFloat(quat.k());
        buffer.writeFloat(quat.r());
    }

    public PhysicsUpdatePacket(PacketBuffer buffer) {
        this.airshipID = buffer.readInt();
        this.centerOfMass = readVector(buffer);
        this.momentum = readVector(buffer);
        this.angularMomentum = readVector(buffer);
        this.angularVelocity = readVector(buffer);
        this.orientation = readQuaternion(buffer);
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeInt(airshipID);
        writeVector(buffer, centerOfMass);
        writeVector(buffer, momentum);
        writeVector(buffer, angularMomentum);
        writeVector(buffer, angularVelocity);
        writeQuaternion(buffer, orientation);
    }


    public static void handle(PhysicsUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Make sure it's only executed on the physical client
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                AirshipContraptionEntity airship = AirshipManager.INSTANCE.AllClientAirships.get(msg.airshipID);
                if(airship == null) return;
                airship.simulatedRigidbody.centerOfMass = msg.centerOfMass;
                airship.simulatedRigidbody.momentum = msg.momentum;
                airship.simulatedRigidbody.angularMomentum = msg.angularMomentum;
                airship.simulatedRigidbody.angularVelocity = msg.angularVelocity;
                airship.simulatedRigidbody.orientation = msg.orientation;
                airship.centerOfMassOffset = airship.simulatedRigidbody.centerOfMass;
                airship.quat = airship.simulatedRigidbody.orientation;
                airship.simulatedRigidbody.globalVelocity = airship.simulatedRigidbody.momentum.scale(1.0 / airship.simulatedRigidbody.getMass());
                airship.simulatedRigidbody.localVelocity = MathUtils.rotateQuatReverse(airship.simulatedRigidbody.globalVelocity, airship.simulatedRigidbody.orientation);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}