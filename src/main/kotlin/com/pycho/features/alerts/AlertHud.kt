package com.pycho.features.alerts

import com.pycho.ColdestLand
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Util

@Environment(EnvType.CLIENT)
object AlertHud{
    var alerts: MutableList<Alert> = mutableListOf()

    fun register(){
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ColdestLand.id("alerts_hud")) { context, tickDelta ->
            render(context, tickDelta)
        }
    }
    fun render(context: GuiGraphics, tickDelta: DeltaTracker) {
        val currentTime = Util.getMillis()
        val fadeDuration = 500

        alerts.removeIf { alert ->
            currentTime - alert.creationTime > alert.lifetime + fadeDuration
        }

        for (alert in alerts) {
            val age = currentTime - alert.creationTime

            context.pose().pushMatrix()
            context.pose().scale(alert.scale.toFloat())

            var posX = context.guiWidth() / alert.scale / 2
            var posY = context.guiHeight() / alert.scale / 2

            val alpha = if (age <= alert.lifetime) {
                1.0f
            } else {
                val fadeProgress = (age - alert.lifetime).toFloat() / fadeDuration
                1.0f - fadeProgress.coerceIn(0f, 1f)
            }

            if (alpha <= 0.001f) continue

            val color = (alpha * 255).toInt() shl 24 or 0xFFFFFF

            val font = Minecraft.getInstance().font

            context.drawCenteredString(
                font,
                alert.component,
                posX.toInt(),
                posY.toInt(),
                color
            )


            posY -= 20 * alert.scale
            context.pose().popMatrix()
        }
    }
}
