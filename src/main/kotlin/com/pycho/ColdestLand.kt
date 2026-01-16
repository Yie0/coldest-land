package com.pycho

import com.pycho.commands.CommandRegistry
import com.pycho.items.ModItems
import com.pycho.network.ModNetworking
import com.pycho.systems.barrier.BarrierManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ColdestLand : ModInitializer {
    private val MODID: String = "coldest-land"
    private val LOGGER = LoggerFactory.getLogger(MODID)

	override fun onInitialize() {
        ModNetworking.registerGlobalReceivers()
        ModItems.setup()
        CommandRegistry.setup()

        ServerTickEvents.END_SERVER_TICK.register { server ->
            val currentTime = server.overworld().gameTime
            server.allLevels.forEach { level ->
                BarrierManager.tickLevel(level, currentTime)
            }
        }

	}

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MODID, path)
}