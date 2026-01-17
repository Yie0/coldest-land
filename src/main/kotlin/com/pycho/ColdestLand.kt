package com.pycho

import com.pycho.core.ModItems
import com.pycho.core.Packets
import com.pycho.features.barrier.BarrierManager
import com.pycho.util.CommandRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ColdestLand : ModInitializer {
    private val MODID: String = "coldestland"
    private val LOGGER = LoggerFactory.getLogger(MODID)

	override fun onInitialize() {
        Packets.registerGlobalReceivers()
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
