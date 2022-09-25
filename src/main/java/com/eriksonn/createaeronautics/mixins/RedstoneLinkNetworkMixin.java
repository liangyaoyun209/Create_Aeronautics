package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.blocks.redstone.modulating_redstone_link.ModulatingRedstoneLinkTileEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipContraptionEntity;
import com.eriksonn.createaeronautics.contraptions.AirshipManager;
import com.eriksonn.createaeronautics.dimension.AirshipDimensionManager;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.IRedstoneLinkable;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Set;

@Mixin(RedstoneLinkNetworkHandler.class)
public class RedstoneLinkNetworkMixin {

    @Inject(method = "updateNetworkOf", remap = false, at = @At("HEAD"))
    public void updateNetworkOf(IWorld world, IRedstoneLinkable inRealWorldActor, CallbackInfo ci) {

        // airship dimension trickery
        ServerWorld airshipDimension = AirshipDimensionManager.INSTANCE.getWorld();
        if(world != airshipDimension) {

            // iterate over all airships in the world
            for(AirshipContraptionEntity airship : AirshipManager.INSTANCE.AllAirships.values()) {

                // plotpos
                BlockPos plotPos = airship.getPlotPos();

                // get network
                Set<IRedstoneLinkable> airshipNetwork = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(airshipDimension, inRealWorldActor);
                Set<IRedstoneLinkable> realWorldNetwork = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, inRealWorldActor);

                int power = 0;

                for (Iterator<IRedstoneLinkable> iterator = airshipNetwork.iterator(); iterator.hasNext();) {
                    IRedstoneLinkable inAirshipDimensionActor = iterator.next();

                    if (!inAirshipDimensionActor.isAlive()) {
                        iterator.remove();
                        continue;
                    }

                    if(AirshipManager.getIdFromPlotPos(inAirshipDimensionActor.getLocation()) != airship.plotId)
                        continue;

                    if(!withinRange(inRealWorldActor, airship, plotPos, inAirshipDimensionActor)) {
                        iterator.remove();
                        continue;
                    }

                    if (power < 15)
                        power = Math.max(inAirshipDimensionActor.getTransmittedStrength(), power);

                    for (Iterator<IRedstoneLinkable> iterator1 = realWorldNetwork.iterator(); iterator1.hasNext();) {
                        IRedstoneLinkable inRealWorldActor2 = iterator1.next();

                        if(!inRealWorldActor2.isAlive()) {
                             iterator1.remove();
                            continue;
                        }

                        if(!withinRange(inRealWorldActor2, airship, plotPos, inAirshipDimensionActor)) {
                            iterator1.remove();
                            continue;
                        }

                        if (power < 15)
                            power = Math.max(inRealWorldActor2.getTransmittedStrength(), power);
                    }
                }

                if (inRealWorldActor instanceof LinkBehaviour) {
                    LinkBehaviour linkBehaviour = (LinkBehaviour) inRealWorldActor;
                    // fix one-to-one loading order problem
                    if (linkBehaviour.isListening()) {
                        linkBehaviour.newPosition = true;
                        linkBehaviour.setReceivedStrength(power);
                    }
                }

                for (IRedstoneLinkable other : airshipNetwork) {
                    if (other != inRealWorldActor && other.isListening() && withinRange(inRealWorldActor, airship, plotPos, other))
                        other.setReceivedStrength(power);
                }

            }

        }else
        {
            IRedstoneLinkable inAirshipDimensionActor = inRealWorldActor;
            int id = AirshipManager.getIdFromPlotPos(inRealWorldActor.getLocation());
            AirshipContraptionEntity airship = AirshipManager.INSTANCE.AllAirships.get(id);
            if(airship==null)
                return;
            // plotpos
            BlockPos plotPos = airship.getPlotPos();
            Set<IRedstoneLinkable> airshipNetwork = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(airshipDimension, inRealWorldActor);
            Set<IRedstoneLinkable> realWorldNetwork = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(airship.level, inRealWorldActor);

            int power = 0;

            for (Iterator<IRedstoneLinkable> iterator = realWorldNetwork.iterator(); iterator.hasNext();) {
                inRealWorldActor = iterator.next();
                if (!inRealWorldActor.isAlive()) {
                    iterator.remove();
                    continue;
                }

                if(!withinRange(inRealWorldActor, airship, plotPos, inAirshipDimensionActor)) {
                    iterator.remove();
                    continue;
                }

                if (power < 15)
                    power = Math.max(inRealWorldActor.getTransmittedStrength(), power);

                for (Iterator<IRedstoneLinkable> iterator1 = airshipNetwork.iterator(); iterator1.hasNext();) {
                    IRedstoneLinkable inAirshipDimensionActor2 = iterator1.next();

                    if(!inAirshipDimensionActor2.isAlive()) {
                        iterator1.remove();
                        continue;
                    }

                    if(!withinRange(inRealWorldActor, airship, plotPos, inAirshipDimensionActor2)) {
                        iterator1.remove();
                        continue;
                    }

                    if (power < 15)
                        power = Math.max(inAirshipDimensionActor2.getTransmittedStrength(), power);
                }
            }

            if (inAirshipDimensionActor instanceof LinkBehaviour) {
                LinkBehaviour linkBehaviour = (LinkBehaviour) inAirshipDimensionActor;
                // fix one-to-one loading order problem
                if (linkBehaviour.isListening()) {
                    linkBehaviour.newPosition = true;
                    linkBehaviour.setReceivedStrength(power);
                }
            }

            for (IRedstoneLinkable other : realWorldNetwork) {
                if (other != inAirshipDimensionActor && other.isListening() && withinRange(other, airship, plotPos, inAirshipDimensionActor))
                    other.setReceivedStrength(power);
            }

        }

    }

    private boolean withinRange(IRedstoneLinkable actor, AirshipContraptionEntity airship, BlockPos plotPos, IRedstoneLinkable other) {
        // get position of other relative to airship
        BlockPos otherPos = other.getLocation().subtract(plotPos);

        // get position in overworld
        Vector3d overworldPos = airship.toGlobalVector(new Vector3d(otherPos.getX(), otherPos.getY(), otherPos.getZ()), 1.0f);

        // range check
        return actor.getLocation().closerThan(new BlockPos(overworldPos), AllConfigs.SERVER.logistics.linkRange.get());
    }
    @Inject(method = "updateNetworkOf",locals = LocalCapture.CAPTURE_FAILHARD, remap = false, at = @At(value = "INVOKE",target="Lcom/simibubi/create/foundation/tileEntity/behaviour/linked/LinkBehaviour;setReceivedStrength(I)V"))
    private void setReceivedStrength1(IWorld world, IRedstoneLinkable actor, CallbackInfo ci, Set network, int power, LinkBehaviour linkBehaviour)
    {
        if(linkBehaviour.tileEntity instanceof ModulatingRedstoneLinkTileEntity)
        {
            ((ModulatingRedstoneLinkTileEntity) linkBehaviour.tileEntity).setTransmitterLink((LinkBehaviour)actor);
        }
    }
    @Inject(method = "updateNetworkOf",locals = LocalCapture.CAPTURE_FAILHARD, remap = false, at = @At(value = "INVOKE",target="Lcom/simibubi/create/content/logistics/IRedstoneLinkable;setReceivedStrength(I)V"))
    private void setReceivedStrength2(IWorld world, IRedstoneLinkable actor, CallbackInfo ci, Set network, int power, Iterator var5, IRedstoneLinkable other)
    {
        LinkBehaviour behaviour = (LinkBehaviour)other;
        if(behaviour.tileEntity instanceof ModulatingRedstoneLinkTileEntity)
        {
            ((ModulatingRedstoneLinkTileEntity) behaviour.tileEntity).setTransmitterLink((LinkBehaviour)actor);
        }
    }
    @Unique
    private IRedstoneLinkable other;
    @Unique
    private IRedstoneLinkable actor;
    @Inject(remap=false,locals = LocalCapture.CAPTURE_FAILHARD,method = "updateNetworkOf",at = @At(value="INVOKE",target = "Ljava/lang/Math;max(II)I"))
    private void cacheTransmitter(IWorld world, IRedstoneLinkable actor, CallbackInfo ci, Set network, int power, Iterator iterator, IRedstoneLinkable other)
    {
        this.other=other;
        this.actor=actor;
    }

    @Redirect(remap=false,method = "updateNetworkOf",at = @At(value="INVOKE",target = "Ljava/lang/Math;max(II)I"))
    private int getTransmittedStrength(int a, int b)
    {

        LinkBehaviour behaviour = (LinkBehaviour)other;
        if(behaviour.tileEntity instanceof ModulatingRedstoneLinkTileEntity) {
            int power = ((ModulatingRedstoneLinkTileEntity) behaviour.tileEntity).getCustomTransmissionPower((LinkBehaviour) actor);
            return Math.max(power, b);
        }
        else
            return Math.max(a,b);
    }
}
