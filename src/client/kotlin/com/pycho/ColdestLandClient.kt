package com.pycho

import com.pycho.gui.AlertHud
import com.pycho.render.BarrierRenderer
import com.pycho.client.systems.ClientBarrierManager
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import com.pycho.network.ComboInputPayload
import com.pycho.network.ModNetworking
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents


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

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ALERT_PACKET_ID) { payload, context ->
            AlertHud.alerts.addLast(payload.alert)
        }

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.BARRIER_DATA_PACKET_ID) { payload, context ->
            ClientBarrierManager.upsert(payload.barrier)
        }

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(WorldRenderEvents.BeforeTranslucent { context ->
            BarrierRenderer.render(context)
        })

    }


}