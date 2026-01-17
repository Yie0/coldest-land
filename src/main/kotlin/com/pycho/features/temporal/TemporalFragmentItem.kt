package com.pycho.features.temporal

import com.pycho.core.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.function.Consumer

class TemporalFragmentItem(props: Item.Properties) : Item(props) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        consumer: Consumer<Component>,
        flags: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, consumer, flags)

        when (this) {
            ModItems.Fragments.ENDER -> {
                consumer.accept(Component.literal("Found in the Ender Altar").withColor(0xAA00FF))
                consumer.accept(Component.literal("Unlocks Temporal Recall").withColor(0xFF55FF))
            }
            ModItems.Fragments.WITHER -> {
                consumer.accept(Component.literal("Drop from Wither").withColor(0x222222))
                consumer.accept(Component.literal("Unlocks Necrotic Decay").withColor(0xAA00AA))
            }
            ModItems.Fragments.GUARDIAN -> {
                consumer.accept(Component.literal("Drop from Elder Guardian").withColor(0x00AAAA))
                consumer.accept(Component.literal("Unlocks Tidal Barrier").withColor(0x55FFFF))
            }
            ModItems.Fragments.WARDEN -> {
                consumer.accept(Component.literal("Drop from Warden").withColor(0x00AA00))
                consumer.accept(Component.literal("Unlocks Sonic Resonance").withColor(0x55FF55))
            }
        }

        consumer.accept(Component.literal("Shift+Right-click Temporal Blade to insert")
            .withColor(0xAAAAAA).withStyle(ChatFormatting.ITALIC))
    }
}
