package com.pycho.features.barrier

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.pycho.ColdestLand
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import kotlin.jvm.JvmStatic
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MappableRingBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.util.Util
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import java.util.OptionalDouble
import java.util.OptionalInt

@Environment(EnvType.CLIENT)
object BarrierRenderer{

    val PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(ColdestLand.id("pipeline/barrier_pipeline"))
            .withVertexShader(ColdestLand.id("barrier"))
            .withFragmentShader(ColdestLand.id("barrier"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .build()
    )

    private val allocator = ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE)
    private var buffer: BufferBuilder? = null
    private var vertexBuffer: MappableRingBuffer? = null

    private const val debugCollisionShapes = false

    fun render(context: WorldRenderContext) {
        if(extract(context))
            draw(Minecraft.getInstance())
    }
    private fun extract(context: WorldRenderContext) : Boolean {
        if(ClientBarrierManager.all().isEmpty())
            return false
        val matrices = context.matrices()
        val camera: Vec3 = context.worldState().cameraRenderState.pos

        matrices.pushPose()
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = BufferBuilder(
                allocator,
                PIPELINE.vertexFormatMode,
                PIPELINE.vertexFormat
            )
        }

        ClientBarrierManager.all().forEach { barrier ->
            if (debugCollisionShapes) {
                barrier.data.boundingBoxes.forEachIndexed { index, aabb ->
                    val hue = (index * 0.1f) % 1.0f
                    val (r, g, b) = hsvToRgb(hue, 0.7f, 0.9f)
                    addAABB(matrices, buffer!!, aabb, r, g, b, 0.3f)
                }
            } else {
                addBox(
                    matrices,
                    buffer!!,
                    barrier.data.corners,
                    0.7f, 0.8f, 1.0f, 1f-barrier.collapseProgress(context.worldState().gameTime)
                )
            }
        }

        matrices.popPose()
        return true
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Float, Float, Float> {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h * 6) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when ((h * 6).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Triple(r1 + m, g1 + m, b1 + m)
    }

    private fun addAABB(
        matrices: PoseStack,
        buffer: BufferBuilder,
        aabb: AABB,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val pose = matrices.last().pose()

        val minX = aabb.minX.toFloat()
        val minY = aabb.minY.toFloat()
        val minZ = aabb.minZ.toFloat()
        val maxX = aabb.maxX.toFloat()
        val maxY = aabb.maxY.toFloat()
        val maxZ = aabb.maxZ.toFloat()

        fun v(x: Float, y: Float, z: Float) {
            buffer.addVertex(pose, x, y, z).setColor(r, g, b, a)
        }

        // Front face
        v(minX, minY, minZ); v(maxX, minY, minZ); v(maxX, maxY, minZ)
        v(minX, minY, minZ); v(maxX, maxY, minZ); v(minX, maxY, minZ)

        // Back face
        v(maxX, minY, maxZ); v(minX, minY, maxZ); v(minX, maxY, maxZ)
        v(maxX, minY, maxZ); v(minX, maxY, maxZ); v(maxX, maxY, maxZ)

        // Left face
        v(minX, minY, maxZ); v(minX, minY, minZ); v(minX, maxY, minZ)
        v(minX, minY, maxZ); v(minX, maxY, minZ); v(minX, maxY, maxZ)

        // Right face
        v(maxX, minY, minZ); v(maxX, minY, maxZ); v(maxX, maxY, maxZ)
        v(maxX, minY, minZ); v(maxX, maxY, maxZ); v(maxX, maxY, minZ)

        // Bottom face
        v(minX, minY, maxZ); v(maxX, minY, maxZ); v(maxX, minY, minZ)
        v(minX, minY, maxZ); v(maxX, minY, minZ); v(minX, minY, minZ)

        // Top face
        v(minX, maxY, minZ); v(maxX, maxY, minZ); v(maxX, maxY, maxZ)
        v(minX, maxY, minZ); v(maxX, maxY, maxZ); v(minX, maxY, maxZ)
    }

