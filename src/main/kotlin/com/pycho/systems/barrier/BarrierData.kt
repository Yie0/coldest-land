package com.pycho.systems.barrier

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.*

data class BarrierData(
    val id: UUID,
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val radius: Float,
    val height: Float,
    val ownerUUID: UUID,
    val creationTime: Long,
    val boundingBox: AABB,
    val collisionShape: VoxelShape
) {
    constructor(
        center: BlockPos,
        ownerUUID: UUID,
        creationTime: Long,
        radius: Float = 2.5f,
        height: Float = 3.0f
    ) : this(
        id = UUID.randomUUID(),
        centerX = center.x + 0.5,
        centerY = center.y + 0.5,
        centerZ = center.z + 0.5,
        radius = radius,
        height = height,
        ownerUUID = ownerUUID,
        creationTime = creationTime,
        boundingBox = AABB(
            center.x + 0.5 - radius,
            center.y + 0.5,
            center.z + 0.5 - radius,
            center.x + 0.5 + radius,
            center.y + 0.5 + height,
            center.z + 0.5 + radius
        ),
        collisionShape = Shapes.create(
            AABB(
                center.x + 0.5 - radius,
                center.y + 0.5,
                center.z + 0.5 - radius,
                center.x + 0.5 + radius,
                center.y + 0.5 + height,
                center.z + 0.5 + radius
            )
        )
    )
    
    fun isExpired(currentTime: Long): Boolean {
        return currentTime - creationTime > 5000 // 5 seconds
    }
    
}
