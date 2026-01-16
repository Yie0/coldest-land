package com.pycho.commands

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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object CommandRegistry {
    private const val ROOT = "coldest"
    private val branches = mutableMapOf<String, MutableList<CommandBranch>>()

    init {
        branches["debug"] = mutableListOf()
        branches["exec"] = mutableListOf()
    }

    fun setup() {
        CommandArguments.register()

        CommandAutoLoader.loadPackage("com.pycho.commands")
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            register(dispatcher)
        }
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(ROOT)
                .requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)) }
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
