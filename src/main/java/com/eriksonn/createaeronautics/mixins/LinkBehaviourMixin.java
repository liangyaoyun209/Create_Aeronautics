package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.blocks.redstone.modulating_redstone_link.ModulatingRedstoneLinkTileEntity;
import com.simibubi.create.content.logistics.IRedstoneLinkable;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour;
import net.minecraft.tileentity.TileEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntConsumer;

@Mixin(LinkBehaviour.class)
public class LinkBehaviourMixin{

    @Redirect(remap=false,method = "setReceivedStrength(I)V",at = @At(value="INVOKE",target = "Ljava/util/function/IntConsumer;accept(I)V"))
    private void injected(IntConsumer signalCallback, int networkPower)
    {
        TileEntity tile = ((LinkBehaviour) ((Object) this)).tileEntity;
        if(tile instanceof ModulatingRedstoneLinkTileEntity)
        {
            networkPower=((ModulatingRedstoneLinkTileEntity)tile).getCustomNetworkPower(networkPower);
        }
        signalCallback.accept(networkPower);
    }

}
