package com.eriksonn.createaeronautics.mixins;

import net.minecraft.item.ItemUseContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemUseContext.class)
public interface ItemUseContextMixin {

    @Accessor
    void setLevel(World level);
}
