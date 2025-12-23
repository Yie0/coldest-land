package com.pycho.gui

import Alert
import com.pycho.ColdestLand
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import net.minecraft.world.entity.player.Player

object AlertHud{
    var alerts: MutableList<Alert> = mutableListOf()

    fun register(){
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ColdestLand.id("alerts_hud")) { context, tickDelta ->
            render(context, tickDelta)
        }
    }
    fun render(context: GuiGraphics, tickDelta: DeltaTracker) {
        var posX = context.guiWidth() / 2
        var posY = context.guiHeight() / 2


        val currentTime = Util.getMillis()
        val fadeDuration = 500


        alerts.removeIf { alert ->
            currentTime - alert.creationTime > alert.lifetime + fadeDuration
        }

        for (alert in alerts) {
            val age = currentTime - alert.creationTime


            val alpha = if (age <= alert.lifetime) {
                1.0f
            } else {
                val fadeProgress = (age - alert.lifetime).toFloat() / fadeDuration
                1.0f - fadeProgress.coerceIn(0f, 1f)
            }

            if (alpha <= 0.001f) continue

            val color = (alpha * 255).toInt() shl 24 or 0xFFFFFF

            context.drawCenteredString(
                Minecraft.getInstance().font,
                alert.component,
                posX,
                posY,
                color
            )

            posY -= 20
        }
    }
}