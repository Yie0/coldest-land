package com.pycho.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.pycho.ColdestLand
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CompletableFuture

// Command argument types
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

// Command framework
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PychoCommand

object CommandAutoLoader {
    private data class ScanResult(
        val loaded: List<String>,
        val skipped: List<String>,
        val errors: List<String>
    )

    fun loadPackage(packageName: String) {
        val result = scanPackage(packageName)
        printReport(packageName, result)
    }

    private fun scanPackage(packageName: String): ScanResult {
        val packagePath = packageName.replace('.', '/')
        val classLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        val resources = classLoader.getResources(packagePath).toList()

        val loaded = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val errors = mutableListOf<String>()

        resources.forEach { url ->
            runCatching {
                val classNames = when (url.protocol) {
                    "file" -> scanFileSystem(url, packagePath, packageName)
                    "jar" -> scanJarFile(url, packagePath)
                    else -> emptyList()
                }

                classNames.forEach { className ->
                    when (loadClassIfAnnotated(className)) {
                        LoadResult.Loaded -> loaded.add(className)
                        LoadResult.Skipped -> skipped.add(className)
                        is LoadResult.Error -> errors.add(className)
                    }
                }
            }.onFailure { throwable ->
                errors.add("Failed to scan $url: ${throwable.message}")
            }
        }

        return ScanResult(loaded, skipped, errors)
    }

    private fun scanFileSystem(url: java.net.URL, packagePath: String, packageName: String): List<String> {
        val dir = java.io.File(url.toURI())
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { file ->
                val relativePath = file.relativeTo(dir).path.replace(java.io.File.separatorChar, '/')
                "$packageName.${relativePath.removeSuffix(".class")}".replace('/', '.')
            }
            .toList()
    }

    private fun scanJarFile(url: java.net.URL, packagePath: String): List<String> {
        val jarPath = url.path.substringBefore("!").removePrefix("file:")
        val jarFile = java.util.jar.JarFile(jarPath)

        return jarFile.entries().toList()
            .filter { it.name.startsWith(packagePath) && it.name.endsWith(".class") }
            .map { it.name.removeSuffix(".class").replace('/', '.') }
    }

    private sealed class LoadResult {
        object Loaded : LoadResult()
        object Skipped : LoadResult()
        data class Error(val message: String) : LoadResult()
    }

    private fun loadClassIfAnnotated(className: String): LoadResult {
        return runCatching {
            val clazz = Class.forName(className, false, Thread.currentThread().contextClassLoader)
            if (clazz.isAnnotationPresent(PychoCommand::class.java)) {
                Class.forName(className)
                LoadResult.Loaded
            } else {
                LoadResult.Skipped
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is ClassNotFoundException, is NoClassDefFoundError -> LoadResult.Skipped
                else -> LoadResult.Error(throwable.message ?: "Unknown error")
            }
        }
    }

    private fun printReport(packageName: String, result: ScanResult) {
        println("\n[CommandAutoLoader] Package: $packageName")
        println("[CommandAutoLoader] ✓ Loaded: ${result.loaded.size} commands")
        println("[CommandAutoLoader] ⊘ Skipped: ${result.skipped.size} classes")
        println("[CommandAutoLoader] ✗ Errors: ${result.errors.size}")

        if (result.loaded.isNotEmpty()) {
            println("\n[CommandAutoLoader] Loaded commands:")
            result.loaded.forEach { println("  • $it") }
        }

        if (result.errors.isNotEmpty()) {
            println("\n[CommandAutoLoader] Errors:")
            result.errors.take(5).forEach { println("  ✗ $it") }
            if (result.errors.size > 5) {
                println("  ... and ${result.errors.size - 5} more")
            }
        }
    }
}

object CommandRegistry {
    private const val ROOT = "coldest"
    private val branches = mutableMapOf<String, MutableList<CommandBranch>>()

    init {
        branches["debug"] = mutableListOf()
        branches["exec"] = mutableListOf()
    }

