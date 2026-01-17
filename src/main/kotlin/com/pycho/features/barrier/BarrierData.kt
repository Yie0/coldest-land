package com.pycho.features.barrier

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix3d
import java.util.*
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BarrierData private constructor(
    @JvmField val id: UUID,
    @JvmField val centerX: Double,
    @JvmField val centerY: Double,
    @JvmField val centerZ: Double,
    @JvmField val width: Double,
    @JvmField val height: Double,
    @JvmField val depth: Double,
    @JvmField val yaw: Float,
    @JvmField val pitch: Float,
    @JvmField val precision: Double,
    @JvmField val lifetime: Long,
    @JvmField val ownerUUID: UUID,
    @JvmField val creationTime: Long,
    @JvmField val boundingBoxes: Array<AABB>,
    @JvmField val collisionShape: Array<VoxelShape>,
    @JvmField val corners: Array<Vec3>,
    @JvmField val radius: Double
) {
    @JvmField
    val expirationTime: Long = creationTime + lifetime * 20L

    inline fun isExpired(currentTime: Long): Boolean = currentTime > expirationTime

    override fun equals(other: Any?): Boolean = (this === other) || (other is BarrierData && other.id == id)
    override fun hashCode(): Int = id.hashCode()

    companion object {
        private const val HALF = 0.5

        private fun calculateRadius(width: Double, height: Double, depth: Double): Double {
            val hw = width * HALF
            val hh = height * HALF
            val hd = depth * HALF
            return sqrt(hw * hw + hh * hh + hd * hd)
        }

        private fun estimateVoxelCount(width: Double, height: Double, depth: Double, precision: Double): Int {
            val sx = ceil(width / precision).toInt()
            val sy = ceil(height / precision).toInt()
            val sz = ceil(depth / precision).toInt()
            val max = Int.MAX_VALUE * HALF
            return (sx.toLong() * sy.toLong() * sz.toLong()).coerceAtMost(max.toLong()).toInt()
        }

        private fun createRotationMatrix(yaw: Float, pitch: Float): Matrix3d {
            val yawRad = Math.toRadians(-yaw.toDouble())
            val pitchRad = Math.toRadians(pitch.toDouble())

            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)
            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)

            return Matrix3d(
                cosYaw, sinYaw * sinPitch, sinYaw * cosPitch,
                0.0, cosPitch, -sinPitch,
                -sinYaw, cosYaw * sinPitch, cosYaw * cosPitch
            )
        }

        private fun Matrix3d.transform(localX: Double, localY: Double, localZ: Double, center: Vec3): Vec3 {
            val rx = m00() * localX + m01() * localY + m02() * localZ + center.x
            val ry = m10() * localX + m11() * localY + m12() * localZ + center.y
            val rz = m20() * localX + m21() * localY + m22() * localZ + center.z
            return Vec3(rx, ry, rz)
        }

        private fun isWithinBounds(
            localX: Double, localY: Double, localZ: Double,
            halfWidth: Double, halfHeight: Double, halfDepth: Double
        ): Boolean {
            return localX in -halfWidth..halfWidth &&
                    localY in -halfHeight..halfHeight &&
                    localZ in -halfDepth..halfDepth
        }

        private fun generateVoxels(
            width: Double, height: Double, depth: Double,
            precision: Double, rotationMatrix: Matrix3d, center: Vec3
        ): Pair<Array<AABB>, Array<VoxelShape>> {
            val halfWidth = width * HALF
            val halfHeight = height * HALF
            val halfDepth = depth * HALF
            val halfVoxel = precision * HALF

            val stepsX = ceil(width / precision).toInt()
            val stepsY = ceil(height / precision).toInt()
            val stepsZ = ceil(depth / precision).toInt()

            val estimated = estimateVoxelCount(width, height, depth, precision)
            val boxList = ArrayList<AABB>(estimated)
            val shapeList = ArrayList<VoxelShape>(estimated)

            for (ix in 0 until stepsX) {
                val localX = -halfWidth + (ix + 0.5) * precision

                for (iy in 0 until stepsY) {
                    val localY = -halfHeight + (iy + 0.5) * precision

                    for (iz in 0 until stepsZ) {
                        val localZ = -halfDepth + (iz + 0.5) * precision

                        if (isWithinBounds(localX, localY, localZ, halfWidth, halfHeight, halfDepth)) {
                            val worldPos = rotationMatrix.transform(localX, localY, localZ, center)

                            val aabb = AABB(
                                worldPos.x - halfVoxel, worldPos.y - halfVoxel, worldPos.z - halfVoxel,
                                worldPos.x + halfVoxel, worldPos.y + halfVoxel, worldPos.z + halfVoxel
                            )

                            boxList.add(aabb)
                            shapeList.add(Shapes.create(aabb))
                        }
                    }
                }
            }

            return Pair(boxList.toTypedArray(), shapeList.toTypedArray())
        }

        private fun generateCorners(
            width: Double, height: Double, depth: Double,
            rotationMatrix: Matrix3d, center: Vec3
        ): Array<Vec3> {
            val halfWidth = width * HALF
            val halfHeight = height * HALF
            val halfDepth = depth * HALF

            val corners = Array(8) { Vec3.ZERO }
            val offsets = arrayOf(
                doubleArrayOf(-halfWidth, halfWidth),
                doubleArrayOf(-halfHeight, halfHeight),
                doubleArrayOf(-halfDepth, halfDepth)
            )

            var index = 0
            for (xi in 0..1) {
                for (yi in 0..1) {
                    for (zi in 0..1) {
                        val localX = offsets[0][xi]
                        val localY = offsets[1][yi]
                        val localZ = offsets[2][zi]
                        corners[index++] = rotationMatrix.transform(localX, localY, localZ, center)
                    }
                }
            }

            return corners
        }

        fun create(
            center: Vec3,
            ownerUUID: UUID,
            creationTime: Long,
            lifetime: Long,
            width: Double,
            height: Double,
            depth: Double,
            yaw: Float,
            pitch: Float,
            precision: Double
        ): BarrierData {
            val rotationMatrix = createRotationMatrix(yaw, pitch)
            val (boundingBoxes, collisionShapes) = generateVoxels(width, height, depth, precision, rotationMatrix, center)
            val corners = generateCorners(width, height, depth, rotationMatrix, center)

            return BarrierData(
                id = UUID.randomUUID(),
                centerX = center.x,
                centerY = center.y,
                centerZ = center.z,
                width = width,
                height = height,
                depth = depth,
                yaw = yaw,
                pitch = pitch,
                precision = precision,
                lifetime = lifetime,
                ownerUUID = ownerUUID,
                creationTime = creationTime,
                boundingBoxes = boundingBoxes,
                collisionShape = collisionShapes,
                corners = corners,
                radius = calculateRadius(width, height, depth)
            )
        }
    }
}
