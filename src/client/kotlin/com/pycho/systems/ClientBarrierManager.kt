package com.pycho.client.systems

import com.pycho.systems.barrier.BarrierData
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ClientBarrierManager {
    private val barriers = ConcurrentHashMap<UUID, ClientBarrier>()

    fun upsert(barrierData: BarrierData) {
        barriers.computeIfAbsent(barrierData.id) {
            ClientBarrier(barrierData)
        }
    }

    fun tick(gameTime: Long) {
        val iterator = barriers.values.iterator()
        while (iterator.hasNext()) {
            val barrier = iterator.next()

            if (barrier.data.isExpired(gameTime)) {
                barrier.markExpired(gameTime)
            }

            if (barrier.isFullyCollapsed(gameTime)) {
                iterator.remove()
            }
        }
    }

    fun getBarriersInArea(level: Level, area: AABB) : List<BarrierData>{
        val result: MutableList<BarrierData> = mutableListOf()
        for (barrier in barriers.values) {
            if(barrier.data.boundingBoxes.any { it.intersects(area) }) {
                 result.add(barrier.data)
            }
        }
        return result
    }

    fun all(): Collection<ClientBarrier> = barriers.values
}
