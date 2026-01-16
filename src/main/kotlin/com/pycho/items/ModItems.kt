package com.pycho.items

import com.pycho.ColdestLand
import com.pycho.components.ModComponents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.ToolMaterial


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

class ItemGroups : CreativeModeTabs() {
    private fun createKey(string: String): ResourceKey<CreativeModeTab> {
        return ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ColdestLand.id(string)
        )
    }
}