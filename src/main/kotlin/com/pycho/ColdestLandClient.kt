package com.pycho

import com.pycho.core.ModItems
import com.pycho.core.Packets
import com.pycho.core.ComboInputPayload
import com.pycho.features.alerts.AlertHud
import com.pycho.features.barrier.BarrierRenderer
import com.pycho.features.barrier.ClientBarrierManager
import com.pycho.features.temporal.TemporalBladeItem
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft


@Environment(EnvType.CLIENT)
object ColdestLandClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            ClientBarrierManager.tick(client.level?.gameTime ?: 0)

            if (!player.mainHandItem.`is`(ModItems.TEMPORAL_BLADE)) return@register

            val comboStep = TemporalBladeItem.getComboStep(player.mainHandItem) ?: return@register
            if (comboStep == 0.toByte()) return@register

            while (client.options.keyAttack.consumeClick()) {
                ClientPlayNetworking.send(ComboInputPayload(isRightClick = false))
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
            }

        }
        AlertHud.register()

        ClientPlayNetworking.registerGlobalReceiver(Packets.ALERT_PACKET_ID) { payload, context ->
            AlertHud.alerts.addLast(payload.alert)
        }

        ClientPlayNetworking.registerGlobalReceiver(Packets.BARRIER_DATA_PACKET_ID) { payload, context ->
            ClientBarrierManager.upsert(payload.barrier)
        }

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(WorldRenderEvents.BeforeTranslucent { context ->
            BarrierRenderer.render(context)
        })

    }


}
