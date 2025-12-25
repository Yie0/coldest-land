package com.pycho.network

import Alert
import com.pycho.ColdestLand
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import com.pycho.systems.barrier.BarrierData
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

object ModNetworking {
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
        return ModNetworking.BARRIER_DATA_PACKET_ID
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
        val ID = CustomPacketPayload.Type<BarrierDataPayload>(com.pycho.ColdestLand.id("barrier_sync"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BarrierDataPayload> = StreamCodec.ofMember(
            { payload, buf -> write(buf, payload.barrier) },
            { buf -> BarrierDataPayload(read(buf)) }
        )

        private fun write(buf: RegistryFriendlyByteBuf, barrier: BarrierData) {
            buf.writeUUID(barrier.id)
            buf.writeDouble(barrier.centerX)
            buf.writeDouble(barrier.centerY)
            buf.writeDouble(barrier.centerZ)
            buf.writeFloat(barrier.radius)
            buf.writeFloat(barrier.height)
            buf.writeUUID(barrier.ownerUUID)
            buf.writeLong(barrier.creationTime)
        }

        private fun read(buf: RegistryFriendlyByteBuf): BarrierData {
            val id = buf.readUUID()
            val x = buf.readDouble()
            val y = buf.readDouble()
            val z = buf.readDouble()
            val radius = buf.readFloat()
            val height = buf.readFloat()
            val owner = buf.readUUID()
            val time = buf.readLong()

            return BarrierData(
                id = id,
                centerX = x, centerY = y, centerZ = z,
                radius = radius, height = height,
                ownerUUID = owner,
                creationTime = time,

                boundingBox = net.minecraft.world.phys.AABB(
                    x - radius, y, z - radius,
                    x + radius, y + height, z + radius
                ),
                collisionShape = net.minecraft.world.phys.shapes.Shapes.create(
                    net.minecraft.world.phys.AABB(
                        x - radius, y, z - radius,
                        x + radius, y + height, z + radius
                    )
                )
            )
        }
    }}