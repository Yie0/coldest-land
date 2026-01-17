package com.pycho.core

import com.pycho.ColdestLand
import com.pycho.features.alerts.Alert
import com.pycho.features.barrier.BarrierData
import com.pycho.features.temporal.TemporalBladeItem
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

object Packets {
    val COMBO_INPUT_PACKET_ID = CustomPacketPayload.Type<ComboInputPayload>(ColdestLand.id("combo_input"))
    val ALERT_PACKET_ID = CustomPacketPayload.Type<AlertPayload>(ColdestLand.id("alert_players"))
    val BARRIER_DATA_PACKET_ID = CustomPacketPayload.Type<BarrierDataPayload>(ColdestLand.id("barrier_data"))

    fun registerGlobalReceivers() {
        PayloadTypeRegistry.playC2S().register(COMBO_INPUT_PACKET_ID, ComboInputPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(ALERT_PACKET_ID, AlertPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BARRIER_DATA_PACKET_ID, BarrierDataPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(COMBO_INPUT_PACKET_ID) { payload, context ->
            val player = context.player()
            val stack = player.mainHandItem

            if (stack.item == com.pycho.core.ModItems.TEMPORAL_BLADE) {
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
        return Packets.COMBO_INPUT_PACKET_ID
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
        return Packets.ALERT_PACKET_ID
    }

    fun sendAll(level: ServerLevel){
        level.players().forEach { player ->
            ServerPlayNetworking.send(player, AlertPayload(alert))
        }
    }

    fun send(players: Collection<ServerPlayer>){
        players.forEach { player ->
            ServerPlayNetworking.send(player, AlertPayload(alert))
        }
    }

    fun send(player: ServerPlayer) {
        ServerPlayNetworking.send(player, AlertPayload(alert))
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
            buf.writeFloat(alert.scale)
        }

        fun read(buf: RegistryFriendlyByteBuf): Alert{
            val component = ComponentSerialization.STREAM_CODEC.decode(buf)
            val lifetime = buf.readInt()
            val creationTime = buf.readLong()
            val scale = buf.readFloat()

            return Alert(
                component = component,
                lifetime = lifetime,
                creationTime = creationTime,
                scale = scale,
            )
        }
    }
}

data class BarrierDataPayload(val barrier: BarrierData) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<BarrierDataPayload> {
        return Packets.BARRIER_DATA_PACKET_ID
    }

    fun sendAll(level: ServerLevel){
        level.players().forEach { player ->
            ServerPlayNetworking.send(player, BarrierDataPayload(barrier))
        }
    }

    fun send(player: ServerPlayer) {
        ServerPlayNetworking.send(player, BarrierDataPayload(barrier))
    }

    companion object {
        val ID = CustomPacketPayload.Type<BarrierDataPayload>(ColdestLand.id("barrier_sync"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BarrierDataPayload> = StreamCodec.ofMember(
            { payload, buf -> write(buf, payload.barrier) },
            { buf -> BarrierDataPayload(read(buf)) }
        )

        private fun write(buf: RegistryFriendlyByteBuf, barrier: BarrierData) {
            buf.writeUUID(barrier.id)
            buf.writeDouble(barrier.centerX)
            buf.writeDouble(barrier.centerY)
            buf.writeDouble(barrier.centerZ)
            buf.writeDouble(barrier.width)
            buf.writeDouble(barrier.height)
            buf.writeDouble(barrier.depth)
            buf.writeFloat(barrier.yaw)
            buf.writeFloat(barrier.pitch)
            buf.writeDouble(barrier.precision)
            buf.writeUUID(barrier.ownerUUID)
            buf.writeLong(barrier.creationTime)
            buf.writeLong(barrier.lifetime)
        }

        private fun read(buf: RegistryFriendlyByteBuf): BarrierData {
            val id = buf.readUUID()
            val x = buf.readDouble()
            val y = buf.readDouble()
            val z = buf.readDouble()
            val width = buf.readDouble()
            val height = buf.readDouble()
            val depth = buf.readDouble()
            val yaw = buf.readFloat()
            val pitch = buf.readFloat()
            val precision = buf.readDouble()
            val owner = buf.readUUID()
            val time = buf.readLong()
            val lifetime = buf.readLong()

            return BarrierData.create(
                center = Vec3(x, y, z),
                ownerUUID = owner,
                creationTime = time,
                lifetime = lifetime,
                width = width,
                height = height,
                depth = depth,
                yaw = yaw,
                pitch = pitch,
                precision = precision
            )
        }
    }
}
