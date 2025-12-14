package com.pycho.network

import com.pycho.ColdestLand
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.InteractionHand

object ModNetworking {
    val COMBO_INPUT_PACKET_ID = CustomPacketPayload.Type<ComboInputPayload>(ColdestLand.id("combo_input"))

    fun registerGlobalReceivers() {
        PayloadTypeRegistry.playC2S().register(COMBO_INPUT_PACKET_ID, ComboInputPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(COMBO_INPUT_PACKET_ID) { payload, context ->
            val player = context.player()
            val stack = player.mainHandItem

            if (stack.item == ModItems.TEMPORAL_BLADE) {
                context.server().execute {
                    (stack.item as TemporalBladeItem).handleComboInput(
                        stack,
                        player.level(),
                        player,
                        payload.isRightClick,
                        InteractionHand.MAIN_HAND
                    )
                }
            }
        }
    }
}

data class ComboInputPayload(val isRightClick: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<ComboInputPayload> {
        return ModNetworking.COMBO_INPUT_PACKET_ID
    }

    companion object {
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ComboInputPayload> = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ComboInputPayload::isRightClick,
            ::ComboInputPayload
        )
    }
}
