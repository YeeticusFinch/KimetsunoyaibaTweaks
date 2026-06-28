package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class ImpactParticleOptions implements ParticleOptions {
    private static final Codec<Vector3f> VECTOR3F_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(Vector3f::x),
            Codec.FLOAT.fieldOf("g").forGetter(Vector3f::y),
            Codec.FLOAT.fieldOf("b").forGetter(Vector3f::z)
        ).apply(instance, Vector3f::new)
    );

    public static final Codec<ImpactParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            VECTOR3F_CODEC.fieldOf("color").forGetter(options -> options.color),
            Codec.FLOAT.fieldOf("size").forGetter(options -> options.size)
        ).apply(instance, ImpactParticleOptions::new)
    );

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<ImpactParticleOptions> DESERIALIZER =
        new ParticleOptions.Deserializer<>() {
            @Override
            public ImpactParticleOptions fromCommand(ParticleType<ImpactParticleOptions> type, StringReader reader)
                    throws CommandSyntaxException {
                reader.expect(' ');
                float r = reader.readFloat();
                reader.expect(' ');
                float g = reader.readFloat();
                reader.expect(' ');
                float b = reader.readFloat();
                reader.expect(' ');
                float size = reader.readFloat();
                return new ImpactParticleOptions(new Vector3f(r, g, b), size);
            }

            @Override
            public ImpactParticleOptions fromNetwork(ParticleType<ImpactParticleOptions> type, FriendlyByteBuf buf) {
                return new ImpactParticleOptions(
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readFloat()
                );
            }
        };

    private final Vector3f color;
    private final float size;

    public ImpactParticleOptions(Vector3f color, float size) {
        this.color = color;
        this.size = size;
    }

    public ImpactParticleOptions(int r, int g, int b, float size) {
        this(new Vector3f(r / 255.0F, g / 255.0F, b / 255.0F), size);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.IMPACT.get();
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
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(getType());
        return String.format("%s %.2f %.2f %.2f %.2f", id, color.x(), color.y(), color.z(), size);
    }

    public Vector3f getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }
}
