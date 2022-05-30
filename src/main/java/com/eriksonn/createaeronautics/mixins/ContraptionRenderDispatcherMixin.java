package com.eriksonn.createaeronautics.mixins;

import com.jozufozu.flywheel.util.WorldAttached;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContraptionRenderDispatcher.class)
public interface ContraptionRenderDispatcherMixin {
    @Accessor("WORLDS")
    public static WorldAttached<ContraptionRenderManager<?>> getWorlds() {
        throw new AssertionError();
    }
}