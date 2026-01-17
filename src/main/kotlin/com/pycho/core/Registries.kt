package com.pycho.core

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.pycho.ColdestLand
import com.pycho.features.temporal.TemporalBladeItem
import com.pycho.features.temporal.TemporalFragmentItem
import io.netty.buffer.ByteBuf
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.ToolMaterial

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

object ModItems {
    val TEMPORAL_BLADE: Item = register(
        name = "temporal_blade",
        factory = ::TemporalBladeItem,
        properties = Item.Properties()
            .stacksTo(1)
            .fireResistant()
            .rarity(Rarity.EPIC)
            .durability(0) // Unbreakable
            .component(ModComponents.CHRONO_ENERGY, TemporalBladeItem.MAX_ENERGY) // Start with full energy
            .component(ModComponents.FRAGMENTS, 0) // No fragments initially
            .component(ModComponents.ENERGY_MULTIPLIER, 1.0f) // Base multiplier
            .component(ModComponents.COMBO_SEQUENCE, 0) // Combo sequence
            .component(ModComponents.COMBO_STEP, 0) // Combo step
            .component(ModComponents.COMBO_TIMESTAMP, 0L) // Last click time
            .sword(ToolMaterial.NETHERITE, 7f, -2.4f),
        group = CreativeModeTabs.COMBAT
    )
    object Fragments{
        val ENDER: Item = register(
            name = "ender_fragment",
            factory = ::TemporalFragmentItem,
            properties = Item.Properties()
                .rarity(Rarity.EPIC),
            group = CreativeModeTabs.TOOLS_AND_UTILITIES
        )
        val GUARDIAN: Item = register(
            name = "guardian_fragment",
            factory = ::TemporalFragmentItem,
            properties = Item.Properties()
                .rarity(Rarity.EPIC),
            group = CreativeModeTabs.TOOLS_AND_UTILITIES
        )
        val WARDEN: Item = register(
            name = "warden_fragment",
            factory = ::TemporalFragmentItem,
            properties = Item.Properties()
                .rarity(Rarity.EPIC),
            group = CreativeModeTabs.TOOLS_AND_UTILITIES
        )
        val WITHER: Item = register(
            name = "wither_fragment",
            factory = ::TemporalFragmentItem,
            properties = Item.Properties()
                .rarity(Rarity.EPIC),
            group = CreativeModeTabs.TOOLS_AND_UTILITIES
        )
    }

    fun register(name: String, factory: (Item.Properties) -> Item, properties: Item.Properties, group: ResourceKey<CreativeModeTab>) : Item {
        val itemKey: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, ColdestLand.id(name))
        val item: Item = Registry.register(BuiltInRegistries.ITEM, itemKey, factory(properties.setId(itemKey)))
        ItemGroupEvents.modifyEntriesEvent(group).register({ entries ->
            entries.accept(item)
        });
        return item
    }

    fun setup(){
        Fragments
    }
}
