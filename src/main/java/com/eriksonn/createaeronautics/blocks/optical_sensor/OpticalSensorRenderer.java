package com.eriksonn.createaeronautics.blocks.optical_sensor;

import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

public class OpticalSensorRenderer extends SmartTileEntityRenderer<OpticalSensorTileEntity> {
    public OpticalSensorRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void renderSafe(OpticalSensorTileEntity tileEntityIn, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer, int light,
                              int overlay) {
        super.renderSafe(tileEntityIn, partialTicks, ms, buffer, light, overlay);

        ms.pushPose();

        IVertexBuilder bufferBuilder = buffer.getBuffer(RenderType.translucent());

//        Tessellator tess = Tessellator.getInstance();
//        BufferBuilder bufferBuilder = tess.getBuilder();
//        bufferBuilder.begin(GL11.GL_QUADS, POSITION_COLOR);
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);


        float red = 0.95f, green = 0f, blue = 0.25f, alpha = 0.4f;

        if(tileEntityIn.getBlockState().getValue(OpticalSensorBlock.POWERED)) {
            red = 0.0f;
            green = 0.8f;
            blue = 0.05f;
        }

        ms.translate(0.5, 0.5, 0.5);
        Direction facing = tileEntityIn.getBlockState().getValue(OpticalSensorBlock.FACING);
        new MatrixTransformStack(ms)
                .rotateY(-facing.getOpposite().toYRot() + 180)
                .rotateX(facing == Direction.UP ? -90 : (facing == Direction.DOWN ? 90 : 0));


        ms.scale(0.2f, 0.2f, Math.min(tileEntityIn.beamLength, (tileEntityIn.detectionLength - 0.1f)) + 0.5f);
        ms.translate(-0.5, -0.5, 0.0);
        for(int i = 0; i < 4; i++) {
            Matrix4f matrix = ms.last().pose();

            bufferBuilder.vertex(matrix, 0, 0f, 0).color(red, green, blue, alpha).uv(0, 0).uv2(0, 240).normal(0, 1, 0).endVertex();
            bufferBuilder.vertex(matrix, 1, 0f, 0).color(red, green, blue, alpha).uv(0, 0).uv2(0, 240).normal(0, 1, 0).endVertex();
            bufferBuilder.vertex(matrix, 1, 0f, 1).color(red, green, blue, 0.0f).uv(0, 0).uv2(0, 240).normal(0, 1, 0).endVertex();
            bufferBuilder.vertex(matrix, 0, 0f, 1).color(red, green, blue, 0.0f).uv(0, 0).uv2(0, 240).normal(0, 1, 0).endVertex();

            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(new Quaternion(new Vector3f(0, 0, 1), 90, true));
            ms.translate(-0.5, -0.5, -0.5);
        }
//        bufferBuilder.en();
//        WorldVertexBufferUploader.end(bufferBuilder);

        RenderSystem.disableBlend();
//        RenderSystem.enableCull();
        RenderSystem.popMatrix();

        ms.popPose();

    }


}
