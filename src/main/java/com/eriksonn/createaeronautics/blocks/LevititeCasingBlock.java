package com.eriksonn.createaeronautics.blocks;

import com.eriksonn.createaeronautics.particle.LevititeSparkleParticleData;
import com.eriksonn.createaeronautics.physics.api.IFloatingBlockProvider;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.FireworkParticle;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

public class LevititeCasingBlock extends CasingBlock implements IFloatingBlockProvider {
    public LevititeCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public double getRelativeStrength() {
        return 6;
    }

    @Override
    public double getVerticalFriction() {
        return 3;
    }

    @Override
    public boolean isAltitudeLocking() {
        return true;
    }

    @Override
    public void tickOnForceApplication(World pLevel, BlockPos pPos,Vector3d gravityForce, Vector3d frictionForce) {
        double probability = 0.015*Math.min(6,1 + gravityForce.length()/6.0 +frictionForce.length()*1.5);
        if (pLevel.random.nextFloat()<probability) {
            spawnParticles(pLevel, pPos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState pState, World pLevel, BlockPos pPos, Random pRand) {
        if (pLevel.random.nextFloat()<0.2) {
            spawnParticles(pLevel, pPos);
        }

    }

    private static void spawnParticles(World pLevel, BlockPos pPos) {
        double d0 = 0.5625D;
        Random random = pLevel.random;

        for(Direction direction : Direction.values()) {
            BlockPos blockpos = pPos.relative(direction);
            if (!pLevel.getBlockState(blockpos).isSolidRender(pLevel, blockpos)) {
                Direction.Axis direction$axis = direction.getAxis();
                double d1 = direction$axis == Direction.Axis.X ? 0.5D + 0.5625D * (double)direction.getStepX() : (double)random.nextFloat();
                double d2 = direction$axis == Direction.Axis.Y ? 0.5D + 0.5625D * (double)direction.getStepY() : (double)random.nextFloat();
                double d3 = direction$axis == Direction.Axis.Z ? 0.5D + 0.5625D * (double)direction.getStepZ() : (double)random.nextFloat();
                pLevel.addParticle(new LevititeSparkleParticleData(pPos), (double)pPos.getX() + d1, (double)pPos.getY() + d2, (double)pPos.getZ() + d3, 0.0D, 0.0D, 0.0D);
            }
        }

    }

}