    fun setup() {
        CommandArguments.register()

        CommandAutoLoader.loadPackage("com.pycho.features")
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            register(dispatcher)
        }
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(ROOT)
                .requires { it.permissions().hasPermission(net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS)) }
                .executes { showHelp(it.source); 1}
                .apply {
                    branches.forEach { (category, commands) ->
                        then(buildCategory(category, commands))
                    }
                    then(Commands.literal("help").executes { showHelp(it.source); 1 })
                    then(Commands.literal("info").executes {
                        it.source.sendSuccess({ Component.literal("§6=== ColdestLand Mod ===\n§7Version: 1.0.1")}, false)
                        1
                    })
                }
        )
    }

    private fun buildCategory(name: String, commands: List<CommandBranch>): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal(name)
            .executes {
                showCategoryHelp(it.source, name, commands)
                1
            }
            .apply {
                commands.forEach { cmd ->
                    then(cmd.build())
                }
            }
    }

    private fun showHelp(source: CommandSourceStack) {
        source.sendSuccess({
            Component.literal( buildString {
                append("§6=== ColdestLand Commands ===")
                branches.forEach { (category, _) ->
                    append("\n§7/$ROOT $category §f- ${category.replaceFirstChar { it.uppercase() }} commands")
                }
                append("\n§7/$ROOT info §f- Mod info")
            })
        },true)
    }

    private fun showCategoryHelp(source: CommandSourceStack, category: String, commands: List<CommandBranch>) {
        source.sendSuccess({
            Component.literal(buildString {
                append("§6=== ${category.replaceFirstChar { it.uppercase() }} Commands ===")
                commands.forEach { cmd ->
                    append("\n§7${cmd.name} §f- ${cmd.description}")
                }
            })
        },true)
    }

    internal fun register(category: String, branch: CommandBranch) {
        branches.getOrPut(category) { mutableListOf() }.add(branch)
    }
}

abstract class CommandBranch(
    val name: String,
    val description: String,
    category: String = "debug"
) {
    init {
        CommandRegistry.register(category, this)
    }

    abstract fun CommandBuilder.register()

    internal fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = CommandBuilder(name)
        builder.register()
        return builder.root
    }
}

class CommandBuilder(name: String) {
    internal val root = Commands.literal(name)
    private var helpText = mutableListOf<String>()
    private var hasDefaultExecutor = false

    fun help(text: String) {
        helpText.add(text)
    }

    fun executes(block: CommandContext<CommandSourceStack>.(ctx: ModCommandContext) -> Int) {
        hasDefaultExecutor = true
        root.executes { context ->
            val ctx = ModCommandContext(context)
            if (helpText.isNotEmpty() && !hasSubcommands) {
                ctx.success(helpText.joinToString("\n"))
                return@executes 1
            }
            context.block(ctx)
        }
    }

    private var hasSubcommands = false

    fun literal(name: String, block: SubCommandBuilder.() -> Unit) {
        hasSubcommands = true
        val sub = SubCommandBuilder(name)
        sub.block()
        root.then(sub.build())

        if (sub.help.isNotEmpty()) {
            helpText.add("§7$name §f- ${sub.help}")
        }
    }

    fun <T> argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentCommandBuilder<T>.() -> Unit
    ) {
        hasSubcommands = true
        val arg = ArgumentCommandBuilder(name, type)
        arg.block()
        root.then(arg.build())
    }

    init {
        root.executes { context ->
            if (helpText.isNotEmpty()) {
                ModCommandContext(context).success(helpText.joinToString("\n"))
            }
            1
        }
    }
}

class SubCommandBuilder(name: String) {
    private val node = Commands.literal(name)
    var help: String = ""

    fun literal(name: String, block: SubCommandBuilder.() -> Unit) {
        val sub = SubCommandBuilder(name)
        sub.block()
        node.then(sub.build())
    }

    fun executes(block: CommandContext<CommandSourceStack>.(ctx: ModCommandContext) -> Int) {
        node.executes { context ->
            context.block(ModCommandContext(context))
        }
    }

    fun <T> argument(
        name: String,
        type: ArgumentType<T>,
        block: ArgumentCommandBuilder<T>.() -> Unit
    ) {
        val arg = ArgumentCommandBuilder(name, type)
        arg.block()
        node.then(arg.build())
    }

