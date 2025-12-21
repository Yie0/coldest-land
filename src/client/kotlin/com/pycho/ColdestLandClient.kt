package com.pycho

import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import com.pycho.network.ComboInputPayload
import com.pycho.network.ModNetworking
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.chat.Component

object ColdestLandClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            if (!player.mainHandItem.`is`(ModItems.TEMPORAL_BLADE)) return@register

            val comboStep = TemporalBladeItem.getComboStep(player.mainHandItem) ?: return@register
            if (comboStep == 0.toByte()) return@register

            while (client.options.keyAttack.consumeClick()) {
                ClientPlayNetworking.send(ComboInputPayload(isRightClick = false))
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
            }

        }
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ALERT_PLAYERS_PACKET_ID) { payload, context ->
            context.player()?.displayClientMessage(
                    Component.literal(payload.msg),
                    true
            )
        }
    }



}