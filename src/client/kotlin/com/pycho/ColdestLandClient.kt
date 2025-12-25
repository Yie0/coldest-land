package com.pycho

import com.mojang.authlib.minecraft.client.MinecraftClient
import com.pycho.gui.AlertHud
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import com.pycho.network.ComboInputPayload
import com.pycho.network.ModNetworking
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft


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
        AlertHud.register()

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ALERT_PACKET_ID) { payload, context ->
            AlertHud.alerts.addLast(payload.alert)
        }
    }



}