    internal fun build() = node
}

class ArgumentCommandBuilder<T>(
    private val name: String,
    private val type: ArgumentType<T>
) {
    @Suppress("UNCHECKED_CAST")
    private var node = Commands.argument(name, type as ArgumentType<Any>) as RequiredArgumentBuilder<CommandSourceStack, T>

    var help: String = ""

    fun suggests(values: Array<String>) {
        node.suggests { _, builder ->
            values.forEach { builder.suggest(it) }
            builder.buildFuture()
        }
    }

    fun suggests(block: () -> Array<String>) {
        node.suggests { _, builder ->
            block().forEach { builder.suggest(it) }
            builder.buildFuture()
        }
    }

    fun executes(block: CommandContext<CommandSourceStack>.(ctx: ModCommandContext, value: T) -> Int) {
        node.executes { context ->
            val value: T = when (type) {
                is IntegerArgumentType -> context.getArgument(name, Int::class.java) as T
                is DoubleArgumentType -> context.getArgument(name, Double::class.java) as T
                is FloatArgumentType -> context.getArgument(name, Float::class.java) as T
                is StringArgumentType -> context.getArgument(name, String::class.java) as T
                is Vec3ArgumentType -> context.getArgument(name, Vec3::class.java) as T
                is MultiIntArgumentType -> context.getArgument(name, List::class.java) as T
                is MultiDoubleArgumentType -> context.getArgument(name, List::class.java) as T
                else -> context.getArgument(name, Any::class.java) as T
            }
            context.block(ModCommandContext(context), value)
        }
    }

    fun <T2> argument(
        name: String,
        type: ArgumentType<T2>,
        block: ArgumentCommandBuilder<T2>.() -> Unit
    ) {
        val arg = ArgumentCommandBuilder(name, type)
        arg.block()
        node.then(arg.build())
    }

    internal fun build() = node
}

class ModCommandContext(private val context: CommandContext<CommandSourceStack>) {
    val source: CommandSourceStack get() = context.source

    val player: ServerPlayer?
        get() = try {
            source.playerOrException
        } catch (e: CommandSyntaxException) {
            null
        }

    val playerOrFail: ServerPlayer
        get() = source.playerOrException

    fun success(message: String, broadcast: Boolean = false) {
        source.sendSuccess({ Component.literal(message) }, broadcast)
    }

    fun failure(message: String) {
        source.sendFailure(Component.literal(message))
    }

    fun <T> requirePlayer(block: ServerPlayer.() -> T): T? {
        val p = player
        if (p == null) {
            failure("§cOnly players can use this!")
            return null
        }
        return p.block()
    }

    fun requireItemInHand(predicate: (ItemStack) -> Boolean, error: String = "§cWrong item in hand!"): ItemStack? {
        return requirePlayer {
            val stack = mainHandItem
            if (predicate(stack)) stack else {
                source.sendFailure(Component.literal(error))
                null
            }
        }
    }

    fun getInt(name: String): Int = context.getArgument(name, Int::class.java)
    fun getString(name: String): String = context.getArgument(name, String::class.java)
    fun getDouble(name: String): Double = context.getArgument(name, Double::class.java)
    fun getFloat(name: String): Float = context.getArgument(name, Float::class.java)
    fun getVec3(name: String): Vec3 = context.getArgument(name, Vec3::class.java)

    @Suppress("UNCHECKED_CAST")
    fun getMultiInt(name: String): List<Int> = context.getArgument(name, List::class.java) as List<Int>
    @Suppress("UNCHECKED_CAST")
    fun getMultiDouble(name: String): List<Double> = context.getArgument(name, List::class.java) as List<Double>
}

// Standard argument type factories
fun int(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) =
    IntegerArgumentType.integer(min, max)

fun int(range: IntRange) =
    IntegerArgumentType.integer(range.first, range.last)

fun double(min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE) =
    DoubleArgumentType.doubleArg(min, max)

fun float(min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE) =
    FloatArgumentType.floatArg(min, max)

fun string() = StringArgumentType.string()
fun word() = StringArgumentType.word()
fun greedy() = StringArgumentType.greedyString()
