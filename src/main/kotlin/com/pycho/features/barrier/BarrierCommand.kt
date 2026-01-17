package com.pycho.features.barrier

import com.pycho.util.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.max
import kotlin.math.min

@PychoCommand
object BarrierCommand : CommandBranch(
    name = "barrier",
    description = "Barrier debug and management tools",
    category = "debug"
) {
    private val playerLastBarrier = mutableMapOf<UUID, UUID>()

    override fun CommandBuilder.register() {

        literal("conjure") {
            help = "Create barriers in various ways"

            // Conjure in front of player
            literal("front") {
                help = "Create barrier in front of you"
                executes { ctx ->
                    ctx.requirePlayer {
                        val distance = 3.0
                        val lookVec = lookAngle
                        val center = position().add(lookVec.scale(distance))

                        val barrier = BarrierData.create(
                            center = center,
                            ownerUUID = uuid,
                            creationTime = level().gameTime,
                            lifetime = 60, // 60 seconds
                            width = 3.0,
                            height = 3.0,
                            depth = 0.5,
                            yaw = yRot,
                            pitch = xRot,
                            precision = 0.25
                        )

                        BarrierManager.addBarrier(level() as ServerLevel, barrier)
                        playerLastBarrier[uuid] = barrier.id

                        ctx.success("§aCreated barrier §7${barrier.id.toString().substring(0, 8)}§a in front of you")
                    }
                    1
                }

                // With custom parameters
                argument("width", double(0.1, 100.0)) {
                    argument("height", double(0.1, 100.0)) {
                        argument("depth", double(0.1, 100.0)) {
                            argument("precision", double(0.01, 5.0)) {
                                executes { ctx, precision ->
                                    val depth = ctx.getDouble("depth")
                                    val height = ctx.getDouble("height")
                                    val width = ctx.getDouble("width")

                                    ctx.requirePlayer {
                                        val distance = 3.0
                                        val lookVec = lookAngle
                                        val center = position().add(lookVec.scale(distance))

                                        val barrier = BarrierData.create(
                                            center = center,
                                            ownerUUID = uuid,
                                            creationTime = level().gameTime,
                                            lifetime = 60,
                                            width = width,
                                            height = height,
                                            depth = depth,
                                            yaw = yRot,
                                            pitch = xRot,
                                            precision = precision
                                        )

                                        BarrierManager.addBarrier(level() as ServerLevel, barrier)
                                        playerLastBarrier[uuid] = barrier.id

                                        ctx.success("§aCreated barrier §7${barrier.id.toString().substring(0, 8)}")
                                        ctx.success("§7Dimensions: ${width}x${height}x${depth}, Precision: $precision")
                                    }
                                    1
                                }
                            }
                        }
                    }
                }
            }

            // Conjure at specific coordinates
            literal("at") {
                help = "Create barrier at coordinates {x,y,z}"
                argument("position", vec3()) {
                    executes { ctx, center ->
                        ctx.requirePlayer {
                            val barrier = BarrierData.create(
                                center = center,
                                ownerUUID = uuid,
                                creationTime = level().gameTime,
                                lifetime = 60,
                                width = 3.0,
                                height = 3.0,
                                depth = 0.5,
                                yaw = yRot,
                                pitch = xRot,
                                precision = 0.25
                            )

                            BarrierManager.addBarrier(level() as ServerLevel, barrier)
                            playerLastBarrier[uuid] = barrier.id

                            ctx.success("§aCreated barrier at (${center.x.toInt()}, ${center.y.toInt()}, ${center.z.toInt()})")
                        }
                        1
                    }
                }
            }

            // Conjure custom (full control)
            literal("custom") {
                help = "Create barrier with full control: position rotation dimensions precision lifetime"
                argument("position", vec3()) {
                    argument("yaw", float()) {
                        argument("pitch", float()) {
                            argument("dimensions", multiDouble(3, 3)) {
                                argument("precision", double(0.01, 5.0)) {
                                    argument("lifetime", int(1, 3600)) {
                                        executes { ctx, lifetime ->
                                            ctx.requirePlayer {
                                                val center = ctx.getVec3("position")
                                                val yaw = ctx.getFloat("yaw")
                                                val pitch = ctx.getFloat("pitch")
                                                val precision = ctx.getDouble("precision")
                                                val dims = ctx.getMultiDouble("dimensions")

                                                val barrier = BarrierData.create(
                                                    center = center,
                                                    ownerUUID = uuid,
                                                    creationTime = level().gameTime,
                                                    lifetime = lifetime.toLong(),
                                                    width = dims[0],
                                                    height = dims[1],
                                                    depth = dims[2],
                                                    yaw = yaw,
                                                    pitch = pitch,
                                                    precision = precision
                                                )

                                                BarrierManager.addBarrier(level() as ServerLevel, barrier)
                                                playerLastBarrier[uuid] = barrier.id

                                                ctx.success("§aCreated custom barrier §7${barrier.id.toString().substring(0, 8)}")
                                                ctx.success("§7Center: (${center.x.toInt()},${center.y.toInt()},${center.z.toInt()}), Rotation: ($yaw°, $pitch°)")
                                                ctx.success("§7Dimensions: ${dims[0]}x${dims[1]}x${dims[2]}, Lifetime: ${lifetime}s")
                                            }
                                            1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Conjure wall
            literal("wall") {
                help = "Create a wall barrier facing you"
                argument("width", double(1.0, 50.0)) {
                    argument("height", double(1.0, 50.0)) {
                        executes { ctx, height ->
                            val width = ctx.getDouble("width")

                            ctx.requirePlayer {
                                val distance = 3.0
                                val lookVec = lookAngle
                                val center = position().add(lookVec.scale(distance))

                                val barrier = BarrierData.create(
                                    center = center,
                                    ownerUUID = uuid,
                                    creationTime = level().gameTime,
                                    lifetime = 120,
                                    width = width,
                                    height = height,
                                    depth = 0.25,
                                    yaw = yRot,
                                    pitch = 0f,
                                    precision = 0.25
                                )

                                BarrierManager.addBarrier(level() as ServerLevel, barrier)
                                playerLastBarrier[uuid] = barrier.id

                                ctx.success("§aCreated wall barrier ${width}x${height}")
                            }
                            1
                        }
                    }
                }
            }

            // Conjure box
            literal("box") {
                help = "Create a box barrier around you"
                argument("size", double(1.0, 20.0)) {
                    executes { ctx, size ->
                        ctx.requirePlayer {
                            val center = position()

                            val barrier = BarrierData.create(
                                center = center,
                                ownerUUID = uuid,
                                creationTime = level().gameTime,
                                lifetime = 120,
                                width = size,
                                height = size,
                                depth = size,
                                yaw = 0f,
                                pitch = 0f,
                                precision = 0.5
                            )

                            BarrierManager.addBarrier(level() as ServerLevel, barrier)
                            playerLastBarrier[uuid] = barrier.id

                            ctx.success("§aCreated box barrier ${size}x${size}x${size}")
                        }
                        1
                    }
                }
            }
        }

        literal("remove") {
            help = "Remove barriers"

            // Remove last created
            literal("last") {
                help = "Remove your last created barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val barrierId = playerLastBarrier[uuid]
                        if (barrierId == null) {
                            ctx.failure("§cYou haven't created any barriers")
                            return@requirePlayer
                        }

                        BarrierManager.removeBarrier(level() as ServerLevel, barrierId)
                        playerLastBarrier.remove(uuid)
                        ctx.success("§aRemoved barrier §7${barrierId.toString().substring(0, 8)}")
                    }
                    1
                }
            }

            // Remove by ID
            literal("id") {
                help = "Remove barrier by UUID"
                argument("uuid", string()) {
                    suggests {
                        val barriers = BarrierManager.getBarriers()
                        val barrierIds: Array<String> = Array(barriers.size) { idx ->
                            barriers[idx].id.toString()
                        }
                        barrierIds
                    }
                    executes { ctx, uuidStr ->
                        ctx.requirePlayer {
                            val barrierId = try {
                                UUID.fromString(uuidStr)
                            } catch (e: IllegalArgumentException) {
                                ctx.failure("§cInvalid UUID format")
                                return@requirePlayer
                            }

                            BarrierManager.removeBarrier(level() as ServerLevel, barrierId)
                            ctx.success("§aRemoved barrier §7${barrierId.toString().substring(0, 8)}")
                        }
                        1
                    }
                }
            }

            // Remove nearby
            literal("nearby") {
                help = "Remove all barriers within radius"
                argument("radius", double(1.0, 100.0)) {
                    executes { ctx, radius ->
                        ctx.requirePlayer {
                            val center = position()
                            val area = AABB(
                                center.x - radius, center.y - radius, center.z - radius,
                                center.x + radius, center.y + radius, center.z + radius
                            )

                            val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, area)
                            val nearbyBarriers = barriers.filter {
                                center.distanceTo(Vec3(it.centerX, it.centerY, it.centerZ)) <= radius
                            }

                            nearbyBarriers.forEach { barrier ->
                                BarrierManager.removeBarrier(level() as ServerLevel, barrier.id)
                            }

                            ctx.success("§aRemoved ${nearbyBarriers.size} barrier(s) within $radius blocks")
                        }
                        1
                    }
                }
            }

            // Clear all
            literal("all") {
                help = "Remove ALL barriers in the world"
                executes { ctx ->
                    ctx.requirePlayer {
                        val level = level() as ServerLevel
                        val allBarriers = BarrierManager.getBarriersInArea(
                            level,
                            AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0)
                        )

                        allBarriers.forEach { barrier ->
                            BarrierManager.removeBarrier(level, barrier.id)
                        }

                        ctx.success("§aRemoved all ${allBarriers.size} barrier(s) from the world")
                    }
                    1
                }
            }
        }

        // LIST/INFO COMMANDS
        literal("list") {
            help = "List barriers"

            // List all nearby
            literal("nearby") {
                help = "List barriers within radius"
                argument("radius", double(1.0, 100.0)) {
                    executes { ctx, radius ->
                        ctx.requirePlayer {
                            val center = position()
                            val area = AABB(
                                center.x - radius, center.y - radius, center.z - radius,
                                center.x + radius, center.y + radius, center.z + radius
                            )

                            val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, area)
                            val nearbyBarriers = barriers.filter {
                                center.distanceTo(Vec3(it.centerX, it.centerY, it.centerZ)) <= radius
                            }

                            if (nearbyBarriers.isEmpty()) {
                                ctx.success("§7No barriers within $radius blocks")
                            } else {
                                ctx.success("§6=== Barriers within $radius blocks ===")
                                nearbyBarriers.forEach { barrier ->
                                    val dist = center.distanceTo(Vec3(barrier.centerX, barrier.centerY, barrier.centerZ))
                                    ctx.success("§7${barrier.id.toString().substring(0, 8)} §f- ${String.format("%.1f", dist)}m away")
                                }
                            }
                        }
                        1
                    }
                }
            }

            // List in area
            literal("area") {
                help = "List barriers in bounding box {minX,minY,minZ} {maxX,maxY,maxZ}"
                argument("min", vec3()) {
                    argument("max", vec3()) {
                        executes { ctx, max ->
                            val min = ctx.getVec3("min")

                            ctx.requirePlayer {
                                val area = AABB(
                                    min(min.x, max.x), min(min.y, max.y), min(min.z, max.z),
                                    max(min.x, max.x), max(min.y, max.y), max(min.z, max.z)
                                )

                                val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, area)

                                if (barriers.isEmpty()) {
                                    ctx.success("§7No barriers in specified area")
                                } else {
                                    ctx.success("§6=== Barriers in area ===")
                                    barriers.forEach { barrier ->
                                        ctx.success("§7${barrier.id.toString().substring(0, 8)} §fat (${barrier.centerX.toInt()}, ${barrier.centerY.toInt()}, ${barrier.centerZ.toInt()})")
                                    }
                                }
                            }
                            1
                        }
                    }
                }
            }
        }

        // INFO COMMANDS
        literal("info") {
            help = "Get barrier information"

            // Info about last
            literal("last") {
                help = "Get info about your last created barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val barrierId = playerLastBarrier[uuid]
                        if (barrierId == null) {
                            ctx.failure("§cYou haven't created any barriers")
                            return@requirePlayer
                        }

                        showBarrierInfo(ctx, this, barrierId)
                    }
                    1
                }
            }

            // Info by ID
            literal("id") {
                help = "Get info about barrier by UUID"
                argument("uuid", string()) {
                    executes { ctx, uuidStr ->
                        ctx.requirePlayer {
                            val barrierId = try {
                                UUID.fromString(uuidStr)
                            } catch (e: IllegalArgumentException) {
                                ctx.failure("§cInvalid UUID format")
                                return@requirePlayer
                            }

                            showBarrierInfo(ctx, this, barrierId)
                        }
                        1
                    }
                }
            }

            // Info about nearest
            literal("nearest") {
                help = "Get info about nearest barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val center = position()
                        val area = AABB(
                            center.x - 50, center.y - 50, center.z - 50,
                            center.x + 50, center.y + 50, center.z + 50
                        )

                        val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, area)
                        val nearest = barriers.minByOrNull {
                            center.distanceTo(Vec3(it.centerX, it.centerY, it.centerZ))
                        }

                        if (nearest == null) {
                            ctx.failure("§cNo barriers found nearby")
                            return@requirePlayer
                        }

                        showBarrierInfo(ctx, this, nearest.id)
                    }
                    1
                }
            }
        }

        // TELEPORT COMMANDS
        literal("teleport") {
            help = "Teleport to barriers"

            // TP to last
            literal("last") {
                help = "Teleport to your last created barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val barrierId = playerLastBarrier[uuid]
                        if (barrierId == null) {
                            ctx.failure("§cYou haven't created any barriers")
                            return@requirePlayer
                        }

                        teleportToBarrier(ctx, this, barrierId)
                    }
                    1
                }
            }

            // TP by ID
            literal("id") {
                help = "Teleport to barrier by UUID"
                argument("uuid", string()) {
                    executes { ctx, uuidStr ->
                        ctx.requirePlayer {
                            val barrierId = try {
                                UUID.fromString(uuidStr)
                            } catch (e: IllegalArgumentException) {
                                ctx.failure("§cInvalid UUID format")
                                return@requirePlayer
                            }

                            teleportToBarrier(ctx, this, barrierId)
                        }
                        1
                    }
                }
            }

            // TP to nearest
            literal("nearest") {
                help = "Teleport to nearest barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val center = position()
                        val area = AABB(
                            center.x - 100, center.y - 100, center.z - 100,
                            center.x + 100, center.y + 100, center.z + 100
                        )

                        val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, area)
                        val nearest = barriers.minByOrNull {
                            center.distanceTo(Vec3(it.centerX, it.centerY, it.centerZ))
                        }

                        if (nearest == null) {
                            ctx.failure("§cNo barriers found nearby")
                            return@requirePlayer
                        }

                        teleportToBarrier(ctx, this, nearest.id)
                    }
                    1
                }
            }
        }

        // TEST/DEBUG COMMANDS
        literal("test") {
            help = "Test barrier functionality"

            // Test collision
            literal("collision") {
                help = "Test if current position collides with any barrier"
                executes { ctx ->
                    ctx.requirePlayer {
                        val pos = position()
                        val testArea = AABB(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3)

                        val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, testArea)
                        val colliding = barriers.filter { barrier ->
                            barrier.boundingBoxes.any { it.intersects(testArea) }
                        }

                        if (colliding.isEmpty()) {
                            ctx.success("§aNo collision detected at current position")
                        } else {
                            ctx.success("§c§lCOLLISION DETECTED!")
                            ctx.success("§7Colliding with ${colliding.size} barrier(s):")
                            colliding.forEach { barrier ->
                                ctx.success("§7- ${barrier.id.toString().substring(0, 8)}")
                            }
                        }
                    }
                    1
                }
            }

            // Test area intersection
            literal("area") {
                help = "Test barrier intersection with area {minX,minY,minZ} {maxX,maxY,maxZ}"
                argument("min", vec3()) {
                    argument("max", vec3()) {
                        executes { ctx, max ->
                            val min = ctx.getVec3("min")

                            ctx.requirePlayer {
                                val testArea = AABB(
                                    min(min.x, max.x), min(min.y, max.y), min(min.z, max.z),
                                    max(min.x, max.x), max(min.y, max.y), max(min.z, max.z)
                                )

                                val barriers = BarrierManager.getBarriersInArea(level() as ServerLevel, testArea)

                                ctx.success("§6=== Area Intersection Test ===")
                                ctx.success("§7Found ${barriers.size} barrier(s) intersecting area")
                                barriers.forEach { barrier ->
                                    val intersections = barrier.boundingBoxes.count { it.intersects(testArea) }
                                    ctx.success("§7${barrier.id.toString().substring(0, 8)} - $intersections voxel(s)")
                                }
                            }
                            1
                        }
                    }
                }
            }

            // Performance test
            literal("performance") {
                help = "Run performance test with multiple barriers"
                argument("count", int(1, 100000)) {
                    executes { ctx, count ->
                        ctx.requirePlayer {
                            val startTime = System.nanoTime()
                            val center = position()
                            val createdBarriers = mutableListOf<UUID>()

                            repeat(count) { i ->
                                val angle = (i.toDouble() / count) * Math.PI * 2
                                val radius = 10.0
                                val x = center.x + Math.cos(angle) * radius
                                val z = center.z + Math.sin(angle) * radius

                                val barrier = BarrierData.create(
                                    center = Vec3(x, center.y, z),
                                    ownerUUID = uuid,
                                    creationTime = level().gameTime,
                                    lifetime = 30,
                                    width = 2.0,
                                    height = 2.0,
                                    depth = 0.5,
                                    yaw = (angle * 180 / Math.PI).toFloat(),
                                    pitch = 0f,
                                    precision = 0.25
                                )

                                BarrierManager.addBarrier(level() as ServerLevel, barrier)
                                createdBarriers.add(barrier.id)
                            }

                            val createTime = (System.nanoTime() - startTime) / 1_000_000.0

                            // Test query performance
                            val queryStart = System.nanoTime()
                            val testArea = AABB(
                                center.x - 15, center.y - 5, center.z - 15,
                                center.x + 15, center.y + 5, center.z + 15
                            )
                            val found = BarrierManager.getBarriersInArea(level() as ServerLevel, testArea)
                            val queryTime = (System.nanoTime() - queryStart) / 1_000_000.0

                            ctx.success("§6=== Performance Test Results ===")
                            ctx.success("§7Created: $count barriers in ${String.format("%.2f", createTime)}ms")
                            ctx.success("§7Query: Found ${found.size} barriers in ${String.format("%.2f", queryTime)}ms")
                            ctx.success("§7Avg Create: ${String.format("%.3f", createTime / count)}ms per barrier")
                        }
                        1
                    }
                }
            }
        }

        // UTILITY COMMANDS
        literal("stats") {
            help = "Show barrier statistics for current world"
            executes { ctx ->
                ctx.requirePlayer {
                    val level = level() as ServerLevel
                    val allBarriers = BarrierManager.getBarriersInArea(
                        level,
                        AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0)
                    )

                    val totalVoxels = allBarriers.sumOf { it.boundingBoxes.size }
                    val avgVoxels = if (allBarriers.isNotEmpty()) totalVoxels / allBarriers.size else 0

                    val now = level.gameTime
                    val expiring = allBarriers.filter { it.expirationTime - now <= 200 } // <10 seconds

                    ctx.success("§6=== Barrier Statistics ===")
                    ctx.success("§7Total Barriers: ${allBarriers.size}")
                    ctx.success("§7Total Voxels: $totalVoxels")
                    ctx.success("§7Avg Voxels/Barrier: $avgVoxels")
                    ctx.success("§7Expiring Soon: ${expiring.size}")
                }
                1
            }
        }
    }

    private fun showBarrierInfo(ctx: ModCommandContext, player: ServerPlayer, barrierId: UUID) {
        val level = player.level() as ServerLevel
        val area = AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0)
        val barrier = BarrierManager.getBarriersInArea(level, area).find { it.id == barrierId }

        if (barrier == null) {
            ctx.failure("§cBarrier not found (may have expired)")
            return
        }

        val now = level.gameTime
        val remaining = ((barrier.expirationTime - now) / 20.0)
        val distance = player.position().distanceTo(Vec3(barrier.centerX, barrier.centerY, barrier.centerZ))

        ctx.success("§6=== Barrier Info ===")
        ctx.success("§7ID: ${barrier.id}")
        ctx.success("§7Center: (${barrier.centerX.toInt()}, ${barrier.centerY.toInt()}, ${barrier.centerZ.toInt()})")
        ctx.success("§7Dimensions: ${barrier.width}x${barrier.height}x${barrier.depth}")
        ctx.success("§7Rotation: Yaw=${barrier.yaw}°, Pitch=${barrier.pitch}°")
        ctx.success("§7Precision: ${barrier.precision}")
        ctx.success("§7Voxel Count: ${barrier.boundingBoxes.size}")
        ctx.success("§7Lifetime: ${barrier.lifetime}s (${String.format("%.1f", remaining)}s remaining)")
        ctx.success("§7Distance: ${String.format("%.2f", distance)} blocks")
        ctx.success("§7Owner: ${barrier.ownerUUID}")
    }

    private fun teleportToBarrier(ctx: ModCommandContext, player: ServerPlayer, barrierId: UUID) {
        val level = player.level() as ServerLevel
        val area = AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0)
        val barrier = BarrierManager.getBarriersInArea(level, area).find { it.id == barrierId }

        if (barrier == null) {
            ctx.failure("§cBarrier not found (may have expired)")
            return
        }

        player.teleportTo(barrier.centerX, barrier.centerY, barrier.centerZ)
        ctx.success("§aTeleported to barrier §7${barrier.id.toString().substring(0, 8)}")
    }
}
