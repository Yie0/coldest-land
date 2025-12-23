package com.pycho.network

import Alert
import com.pycho.ColdestLand
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand

object ModNetworking {
    val COMBO_INPUT_PACKET_ID = CustomPacketPayload.Type<ComboInputPayload>(ColdestLand.id("combo_input"))
    val ALERT_PACKET_ID = CustomPacketPayload.Type<AlertPayload>(ColdestLand.id("alert_players"))

    fun registerGlobalReceivers() {
        PayloadTypeRegistry.playC2S().register(COMBO_INPUT_PACKET_ID, ComboInputPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(ALERT_PACKET_ID, AlertPayload.CODEC)

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

data class AlertPayload(val alert: Alert) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<AlertPayload> {
        return ModNetworking.ALERT_PACKET_ID
    }

    fun sendAll(level: ServerLevel){
        level.players().forEach { player ->
            ServerPlayNetworking.send(player, AlertPayload(alert))
        }
    }

    companion object {
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, AlertPayload> = StreamCodec.ofMember(
            { payload, buf -> write(buf, payload.alert)},
            { buf -> AlertPayload(read(buf))},
        )

        fun write(buf: RegistryFriendlyByteBuf, alert: Alert) {
            ComponentSerialization.STREAM_CODEC.encode(buf,alert.component)
            buf.writeInt(alert.lifetime)
            buf.writeLong(alert.creationTime)
        }

        fun read(buf: RegistryFriendlyByteBuf): Alert{
            val component = ComponentSerialization.STREAM_CODEC.decode(buf)
            val lifetime = buf.readInt()
            val creationTime = buf.readLong()

            return Alert(
                component = component,
                lifetime = lifetime,
                creationTime
            )
        }
    }
}