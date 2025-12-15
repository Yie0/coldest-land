package com.pycho

import com.pycho.commands.DebugCommands
import com.pycho.items.ModItems
import com.pycho.network.ModNetworking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ColdestLand : ModInitializer {
    private val MODID: String = "coldest-land"
    private val logger = LoggerFactory.getLogger(MODID)

	override fun onInitialize() {
		logger.info("Hello Fabric world!")
        ModNetworking.registerGlobalReceivers()
        val item = ModItems.Fragments.ENDER
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            DebugCommands.register(dispatcher)
        }

	}

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MODID, path)
}