package com.pycho.items

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.pycho.ColdestLand
import io.netty.buffer.ByteBuf
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

object ModComponents {
    // Energy component for TemporalBlade
    val CHRONO_ENERGY: DataComponentType<Int> = register(
        "chrono_energy",
        DataComponentType.builder<Int>()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
    )

    // Fragments bitmask
    val FRAGMENTS: DataComponentType<Byte> = register(
        "fragments",
        DataComponentType.builder<Byte>()
            .persistent(Codec.BYTE)
            .networkSynchronized(ByteBufCodecs.BYTE)
    )

    // Energy cost multiplier
    val ENERGY_MULTIPLIER: DataComponentType<Float> = register(
        "energy_multiplier",
        DataComponentType.Builder<Float>()
            .persistent(Codec.FLOAT)
            .networkSynchronized(ByteBufCodecs.FLOAT)
    )

    // Combo tracking
    val COMBO_SEQUENCE: DataComponentType<Byte> = register(
        "combo_sequence",
        DataComponentType.Builder<Byte>()
            .persistent(Codec.BYTE)
            .networkSynchronized(ByteBufCodecs.BYTE)
    )

    val COMBO_STEP: DataComponentType<Byte> = register(
        "combo_step",
        DataComponentType.Builder<Byte>()
            .persistent(Codec.BYTE)
            .networkSynchronized(ByteBufCodecs.BYTE)
    )

    val COMBO_TIMESTAMP: DataComponentType<Long> = register(
        "combo_timestamp",
        DataComponentType.Builder<Long>()
            .persistent(Codec.LONG)
            .networkSynchronized(ByteBufCodecs.LONG)
    )

    val TEMPORAL_HISTORY: AttachmentType<MutableList<TemporalRecord>> = AttachmentRegistry.createDefaulted(ColdestLand.id("temporal_history")) { mutableListOf() }

    fun <T : Any> register(name: String, builder: DataComponentType.Builder<T>): DataComponentType<T> {
        return Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ColdestLand.id(name),
            builder.build()
        )
    }


}

data class TemporalRecord(val x: Double, val y: Double, val z: Double, val health: Float) {
    companion object {
        // Save to disk/NBT
        val CODEC: Codec<TemporalRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter { it.x },
                Codec.DOUBLE.fieldOf("y").forGetter { it.y },
                Codec.DOUBLE.fieldOf("z").forGetter { it.z },
                Codec.FLOAT.fieldOf("health").forGetter { it.health }
            ).apply(instance, ::TemporalRecord)
        }

        // Sync over network
        val STREAM_CODEC: StreamCodec<ByteBuf, TemporalRecord> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, TemporalRecord::x,
            ByteBufCodecs.DOUBLE, TemporalRecord::y,
            ByteBufCodecs.DOUBLE, TemporalRecord::z,
            ByteBufCodecs.FLOAT, TemporalRecord::health,
            ::TemporalRecord
        )

        val ZERO = TemporalRecord(0.0, 0.0, 0.0, 0.0f)
    }
}
