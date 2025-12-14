package com.pycho.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.pycho.ColdestLand
import com.pycho.items.ModItems
import com.pycho.items.TemporalBladeItem
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.item.ItemStack

object DebugCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("temporal")
                .requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)) }
                .then(Commands.literal("give").executes { context ->
                    val player = context.source.playerOrException
                    val stack = ItemStack(ModItems.TEMPORAL_BLADE)
                    player.addItem(stack)
                    context.source.sendSuccess(
                        { Component.literal("Gave Temporal Blade to ${player.displayName.string}") },
                        true
                    )
                    1
                })
                .then(Commands.literal("energy")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000))
                        .executes { context ->
                            val player = context.source.playerOrException
                            val amount = IntegerArgumentType.getInteger(context, "amount")
                            val stack = player.mainHandItem

                            if (stack.item is TemporalBladeItem) {
                                TemporalBladeItem.setEnergy(stack, amount)
                                context.source.sendSuccess(
                                    { Component.literal("Set energy to $amount") },
                                    true
                                )
                            } else {
                                context.source.sendFailure(
                                    Component.literal("Hold Temporal Blade in main hand!")
                                )
                            }
                            1
                        }))
                .then(Commands.literal("fragment")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val type = StringArgumentType.getString(context, "type")
                            val stack = player.mainHandItem

                            if (stack.item is TemporalBladeItem) {
                                val fragment = when (type.lowercase()) {
                                    "ender" -> TemporalBladeItem.ENDER_FRAGMENT
                                    "wither" -> TemporalBladeItem.WITHER_FRAGMENT
                                    "guardian" -> TemporalBladeItem.GUARDIAN_FRAGMENT
                                    "warden" -> TemporalBladeItem.WARDEN_FRAGMENT
                                    else -> null
                                }

                                if (fragment != null) {
                                    TemporalBladeItem.addFragment(stack, fragment)
                                    context.source.sendSuccess(
                                        { Component.literal("Added $type fragment") },
                                        true
                                    )
                                } else {
                                    context.source.sendFailure(
                                        Component.literal("Invalid fragment type: $type")
                                    )
                                }
                            } else {
                                context.source.sendFailure(
                                    Component.literal("Hold Temporal Blade in main hand!")
                                )
                            }
                            1
                        }))
                .then(Commands.literal("combo").executes { context ->
                    val player = context.source.playerOrException
                    val stack = player.mainHandItem

                    if (stack.item is TemporalBladeItem) {
                        val sequence = TemporalBladeItem.getComboSequence(stack) ?: 0
                        val step = TemporalBladeItem.getComboStep(stack) ?: 0
                        val time = TemporalBladeItem.getLastClickTime(stack) ?: 0L

                        context.source.sendSuccess(
                            {
                                Component.literal("Combo State:")
                                    .append("\nSequence: $sequence (${sequence.toString(2)})")
                                    .append("\nStep: $step")
                                    .append("\nLast Click: $time")
                            },
                            true
                        )
                    } else {
                        context.source.sendFailure(
                            Component.literal("Hold Temporal Blade in main hand!")
                        )
                    }
                    1
                })
        )
    }
}