    private fun addBox(
        matrices: PoseStack,
        buffer: BufferBuilder,
        corners: Array<Vec3>,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val pose = matrices.last().pose()

        fun vertex(corner: Vec3) {
            buffer.addVertex(pose, corner.x.toFloat(), corner.y.toFloat(), corner.z.toFloat()).setColor(r,g,b,a)
        }

        vertex(corners[0])
        vertex(corners[1])
        vertex(corners[3])
        vertex(corners[0])
        vertex(corners[3])
        vertex(corners[2])

        // Back face (5,4,6,7)
        vertex(corners[5])
        vertex(corners[4])
        vertex(corners[6])
        vertex(corners[5])
        vertex(corners[6])
        vertex(corners[7])

        // Left face (4,0,2,6)
        vertex(corners[4])
        vertex(corners[0])
        vertex(corners[2])
        vertex(corners[4])
        vertex(corners[2])
        vertex(corners[6])

        // Right face (1,5,7,3)
        vertex(corners[1])
        vertex(corners[5])
        vertex(corners[7])
        vertex(corners[1])
        vertex(corners[7])
        vertex(corners[3])

        // Bottom face (4,5,1,0)
        vertex(corners[4])
        vertex(corners[5])
        vertex(corners[1])
        vertex(corners[4])
        vertex(corners[1])
        vertex(corners[0])

        // Top face (2,3,7,6)
        vertex(corners[2])
        vertex(corners[3])
        vertex(corners[7])
        vertex(corners[2])
        vertex(corners[7])
        vertex(corners[6])
    }


    private fun draw(client: Minecraft) {
        val localBuffer = buffer ?: return

        val mesh: MeshData = localBuffer.buildOrThrow()
        val drawState = mesh.drawState()
        val format = drawState.format()

        val vertices = upload(drawState, format, mesh)

        executeDraw(client, mesh, drawState, vertices, format)

        vertexBuffer!!.rotate()
        buffer = null
    }

    private fun upload(
        drawState: MeshData.DrawState,
        format: VertexFormat,
        mesh: MeshData
    ): GpuBuffer {
        val size = drawState.vertexCount() * format.vertexSize

        if (vertexBuffer == null || vertexBuffer!!.size() < size) {
            vertexBuffer = MappableRingBuffer(
                { "coldestland pycho render pipeline" },
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE,
                size
            )
        }

        val encoder: CommandEncoder = RenderSystem.getDevice().createCommandEncoder()
        val slice = vertexBuffer!!.currentBuffer().slice(0, mesh.vertexBuffer().remaining().toLong())

        encoder.mapBuffer(slice, false, true).use { mapped ->
            MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data())
        }

        return vertexBuffer!!.currentBuffer()
    }

    private fun executeDraw(
        client: Minecraft,
        mesh: MeshData,
        drawState: MeshData.DrawState,
        vertices: GpuBuffer,
        format: VertexFormat
    ) {
        val seq = RenderSystem.getSequentialBuffer(PIPELINE.vertexFormatMode)
        val indices = seq.getBuffer(drawState.indexCount())
        val indexType = seq.type()

        val cameraPos = client.gameRenderer.mainCamera.position()

        val camX = (cameraPos.x % 100000.0).toFloat()
        val camY = (cameraPos.y % 100000.0).toFloat()
        val camZ = (cameraPos.z % 100000.0).toFloat()

        val transforms: GpuBufferSlice =
            RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                Vector4f(camX, camY, camZ, 1f),
                Vector3f(Util.getMillis().toFloat(),0f,0f),
                Matrix4f()
            )

        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                { "coldestland pycho render pipeline rendering" },
                client.mainRenderTarget.colorTextureView!!,
                OptionalInt.empty(),
                client.mainRenderTarget.depthTextureView,
                OptionalDouble.empty()
            ).use { pass ->
                pass.setPipeline(PIPELINE)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("DynamicTransforms", transforms)
                pass.setVertexBuffer(0, vertices)
                pass.setIndexBuffer(indices, indexType)
                pass.drawIndexed(0, 0, drawState.indexCount(), 1)
            }

        mesh.close()
    }

    @JvmStatic
    fun close() {
        allocator.close()
        vertexBuffer?.close()
        vertexBuffer = null
    }
}
