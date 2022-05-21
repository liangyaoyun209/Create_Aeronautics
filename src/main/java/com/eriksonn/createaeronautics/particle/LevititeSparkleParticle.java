package com.eriksonn.createaeronautics.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class LevititeSparkleParticle  extends SimpleAnimatedParticle {
    Vector3d motion;
    protected LevititeSparkleParticle(ClientWorld world, double x, double y, double z, double dx, double dy,
                                      double dz, IAnimatedSprite sprite) {
        super(world, x, y, z, sprite, world.random.nextFloat() * .5f);
        hasPhysics = false;
        this.lifetime =16;
        this.quadSize *= 0.75F;
        this.xo = x;
        this.yo = y;
        this.zo = z;
        motion=new Vector3d(dx,dy,dz);

        selectSprite(level.random.nextInt(2));
        this.age++;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }else
        {
            int previousIndex = ((this.age-1) * 4)/(this.lifetime+1);
            int index = ((this.age) * 4)/(this.lifetime+1);
            if(previousIndex!=index)
            {
                selectSprite(index*2 + level.random.nextInt(2));
            }

            xd = motion.x;
            yd = motion.y;
            zd = motion.z;
            this.move(this.xd, this.yd, this.zd);
        }

    }

    private void selectSprite(int index) {
        setSprite(sprites.get(7-index, 8));
    }
    public static class Factory implements IParticleFactory<LevititeSparkleParticleData> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite animatedSprite) {
            this.spriteSet = animatedSprite;
        }

        public Particle createParticle(LevititeSparkleParticleData data, ClientWorld worldIn, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new LevititeSparkleParticle(worldIn, x, y, z,xSpeed,ySpeed,zSpeed, this.spriteSet);
        }
    }
}
