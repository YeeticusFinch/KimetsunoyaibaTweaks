package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

/**
 * Particle options for the energy particle, similar to DustParticleOptions.
 * Allows spawning with custom color and size.
 */
public class EnergyParticleOptions implements ParticleOptions {

    // Custom codec for Vector3f since it doesn't have a built-in CODEC in 1.20.1
    private static final Codec<Vector3f> VECTOR3F_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(Vector3f::x),
            Codec.FLOAT.fieldOf("g").forGetter(Vector3f::y),
            Codec.FLOAT.fieldOf("b").forGetter(Vector3f::z)
        ).apply(instance, Vector3f::new)
    );

    public static final Codec<EnergyParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            VECTOR3F_CODEC.fieldOf("color").forGetter(options -> options.color),
            Codec.FLOAT.fieldOf("size").forGetter(options -> options.size)
        ).apply(instance, EnergyParticleOptions::new)
    );

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<EnergyParticleOptions> DESERIALIZER =
        new ParticleOptions.Deserializer<>() {
            @Override
            public EnergyParticleOptions fromCommand(ParticleType<EnergyParticleOptions> type, StringReader reader)
                    throws CommandSyntaxException {
                reader.expect(' ');
                float r = reader.readFloat();
                reader.expect(' ');
                float g = reader.readFloat();
                reader.expect(' ');
                float b = reader.readFloat();
                reader.expect(' ');
                float size = reader.readFloat();
                return new EnergyParticleOptions(new Vector3f(r, g, b), size);
            }

            @Override
            public EnergyParticleOptions fromNetwork(ParticleType<EnergyParticleOptions> type, FriendlyByteBuf buf) {
                return new EnergyParticleOptions(
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readFloat()
                );
            }
        };

    private final Vector3f color;
    private final float size;

    /**
     * Create energy particle options with RGB color (0-1 range) and size.
     * @param color RGB color as Vector3f with values 0-1
     * @param size Size of the particle (1.0 is default)
     */
    public EnergyParticleOptions(Vector3f color, float size) {
        this.color = color;
        this.size = size;
    }

    /**
     * Convenience constructor for RGB values (0-255 range).
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @param size Size of the particle (1.0 is default)
     */
    public EnergyParticleOptions(int r, int g, int b, float size) {
        this(new Vector3f(r / 255f, g / 255f, b / 255f), size);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ENERGY.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(color.x());
        buf.writeFloat(color.y());
        buf.writeFloat(color.z());
        buf.writeFloat(size);
    }

    @Override
    public String writeToString() {
        return String.format("%s %.2f %.2f %.2f %.2f",
            getType().getClass().getSimpleName(),
            color.x(), color.y(), color.z(), size);
    }

    public Vector3f getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }
}
