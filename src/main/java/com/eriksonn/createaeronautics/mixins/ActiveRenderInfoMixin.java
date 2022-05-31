package com.eriksonn.createaeronautics.mixins;

import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ActiveRenderInfo.class})
public interface ActiveRenderInfoMixin {
    @Invoker
    void invokeSetPosition(double x, double y, double z);

    @Accessor
    Vector3f getForwards();

    @Accessor
    Vector3f getUp();

    @Accessor
    Vector3f getLeft();
}
