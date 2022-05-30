package com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing;

import com.eriksonn.createaeronautics.index.CABlockPartials;
import com.eriksonn.createaeronautics.utils.math.Quaternionf;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.foundation.render.PartialBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;

public class GyroscopicPropellerBearingRenderer  extends KineticTileEntityRenderer {
    public GyroscopicPropellerBearingRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer,
                              int light, int overlay) {

        if (Backend.getInstance().canUseInstancing(te.getLevel())) return;

        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

        IBearingTileEntity bearingTe = (IBearingTileEntity) te;
        final Direction facing = te.getBlockState()
                .getValue(BlockStateProperties.FACING);
        Vector3d normal = new Vector3d(facing.getStepX(),facing.getStepY(),facing.getStepZ());
        GyroscopicPropellerBearingTileEntity gyroBearing = (GyroscopicPropellerBearingTileEntity)te;
        Quaternionf tiltQuat = new Quaternionf(gyroBearing.tiltQuat);
        Quaternionf Q = tiltQuat.copy();
        Q.conj();
        Q.mul(new Quaternionf((float)normal.x,(float)normal.y,(float)normal.z,0f));
        Q.mul(tiltQuat);
        Vector3d contraptionNormal = new Vector3d(Q.i(),Q.j(),Q.k());

        PartialModel top = AllBlockPartials.BEARING_TOP;
        SuperByteBuffer superBuffer = PartialBufferer.get(top, te.getBlockState());

        superBuffer.translate(normal.scale(4/16f));
        superBuffer.rotateCentered(tiltQuat.toMojangQuaternion());
        superBuffer.translate(normal.scale(-4/16f));

        float interpolatedAngle = bearingTe.getInterpolatedAngle(partialTicks - 1);
        kineticRotationTransform(superBuffer, te, facing.getAxis(), (float) (interpolatedAngle / 180 * Math.PI), light);

        if (facing.getAxis()
                .isHorizontal())
            superBuffer.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));

        superBuffer.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));


        for(int i =0;i<4;i++) {

            SuperByteBuffer headBuffer = PartialBufferer.get(CABlockPartials.GYRO_BEARING_PISTON_HEAD, te.getBlockState());
            SuperByteBuffer poleBuffer = PartialBufferer.get(CABlockPartials.GYRO_BEARING_PISTON_POLE, te.getBlockState());
            Vector3d originalPos = VecHelper.rotate(new Vector3d(6/16.0,0,0), -90*i, Direction.Axis.Y);
            Vector3d translatedPos=originalPos;

            if (facing.getAxis().isHorizontal()) {

                translatedPos=VecHelper.rotate(translatedPos, AngleHelper.horizontalAngle(facing),Direction.Axis.Z);
                translatedPos=VecHelper.rotate(translatedPos, -90 + AngleHelper.verticalAngle(facing),Direction.Axis.X);
            }

            double translateDistance = translatedPos.dot(contraptionNormal)/normal.dot(contraptionNormal);
            translatedPos = translatedPos.add(normal.scale(translateDistance+3/16.0));


            //headBuffer.translate(originalPos.scale(-1));
            headBuffer.translate(translatedPos);
            headBuffer.translate(0.5f,0.5f,0.5f);


            poleBuffer.translate(translatedPos);
            poleBuffer.translate(0.5f,0.5f,0.5f);

            headBuffer.rotate(tiltQuat.toMojangQuaternion());
            int j =i;
            if(facing==Direction.DOWN)
            {
                if(i%2==0) {
                    headBuffer.rotate(Direction.EAST, AngleHelper.rad(180));
                    poleBuffer.rotate(Direction.EAST, AngleHelper.rad(180));
                }
                else {
                    headBuffer.rotate(Direction.SOUTH, AngleHelper.rad(180));
                    poleBuffer.rotate(Direction.SOUTH, AngleHelper.rad(180));
                }
            }
            if (facing.getAxis().isHorizontal()) {

                headBuffer.rotate(Direction.UP,
                        AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
                poleBuffer.rotate(Direction.UP,
                        AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
                headBuffer.rotate(Direction.EAST, AngleHelper.rad(-90 + AngleHelper.verticalAngle(facing)));
                poleBuffer.rotate(Direction.EAST, AngleHelper.rad(-90 + AngleHelper.verticalAngle(facing)));
                j = 2-j;
            }

            //headBuffer.translate(0,3/16.0,0);
            poleBuffer.translate(0,0.5f/16.0,0);

            headBuffer.rotate(Direction.UP, AngleHelper.rad(-90*j));
            poleBuffer.rotate(Direction.UP, AngleHelper.rad(-90*j));

            headBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
            poleBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(KineticTileEntity te) {
        return PartialBufferer.getFacing(AllBlockPartials.SHAFT_HALF, te.getBlockState(), te.getBlockState()
                .getValue(BearingBlock.FACING)
                .getOpposite());
    }
}
