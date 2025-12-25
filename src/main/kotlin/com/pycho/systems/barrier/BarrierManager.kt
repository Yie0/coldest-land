package com.pycho.systems.barrier

import com.pycho.network.AlertPayload
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.AABB
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

object BarrierManager {
    private val barriersByWorld = mutableMapOf<ServerLevel, WorldBarrierData>()

    private class WorldBarrierData {
        val barriersById = mutableMapOf<UUID, BarrierData>()
        val barriersByChunk = Long2ObjectOpenHashMap<MutableSet<BarrierData>>()

        // Thread-local reusable collections to avoid allocations
        private val tempBarrierList = ThreadLocal.withInitial { mutableListOf<BarrierData>() }

        fun addBarrier(barrier: BarrierData) {
            barriersById[barrier.id] = barrier

            val minChunkX = floor((barrier.centerX - barrier.radius) / 16).toInt()
            val maxChunkX = ceil((barrier.centerX + barrier.radius) / 16).toInt()
            val minChunkZ = floor((barrier.centerZ - barrier.radius) / 16).toInt()
            val maxChunkZ = ceil((barrier.centerZ + barrier.radius) / 16).toInt()

            for (chunkX in minChunkX..maxChunkX) {
                for (chunkZ in minChunkZ..maxChunkZ) {
                    val chunkKey = SectionPos.asLong(chunkX, 0, chunkZ)
                    barriersByChunk
                        .getOrPut(chunkKey) { mutableSetOf() }
                        .add(barrier)
                }
            }
        }

        fun removeBarrier(barrierId: UUID) {
            val barrier = barriersById.remove(barrierId) ?: return

            val minChunkX = floor((barrier.centerX - barrier.radius) / 16).toInt()
            val maxChunkX = ceil((barrier.centerX + barrier.radius) / 16).toInt()
            val minChunkZ = floor((barrier.centerZ - barrier.radius) / 16).toInt()
            val maxChunkZ = ceil((barrier.centerZ + barrier.radius) / 16).toInt()

            for (chunkX in minChunkX..maxChunkX) {
                for (chunkZ in minChunkZ..maxChunkZ) {
                    val chunkKey = SectionPos.asLong(chunkX, 0, chunkZ)
                    barriersByChunk[chunkKey]?.remove(barrier)
                }
            }
        }

        fun getBarriersInArea(area: AABB): List<BarrierData> {
            val result = tempBarrierList.get()
            result.clear()

            val minChunkX = floor(area.minX / 16).toInt()
            val maxChunkX = ceil(area.maxX / 16).toInt()
            val minChunkZ = floor(area.minZ / 16).toInt()
            val maxChunkZ = ceil(area.maxZ / 16).toInt()

            for (chunkX in minChunkX..maxChunkX) {
                for (chunkZ in minChunkZ..maxChunkZ) {
                    val chunkKey = SectionPos.asLong(chunkX, 0, chunkZ)
                    barriersByChunk[chunkKey]?.forEach { barrier ->
                        if (barrier.boundingBox.intersects(area)) {
                            result.add(barrier)
                        }
                    }
                }
            }

            return result
        }

        fun tick(currentTime: Long) {
            val iterator = barriersById.iterator()
            while (iterator.hasNext()) {
                val barrier = iterator.next().value
                if (barrier.isExpired(currentTime)) {
                    removeBarrier(barrier.id)
                    iterator.remove()
                }
            }
        }
    }

    fun addBarrier(level: ServerLevel, barrier: BarrierData) {
        barriersByWorld.getOrPut(level) { WorldBarrierData() }.addBarrier(barrier)
    }

    fun removeBarrier(level: ServerLevel, barrierId: UUID) {
        barriersByWorld[level]?.removeBarrier(barrierId)
    }

    fun getBarriersInArea(level: ServerLevel, area: AABB): List<BarrierData> {
        return barriersByWorld[level]?.getBarriersInArea(area) ?: emptyList()
    }

    fun tickLevel(level: ServerLevel, currentTime: Long) {
        barriersByWorld[level]?.tick(currentTime)
    }
}