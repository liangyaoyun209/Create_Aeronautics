package com.eriksonn.createaeronautics.particle;

import com.eriksonn.createaeronautics.index.CAParticleTypes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.contraptions.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.math.vector.Vector3i;

import java.util.Locale;

public class LevititeSparkleParticleData  implements IParticleData, ICustomParticleDataWithSprite<LevititeSparkleParticleData> {
    @Override
    public ParticleType<?> getType() {
        return CAParticleTypes.LEVITITE_SPARKLE.get();
    }

    public static final Codec<LevititeSparkleParticleData> CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.INT.fieldOf("x").forGetter(p -> p.posX),
                    Codec.INT.fieldOf("y").forGetter(p -> p.posY),
                    Codec.INT.fieldOf("z").forGetter(p -> p.posZ))
                    .apply(i, LevititeSparkleParticleData::new));
    public static final IParticleData.IDeserializer<LevititeSparkleParticleData> DESERIALIZER = new IParticleData.IDeserializer<LevititeSparkleParticleData>() {
        public LevititeSparkleParticleData fromCommand(ParticleType<LevititeSparkleParticleData> particleTypeIn, StringReader reader)
                throws CommandSyntaxException {
            reader.expect(' ');
            int x = reader.readInt();
            reader.expect(' ');
            int y = reader.readInt();
            reader.expect(' ');
            int z = reader.readInt();
            return new LevititeSparkleParticleData(x, y, z);
        }

        public LevititeSparkleParticleData fromNetwork(ParticleType<LevititeSparkleParticleData> particleTypeIn, PacketBuffer buffer) {
            return new LevititeSparkleParticleData(buffer.readInt(), buffer.readInt(), buffer.readInt());
        }
    };

    final int posX;
    final int posY;
    final int posZ;

    public LevititeSparkleParticleData(Vector3i pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }
    public LevititeSparkleParticleData(int posX, int posY, int posZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }
    public LevititeSparkleParticleData() {
        this(0, 0, 0);
    }


    @Override
    public void writeToNetwork(PacketBuffer buffer) {
        buffer.writeInt(posX);
        buffer.writeInt(posY);
        buffer.writeInt(posZ);
    }

    @Override
    public IDeserializer<LevititeSparkleParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public Codec<LevititeSparkleParticleData> getCodec(ParticleType<LevititeSparkleParticleData> type) {
        return CODEC;
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %d %d", CAParticleTypes.LEVITITE_SPARKLE.parameter(), posX, posY, posZ);
    }
    @Override
    public ParticleManager.IParticleMetaFactory<LevititeSparkleParticleData> getMetaFactory() {
        return LevititeSparkleParticle.Factory::new;
    }
}
