package com.eriksonn.createaeronautics.blocks.compass_table;

import com.eriksonn.createaeronautics.index.CABlockPartials;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.SplitShaftRenderer;
import com.simibubi.create.foundation.render.PartialBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;


public class CompassTableRenderer extends SafeTileEntityRenderer<CompassTableTileEntity> {

    public CompassTableRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void renderSafe(CompassTableTileEntity te, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer, int light, int overlay) {
        // mogus mogus so sus

        if(te.hasCompass()) {
            SuperByteBuffer compass = PartialBufferer.get(CABlockPartials.MINI_COMPASS, te.getBlockState());

            compass.translate(0.0, 14.0 / 16.0, 0.0);
            compass.renderInto(ms, buffer.getBuffer(RenderType.solid()));

            // hand / pointer
            SuperByteBuffer hand = PartialBufferer.get(CABlockPartials.MINI_COMPASS_ARROW, te.getBlockState());


            hand.rotateCentered(Direction.UP, te.getCompassRotation());

            hand.translate(0.0, 14.0 / 16.0, 0.0);
            hand.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        for(int i = 0; i < 4; i++) {
            PartialModel top = CABlockPartials.COMPASS_CONTACT_OFF;

            if(te.getBlockState().getBlock().getSignal(te.getBlockState(), te.getWorld(), te.getBlockPos(), (new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST})[3 - i]) > 0) {
                top = CABlockPartials.COMPASS_CONTACT_ON;
            }

            SuperByteBuffer superBuffer = PartialBufferer.get(top, te.getBlockState());

            superBuffer.translate(0.0, 0.0, 0.0);
            superBuffer.rotateCentered(Direction.UP, (float) Math.toRadians(90 * (i - 1)));

            superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}

