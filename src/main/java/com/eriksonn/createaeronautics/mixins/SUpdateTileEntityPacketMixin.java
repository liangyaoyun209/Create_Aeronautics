package com.eriksonn.createaeronautics.mixins;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SUpdateTileEntityPacket.class)
public interface SUpdateTileEntityPacketMixin {

    @Accessor
    BlockPos getPos();

    @Accessor
    int getType();

    @Accessor
    CompoundNBT getTag();

}
