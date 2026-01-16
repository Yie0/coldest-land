package com.pycho.commands

import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.pycho.ColdestLand
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture

class Vec3ArgumentType : ArgumentType<Vec3> {

    override fun parse(reader: StringReader): Vec3 {
        try {
            val string = reader.readString()
            val cleaned = string.replace("{", "").replace("}", "")
            val split = cleaned.split(",")

            if (split.size != 3) {
                throw INVALID_FORMAT.create()
            }

            val x = split[0].trim().toDouble()
            val y = split[1].trim().toDouble()
            val z = split[2].trim().toDouble()

            return Vec3(x, y, z)
        } catch (e: NumberFormatException) {
            throw INVALID_FORMAT.create()
        } catch (e: CommandSyntaxException) {
            throw e
        } catch (e: Exception) {
            throw INVALID_FORMAT.create()
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (builder.remaining.isEmpty()) {
            builder.suggest("\"{0,0,0}\"")
        }
        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    companion object {
        private val EXAMPLES = listOf("\"{0,0,0}\"", "\"{10.5,64,20.3}\"", "\"{-100,70,200}\"")
        private val INVALID_FORMAT = SimpleCommandExceptionType(
            Component.literal("Invalid Vec3 format. Expected \"{x,y,z}\"")
        )
    }
}

class MultiIntArgumentType(val minCount: Int = 1, val maxCount: Int = Int.MAX_VALUE) : ArgumentType<List<Int>> {

    override fun parse(reader: StringReader): List<Int> {
        try {
            val string = reader.readString()
            val cleaned = string.replace("{", "").replace("}", "").trim()

            if (cleaned.isEmpty()) {
                throw EMPTY_LIST.create()
            }

            val values = cleaned.split(",").map { it.trim().toInt() }

            if (values.size < minCount) {
                throw TOO_FEW_VALUES.create()
            }
            if (values.size > maxCount) {
                throw TOO_MANY_VALUES.create()
            }

            return values
        } catch (e: NumberFormatException) {
            throw INVALID_FORMAT.create()
        } catch (e: CommandSyntaxException) {
            throw e
        } catch (e: Exception) {
            throw INVALID_FORMAT.create()
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (builder.remaining.isEmpty()) {
            builder.suggest("\"{1,2,3}\"")
        }
        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    companion object {
        private val EXAMPLES = listOf("\"{1,2,3}\"", "\"{10,20,30,40}\"", "\"{-5,0,5}\"")
        private val INVALID_FORMAT = SimpleCommandExceptionType(
            Component.literal("Invalid multi-int format. Expected \"{int,int,int,...}\"")
        )
        private val EMPTY_LIST = SimpleCommandExceptionType(
            Component.literal("Multi-int list cannot be empty")
        )
        private val TOO_FEW_VALUES = SimpleCommandExceptionType(
            Component.literal("Too few values provided")
        )
        private val TOO_MANY_VALUES = SimpleCommandExceptionType(
            Component.literal("Too many values provided")
        )
    }
}

class MultiDoubleArgumentType(val minCount: Int = 1, val maxCount: Int = Int.MAX_VALUE) : ArgumentType<List<Double>> {

    override fun parse(reader: StringReader): List<Double> {
        try {
            val string = reader.readString()
            val cleaned = string.replace("{", "").replace("}", "").trim()

            if (cleaned.isEmpty()) {
                throw EMPTY_LIST.create()
            }

            val values = cleaned.split(",").map { it.trim().toDouble() }

            if (values.size < minCount) {
                throw TOO_FEW_VALUES.create()
            }
            if (values.size > maxCount) {
                throw TOO_MANY_VALUES.create()
            }

            return values
        } catch (e: NumberFormatException) {
            throw INVALID_FORMAT.create()
        } catch (e: CommandSyntaxException) {
            throw e
        } catch (e: Exception) {
            throw INVALID_FORMAT.create()
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if (builder.remaining.isEmpty()) {
            builder.suggest("\"{1.0,2.0,3.0}\"")
        }
        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    companion object {
        private val EXAMPLES = listOf("\"{1.5,2.3,3.7}\"", "\"{10.0,20.5,30.2}\"", "\"{-5.5,0.0,5.5}\"")
        private val INVALID_FORMAT = SimpleCommandExceptionType(
            Component.literal("Invalid multi-double format. Expected \"{double,double,double,...}\"")
        )
        private val EMPTY_LIST = SimpleCommandExceptionType(
            Component.literal("Multi-double list cannot be empty")
        )
        private val TOO_FEW_VALUES = SimpleCommandExceptionType(
            Component.literal("Too few values provided")
        )
        private val TOO_MANY_VALUES = SimpleCommandExceptionType(
            Component.literal("Too many values provided")
        )
    }
}

class MultiIntArgumentInfo : ArgumentTypeInfo<MultiIntArgumentType, MultiIntArgumentInfo.Template> {

    override fun serializeToNetwork(template: Template, buffer: FriendlyByteBuf) {
        buffer.writeInt(template.minCount)
        buffer.writeInt(template.maxCount)
    }

    override fun deserializeFromNetwork(buffer: FriendlyByteBuf): Template {
        return Template(buffer.readInt(), buffer.readInt())
    }

    override fun serializeToJson(template: Template, json: JsonObject) {
        json.addProperty("min", template.minCount)
        json.addProperty("max", template.maxCount)
    }

    override fun unpack(type: MultiIntArgumentType): Template {
        return Template(type.minCount, type.maxCount)
    }

    inner class Template(val minCount: Int, val maxCount: Int) : ArgumentTypeInfo.Template<MultiIntArgumentType> {
        override fun instantiate(commandBuildContext: net.minecraft.commands.CommandBuildContext): MultiIntArgumentType {
            return MultiIntArgumentType(minCount, maxCount)
        }

        override fun type(): ArgumentTypeInfo<MultiIntArgumentType, *> {
            return this@MultiIntArgumentInfo
        }
    }
}

class MultiDoubleArgumentInfo : ArgumentTypeInfo<MultiDoubleArgumentType, MultiDoubleArgumentInfo.Template> {

    override fun serializeToNetwork(template: Template, buffer: FriendlyByteBuf) {
        buffer.writeInt(template.minCount)
        buffer.writeInt(template.maxCount)
    }

    override fun deserializeFromNetwork(buffer: FriendlyByteBuf): Template {
        return Template(buffer.readInt(), buffer.readInt())
    }

    override fun serializeToJson(template: Template, json: JsonObject) {
        json.addProperty("min", template.minCount)
        json.addProperty("max", template.maxCount)
    }

    override fun unpack(type: MultiDoubleArgumentType): Template {
        return Template(type.minCount, type.maxCount)
    }

    inner class Template(val minCount: Int, val maxCount: Int) : ArgumentTypeInfo.Template<MultiDoubleArgumentType> {
        override fun instantiate(commandBuildContext: net.minecraft.commands.CommandBuildContext): MultiDoubleArgumentType {
            return MultiDoubleArgumentType(minCount, maxCount)
        }

        override fun type(): ArgumentTypeInfo<MultiDoubleArgumentType, *> {
            return this@MultiDoubleArgumentInfo
        }
    }
}

object CommandArguments {

    fun register() {
        ArgumentTypeRegistry.registerArgumentType(
            ColdestLand.id("vec3"),
            Vec3ArgumentType::class.java,
            SingletonArgumentInfo.contextFree { Vec3ArgumentType() }
        )

        ArgumentTypeRegistry.registerArgumentType(
            ColdestLand.id("multi_int"),
            MultiIntArgumentType::class.java,
            MultiIntArgumentInfo()
        )

        ArgumentTypeRegistry.registerArgumentType(
            ColdestLand.id("multi_double"),
            MultiDoubleArgumentType::class.java,
            MultiDoubleArgumentInfo()
        )
    }
}

fun vec3() = Vec3ArgumentType()
fun multiInt(minCount: Int = 1, maxCount: Int = Int.MAX_VALUE) = MultiIntArgumentType(minCount, maxCount)
fun multiDouble(minCount: Int = 1, maxCount: Int = Int.MAX_VALUE) = MultiDoubleArgumentType(minCount, maxCount)
