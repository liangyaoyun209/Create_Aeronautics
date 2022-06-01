package com.eriksonn.createaeronautics.mixins;

import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderInfo;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContraptionRenderManager.class)
public interface ContraptionRenderManagerMixin {

    @Accessor
    Int2ObjectMap<ContraptionRenderInfo> getRenderInfos();

}
