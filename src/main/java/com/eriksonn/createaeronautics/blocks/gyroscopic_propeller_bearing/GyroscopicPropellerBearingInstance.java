package com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing;

import com.eriksonn.createaeronautics.index.CABlockPartials;
import com.eriksonn.createaeronautics.utils.MathUtils;
import com.jozufozu.flywheel.backend.instancing.IDynamicInstance;
import com.jozufozu.flywheel.backend.material.InstanceMaterial;
import com.jozufozu.flywheel.backend.material.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.ModelData;
import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.BackHalfShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class GyroscopicPropellerBearingInstance extends BackHalfShaftInstance implements IDynamicInstance {

    final GyroscopicPropellerBearingTileEntity bearing;

    //final OrientedData topInstance;
    final ModelData topData;

    protected ModelData[] pistonHeads=new ModelData[4];
    protected ModelData[] pistonPoles=new ModelData[4];

    final Vector3f rotationAxis;
    final Quaternion blockOrientation;

    public GyroscopicPropellerBearingInstance(MaterialManager<?> modelManager, GyroscopicPropellerBearingTileEntity tile) {
        super(modelManager, tile);

        this.bearing = tile;

        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        rotationAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis).step();
        bearing.blockNormal= new Vector3d(facing.getStepX(),facing.getStepY(),facing.getStepZ());
        blockOrientation = getBlockStateOrientation(facing);

        PartialModel top =
                bearing.isWoodenTop() ? AllBlockPartials.BEARING_TOP_WOODEN : AllBlockPartials.BEARING_TOP;

        InstanceMaterial<ModelData> mat = getTransformMaterial();
        topData=mat.getModel(top,blockState).createInstance();

        for (int i =0;i<4;i++)
        {
            //mat = getTransformMaterial();
            pistonHeads[i]=mat.getModel(CABlockPartials.GYRO_BEARING_PISTON_HEAD, blockState).createInstance();
            pistonPoles[i]=mat.getModel(CABlockPartials.GYRO_BEARING_PISTON_POLE, blockState).createInstance();
        }

        //topInstance = getOrientedMaterial().getModel(top, blockState).createInstance();

        Vector3d pos = VecHelper.getCenterOf(getInstancePosition()).subtract(bearing.blockNormal.scale(0.25)).subtract(0.5,0.5,0.5);

        //topInstance.setPosition(getInstancePosition()).setRotation(blockOrientation);
        Vector3d pivot = bearing.blockNormal.scale(0.25).add(0.5,0.5,0.5);
        //topInstance.setPivot((float)pivot.x,(float)pivot.y,(float)pivot.z);
        //topInstance.setPivot(0.5f,0.75f,0.5f);
    }

    @Override
    public void beginFrame() {


        float interpolatedAngle = bearing.getInterpolatedAngle(AnimationTickHolder.getPartialTicks() - 1);
        Quaternion rot=Quaternion.ONE.copy();

        rot.mul(bearing.tiltQuat);

        rot.mul(rotationAxis.rotationDegrees(interpolatedAngle));

        rot.mul(blockOrientation);

        MatrixStack ms = new MatrixStack();
        MatrixTransformStack msr = MatrixTransformStack.of(ms);

        msr.translate(getInstancePosition());
        msr.centre();
        msr.push();
        msr.translate(bearing.blockNormal.scale(0.25));
        msr.multiply(bearing.tiltQuat);
        msr.translate(bearing.blockNormal.scale(-0.25));
        msr.multiply(blockOrientation);




        msr.multiply(rotationAxis.rotationDegrees(interpolatedAngle));

        msr.unCentre();
        topData.setTransform(ms);
        msr.pop();
        for (int i = 0; i < 4; i++) {
            msr.push();
            msr.multiply(blockOrientation);
            Vector3d originalPos = VecHelper.rotate(new Vector3d(6/16.0,0,0), -90*i, Direction.Axis.Y);

            Vector3d translatedPos=originalPos;

            translatedPos = MathUtils.rotateQuatReverse(translatedPos,blockOrientation);
            double translateDistance = -translatedPos.dot(bearing.tiltVector)/bearing.blockNormal.dot(bearing.tiltVector);
            //translateDistance=0;
            translatedPos = originalPos.add(new Vector3d(0. ,1,0).scale(translateDistance+3/16.0));


            msr.translate(translatedPos);
            msr.push();
            msr.rotate(-90*i,Direction.Axis.Y);
            msr.translate(0,1/32.0,0);
            pistonPoles[i].setTransform(ms);
            msr.pop();
            Quaternion Q = blockOrientation.copy();
            Q.conj();
            Q.mul(bearing.tiltQuat);
            Q.mul(blockOrientation);
            msr.multiply(Q);
            msr.rotate(-90*i,Direction.Axis.Y);
            pistonHeads[i].setTransform(ms);
            msr.pop();
        }
        //topData

        //topInstance.setRotation(rot);
    }

    @Override
    public void updateLight() {
        super.updateLight();
        //relight(pos, topInstance);
        relight(pos,topData);
        for (int i = 0; i < 4; i++) {
            relight(pos,pistonHeads[i]);
            relight(pos,pistonPoles[i]);
        }
    }

    @Override
    public void remove() {
        super.remove();
        //topInstance.delete();
        topData.delete();
        for (int i = 0; i < 4; i++) {
            pistonHeads[i].delete();
            pistonPoles[i].delete();
        }
    }

    static Quaternion getBlockStateOrientation(Direction facing) {
        Quaternion orientation;

        if (facing.getAxis().isHorizontal()) {
            orientation = Vector3f.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
        } else {
            orientation = Quaternion.ONE.copy();
        }

        orientation.mul(Vector3f.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
        return orientation;
    }
}
