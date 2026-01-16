package com.pycho.client.systems

import com.pycho.systems.barrier.BarrierData
import net.minecraft.util.Mth

class ClientBarrier(
    val data: BarrierData
) {
    var collapseStartTime: Long? = null
        private set

    fun markExpired(gameTime: Long) {
        if (collapseStartTime == null) {
            collapseStartTime = gameTime
        }
    }

    fun collapseProgress(gameTime: Long): Float {
        val start = collapseStartTime ?: return 0f
        val t = (gameTime - start) / 20f
        return Mth.clamp(t, 0f, 1f)
    }

    fun isFullyCollapsed(gameTime: Long): Boolean {
        return collapseProgress(gameTime) >= 1f //0 is fresh 1 is collapsed
    }
}
