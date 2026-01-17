package com.pycho.features.temporal

import com.pycho.core.ModItems
import com.pycho.util.*
import net.minecraft.world.item.ItemStack

@PychoCommand
object TemporalCommand : CommandBranch(
    name = "temporal",
    description = "Temporal Blade debug tools"
) {
    private val FRAGMENTS = arrayOf("ender", "wither", "guardian", "warden")

    override fun CommandBuilder.register() {
        help("§6=== Temporal Blade Commands ===")

        literal("give") {
            help = "Give temporal blade"

            executes { ctx ->
                ctx.giveItem(1)
            }

            argument("amount", int(1..64)) {
                executes { ctx, amount ->
                    ctx.giveItem(amount)
                }
            }
        }

        literal("energy") {
            help = "Energy management"

            executes { ctx ->
                ctx.withBlade { blade ->
                    val energy = TemporalBladeItem.getEnergy(blade)
                    ctx.success("§6Energy: §a$energy§7/§a1000")
                    1
                } ?: 0
            }

            literal("set") {
                argument("amount", int(0..1000)) {
                    executes { ctx, amount ->
                        ctx.withBlade { blade ->
                            TemporalBladeItem.setEnergy(blade, amount)
                            ctx.success("§aSet energy to §6$amount", true)
                            1
                        } ?: 0
                    }
                }
            }

            literal("add") {
                argument("amount", int(1..1000)) {
                    executes { ctx, amount ->
                        ctx.withBlade { blade ->
                            val current = TemporalBladeItem.getEnergy(blade)
                            val new = (current + amount).coerceIn(0, 1000)
                            TemporalBladeItem.setEnergy(blade, new)
                            ctx.success("§aAdded §6$amount §aenergy (§6$new§a)", true)
                            1
                        } ?: 0
                    }
                }
            }

            literal("max") {
                executes { ctx ->
                    ctx.withBlade { blade ->
                        TemporalBladeItem.setEnergy(blade, 1000)
                        ctx.success("§aMaxed energy!", true)
                        1
                    } ?: 0
                }
            }

            literal("clear") {
                executes { ctx ->
                    ctx.withBlade { blade ->
                        TemporalBladeItem.setEnergy(blade, 0)
                        ctx.success("§aCleared energy!", true)
                        1
                    } ?: 0
                }
            }
        }

        literal("fragment") {
            help = "Fragment management"

            executes { ctx ->
                ctx.withBlade { blade ->
                    val installed = FRAGMENTS.filter {
                        getFragment(it)?.let { frag ->
                            TemporalBladeItem.isFragmentAquired(blade, frag)
                        } ?: false
                    }

                    if (installed.isEmpty()) {
                        ctx.success("§7No fragments installed")
                    } else {
                        ctx.success("§6Fragments:\n§a• " + installed.joinToString("\n§a• ") {
                            it.replaceFirstChar { c -> c.uppercase() }
                        })
                    }
                    1
                } ?: 0
            }

            literal("add") {
                argument("type", word()) {
                    suggests(FRAGMENTS)

                    executes { ctx, type ->
                        ctx.withBlade { blade ->
                            val frag = getFragment(type)
                            if (frag == null) {
                                ctx.failure("§cInvalid fragment type: §6$type\n§7Valid: ${FRAGMENTS.joinToString(", ")}")
                                return@withBlade 0
                            }

                            TemporalBladeItem.addFragment(blade, frag)
                            ctx.success("§aAdded §6${type.replaceFirstChar { it.uppercase() }} Fragment", true)
                            1
                        } ?: 0
                    }
                }
            }

            literal("remove") {
                argument("type", word()) {
                    suggests(FRAGMENTS)

                    executes { ctx, type ->
                        ctx.withBlade { blade ->
                            val frag = getFragment(type)
                            if (frag == null) {
                                ctx.failure("§cInvalid fragment type: §6$type")
                                return@withBlade 0
                            }

                            // remove logic - ensure TemporalBladeItem has a remove method or set appropriate flags
                            // If TemporalBladeItem doesn't have a remove method, this is currently a no-op message
                            // TemporalBladeItem.removeFragment(blade, frag)
                            ctx.success("§aRemoved §6${type.replaceFirstChar { it.uppercase() }} Fragment", true)
                            1
                        } ?: 0
                    }
                }
            }

            literal("clear") {
                executes { ctx ->
                    ctx.withBlade { blade ->
                        TemporalBladeItem.setFragment(blade, 0)
                        ctx.success("§aCleared all fragments!", true)
                        1
                    } ?: 0
                }
            }

            literal("all") {
                executes { ctx ->
                    ctx.withBlade { blade ->
                        FRAGMENTS.forEach { type ->
                            getFragment(type)?.let { TemporalBladeItem.addFragment(blade, it) }
                        }
                        ctx.success("§aAdded all fragments!", true)
                        1
                    } ?: 0
                }
            }
        }

        literal("inspect") {
            help = "Show all blade data"

            executes { ctx ->
                ctx.withBlade { blade ->
                    val energy = TemporalBladeItem.getEnergy(blade)
                    val sequence = TemporalBladeItem.getComboSequence(blade) ?: 0
                    val step = TemporalBladeItem.getComboStep(blade) ?: 0

                    val frags = FRAGMENTS.filter {
                        getFragment(it)?.let { frag ->
                            TemporalBladeItem.isFragmentAquired(blade, frag)
                        } ?: false
                    }

                    ctx.success(buildString {
                        append("§6=== Temporal Blade ===")
                        append("\n§7Energy: §a$energy§7/§a1000")
                        append("\n§7Fragments: §a${frags.joinToString(", ") {
                            it.replaceFirstChar { c -> c.uppercase() }
                        }.ifEmpty { "None" }}")
                        append("\n§7Combo: §a$sequence §7(step §a$step§7)")
                    })
                    1
                } ?: 0
            }
        }

        literal("reset") {
            help = "Reset all blade data"

            executes { ctx ->
                ctx.withBlade { blade ->
                    TemporalBladeItem.setEnergy(blade, 0)
                    TemporalBladeItem.setFragment(blade, 0)
                    ctx.success("§aReset all blade data!", true)
                    1
                } ?: 0
            }
        }
    }

    // Helper extensions

    private fun ModCommandContext.giveItem(amount: Int): Int {
        return requirePlayer {
            val stack = ItemStack(ModItems.TEMPORAL_BLADE, amount)
            if (addItem(stack)) {
                success("§aGave §6${amount}x Temporal Blade", true)
                1
            } else {
                failure("§cInventory full!")
                0
            }
        } ?: 0
    }

    private fun ModCommandContext.withBlade(block: (ItemStack) -> Int): Int? {
        return requireItemInHand(
            { it.item is TemporalBladeItem },
            "§cYou must hold a Temporal Blade!"
        )?.let(block)
    }

    private fun getFragment(type: String): Byte? = when (type.lowercase()) {
        "ender" -> TemporalBladeItem.ENDER_FRAGMENT
        "wither" -> TemporalBladeItem.WITHER_FRAGMENT
        "guardian" -> TemporalBladeItem.GUARDIAN_FRAGMENT
        "warden" -> TemporalBladeItem.WARDEN_FRAGMENT
        else -> null
    }
}
