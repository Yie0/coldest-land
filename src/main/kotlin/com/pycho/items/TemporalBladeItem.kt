package com.pycho.items

import Alert
import com.pycho.components.ModComponents
import com.pycho.components.TemporalRecord
import com.pycho.network.AlertPayload
import com.pycho.systems.barrier.BarrierData
import com.pycho.systems.barrier.BarrierManager
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.Relative
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BeaconBlockEntity
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TemporalBladeItem(props: Item.Properties) : Item(props) {

    companion object {
        // Energy constants
        const val MAX_ENERGY: Int = 1000
        const val ENERGY_PER_HIT: Int = 10
        const val RECHARGE_RATE: Int = 5
        const val BEACON_RANGE: Int = 50
        const val COMBO_TIMEOUT_TICKS: Long = 40 // 2 seconds

        // Combo sequences (3 bits: R=0, L=1)
        const val COMBO_RRR: Byte = 0b000 // 0
        const val COMBO_RRL: Byte = 0b001 // 1
        const val COMBO_RLR: Byte = 0b010 // 2
        const val COMBO_RLL: Byte = 0b011 // 3

        // Fragment byte values
        const val ENDER_FRAGMENT: Byte = 0b00000001
        const val WITHER_FRAGMENT: Byte = 0b00000010
        const val GUARDIAN_FRAGMENT: Byte = 0b00000100
        const val WARDEN_FRAGMENT: Byte = 0b00001000

        // Combo tracking
        fun getComboSequence(stack: ItemStack): Byte? {
            return stack.get(ModComponents.COMBO_SEQUENCE)
        }

        fun setComboSequence(stack: ItemStack, sequence: Byte) {
            stack.set(ModComponents.COMBO_SEQUENCE, sequence)
        }

        fun getComboStep(stack: ItemStack): Byte? {
            return stack.get(ModComponents.COMBO_STEP)
        }

        fun setComboStep(stack: ItemStack, step: Byte) {
            stack.set(ModComponents.COMBO_STEP, step)
        }

        fun getLastClickTime(stack: ItemStack): Long? {
            return stack.get(ModComponents.COMBO_TIMESTAMP)
        }

        fun setLastClickTime(stack: ItemStack, time: Long) {
            stack.set(ModComponents.COMBO_TIMESTAMP, time)
        }

        // Energy helper methods
        fun getEnergy(stack: ItemStack): Int {
            val energy = stack.get<Int>(ModComponents.CHRONO_ENERGY)
            return energy ?: 0
        }

        fun setEnergy(stack: ItemStack, energy: Int) {
            stack.set<Int>(ModComponents.CHRONO_ENERGY, max(0, min(energy, MAX_ENERGY)))
        }

        fun getFragments(stack: ItemStack): Byte {
            val fragments = stack.get<Byte>(ModComponents.FRAGMENTS)
            return fragments ?: 0
        }

        fun setFragment(stack: ItemStack, fragment: Byte) {
            stack.set<Byte>(ModComponents.FRAGMENTS, fragment)
        }

        fun addFragment(stack: ItemStack, fragment: Byte) {
            setFragment(stack, getFragments(stack).or(fragment))
        }

        fun isFragmentAquired(stack: ItemStack, fragment: Byte): Boolean {
            return getFragments(stack).and(fragment).toInt() != 0
        }

        fun getEnergyMultiplier(stack: ItemStack): Float {
            val multiplier = stack.get<Float>(ModComponents.ENERGY_MULTIPLIER)
            return multiplier ?: 1.0f
        }

        fun setEnergyMultiplier(stack: ItemStack, multiplier: Float) {
            stack.set<Float>(ModComponents.ENERGY_MULTIPLIER, max(1.0f, multiplier))
        }

        fun isNearBeacon(level: Level, pos: BlockPos): Boolean {
            //going to modify the beacon interface instead of this in the future
            return level.getBlockEntity(pos.below(3)) is BeaconBlockEntity
        }

        fun sendActivateText(player: ServerPlayer, component: Component){
            val alert = Alert(
                component,
                4f,
                2500,
            )
            AlertPayload(alert).send(player)
        }
    }
    private fun getRecallTarget(player: ServerPlayer): TemporalRecord?{
       return player.getAttached(ModComponents.TEMPORAL_HISTORY)!!.lastOrNull()
    }

    var comboText: Component = Component.empty()
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (player.isCrouching) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f)
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Fragment GUI"),
                    true
                )
            }
            return InteractionResult.SUCCESS
        }
        else{
            handleComboInput(player.getItemInHand(hand), level, player, true, hand)
        }
        return InteractionResult.PASS
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity){
        val energy = getEnergy(stack)
        val multiplier = getEnergyMultiplier(stack)
        val cost = (ENERGY_PER_HIT * multiplier).toInt()

        if (energy >= cost) {
            setEnergy(stack, energy - cost)
            target.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.2f)
        } else {
            target.playSound(SoundEvents.ITEM_BREAK.value(), 0.3f, 0.8f)
        }

        return super.hurtEnemy(stack, target, attacker)
    }

    fun handleComboInput(
        stack: ItemStack,
        level: Level,
        player: Player,
        isRightClick: Boolean,
        hand: InteractionHand
    ) {
        val currentTime = level.gameTime
        val lastClickTime = getLastClickTime(stack) ?: 0L

        // Check timeout
        if (currentTime - lastClickTime > COMBO_TIMEOUT_TICKS) {
            setComboSequence(stack, 0)
            setComboStep(stack, 0)
        }

        var currentSequence = getComboSequence(stack) ?: 0
        var currentStep = getComboStep(stack) ?: 0

        if (currentStep == 0.toByte()) {
            if (!isRightClick) return

            currentSequence = 0
            currentStep = 1
            comboText = Component.literal("Combo Started! → Right").withStyle(ChatFormatting.YELLOW)

            if (!level.isClientSide) {
                player.displayClientMessage(
                    comboText,
                    true
                )
            }
            player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.5f, 1.0f)

        } else if (currentStep == 1.toByte()) {
            // Second click
            currentSequence = if (isRightClick) 0 else 1
            currentStep = 2
            val direction = if (isRightClick) "Right" else "Left"
            if (!level.isClientSide) {
                comboText = Component.literal("Right → $direction").withStyle(ChatFormatting.YELLOW)
                player.displayClientMessage(
                    comboText,
                    true
                )
            }
            player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.5f, 1.2f)

        } else if (currentStep == 2.toByte()) {
            // Final click
            currentSequence = currentSequence.or(if (isRightClick) 0 else 2)

            if (!level.isClientSide) {
                val direction = if (isRightClick) "Right" else "Left"
                comboText = Component.literal(comboText.string + " → $direction ✓").withStyle(ChatFormatting.GREEN)

                player.displayClientMessage(
                    comboText,
                    true
                )
                triggerComboAbility(stack, level, player, currentSequence)
            }
            currentSequence = 0
            currentStep = 0
        }

        setComboSequence(stack, currentSequence)
        setComboStep(stack, currentStep)
        setLastClickTime(stack, currentTime)
    }

    private fun triggerComboAbility(stack: ItemStack, level: Level, player: Player, sequence: Byte) {
        val energy = getEnergy(stack)
        val abilityCost = 100 // could change on the future but good for know

        if (energy < abilityCost) {
            player.displayClientMessage(
                Component.literal("Not enough energy! ($energy/$abilityCost)")
                    .withStyle(ChatFormatting.RED),
                true
            )
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.5f)
            return
        }

        // Deduct energy
        setEnergy(stack, energy - abilityCost)

        when (sequence) {
            COMBO_RRR -> activateAbility1(stack, level, player)
            COMBO_RRL -> activateAbility2(stack, level, player)
            COMBO_RLR -> activateAbility3(stack, level, player)
            COMBO_RLL -> activateAbility4(stack, level, player)
            else -> {
                player.displayClientMessage(
                    Component.literal("Unknown combo sequence!")
                        .withStyle(ChatFormatting.RED),
                    true
                )
                return
            }
        }

        // Play activation sound
        player.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.7f, 1.0f)
        player.playSound(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.5f)
    }

    private fun activateAbility1(stack: ItemStack, level: Level, player: Player) {
        // RRR - Ender Fragment Ability: Temporal Recall
        if (!isFragmentAquired(stack, ENDER_FRAGMENT)) {
            player.displayClientMessage(
                Component.literal("Requires Ender Fragment!")
                    .withStyle(ChatFormatting.DARK_PURPLE),
                true
            )
            return
        }
        sendActivateText(player as ServerPlayer, Component.literal("✦ Temporal Recall Activated! ✦").withStyle(ChatFormatting.LIGHT_PURPLE))

        val recallTarget = getRecallTarget(player) ?: return

        if(isFragmentAquired(stack, WITHER_FRAGMENT)) {
            enderAbilitySynergy(stack, level, player)
            if(recallTarget.health > player.health){
                player.heal((recallTarget.health-player.health)/2)
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
            }
        }

        player.moveOrInterpolateTo(Vec3(recallTarget.x, recallTarget.y, recallTarget.z))
        player.teleport(TeleportTransition(
            level as ServerLevel,
            Vec3(recallTarget.x, recallTarget.y, recallTarget.z),
            player.deltaMovement,
            player.yRot,
            player.xRot,
            false,
            player.isPassenger,
            emptySet<Relative>(),
            TeleportTransition.DO_NOTHING
        ))

    }

    private fun enderAbilitySynergy(stack: ItemStack, level: Level, player: Player) {
        val entities = getEntitiesAround(level,player.position(), 7, {entity ->
            (entity.type != EntityType.WITHER_SKULL) and
                    (entity.type.category == MobCategory.MONSTER) or ((entity.type == EntityType.PLAYER) and (entity.name != player.name))})
        entities.forEach { entity ->
            val pos = BlockPos(
                player.position().x.roundToInt(),
                player.position().y.roundToInt()+1,
                player.position().z.roundToInt()
            )
            val targetPos = entity.getEyePosition()

            var directionVec = targetPos.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            directionVec = directionVec.normalize()
            val speed = Vec3(0.1,0.1,0.1)
            directionVec = directionVec.multiply(speed)
            player.displayClientMessage(Component.literal(entity.type.toString()),true)

            val projectile = EntityType.WITHER_SKULL.spawn(level as ServerLevel, pos, EntitySpawnReason.EVENT)
            projectile?.owner = player
            projectile?.addDeltaMovement(directionVec)
        }
    }

    fun getEntitiesAround(level: Level, center: Vec3, radius: Int, filter: Predicate<Entity>): MutableList<Entity> {
        val boundingBox: AABB = AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        )

        var entities: MutableList<Entity> = level.getEntities(null,boundingBox, filter)

        val radiusSq = radius * radius
        entities.removeIf { entity: Entity -> entity.distanceToSqr(center) > radiusSq }
        return entities
    }

    private fun activateAbility2(stack: ItemStack, level: Level, player: Player) {
        // RRL - Wither Fragment Ability: Necrotic Decay
        if (!isFragmentAquired(stack, WITHER_FRAGMENT)) {
            player.displayClientMessage(
                Component.literal("Requires Wither Fragment!")
                    .withStyle(ChatFormatting.DARK_GRAY),
                true
            )
            return
        }

        sendActivateText(player as ServerPlayer, Component.literal("☠ Necrotic Decay Activated! ☠").withStyle(ChatFormatting.DARK_GRAY))

        // Apply Wither III to all enemies in 8-block radius
        // Heal wielder based on damage dealt
    }

    private fun activateAbility3(stack: ItemStack, level: Level, player: Player) {
        // RLR - Guardian Fragment Ability: Tidal Barrier
        if (!isFragmentAquired(stack, GUARDIAN_FRAGMENT)) {
            player.displayClientMessage(
                Component.literal("Requires Guardian Fragment!")
                    .withStyle(ChatFormatting.DARK_AQUA),
                true
            )
            return
        }
        sendActivateText(player as ServerPlayer,Component.literal("⊚ Tidal Barrier Created! ⊚").withColor(0x00AAAA))

        val lookVec = player.lookAngle
        val spawnDistance = 3.0

        val spawnPos = player.eyePosition.add(lookVec.scale(spawnDistance))

        val barrier = BarrierData.create(
            center = spawnPos,
            ownerUUID = player.uuid,
            creationTime = level.gameTime,
            lifetime = 100,
            height = 3.0,
            width = 3.0,
            depth = 0.25,
            yaw = player.yRot,
            pitch = player.xRot,
            precision =  0.5
        )

        BarrierManager.addBarrier(level as ServerLevel, barrier)

        level.playSound(
            null,
            BlockPos(
                spawnPos.x.roundToInt(),
                spawnPos.y.roundToInt(),
                spawnPos.z.roundToInt()
            ),
            SoundEvents.BUCKET_EMPTY, //This was a genius pull
            player.soundSource,
            1.0f,
            0.8f
        )

        player.addEffect(MobEffectInstance(MobEffects.WATER_BREATHING, 45))


        // Create water barrier that absorbs damage and slows enemies
        // Custom render for the barrier is required plan is to make it 3 by 3 ocean vibes in a block right
        // in front of the barrier and right in front of the barrier 5xblocks cube of slowness reduce to 3 if op
        // Grants water breathing
    }

    private fun activateAbility4(stack: ItemStack, level: Level, player: Player) {
        // RLL - Warden Fragment Ability: Sonic Resonance
        if (!isFragmentAquired(stack, WARDEN_FRAGMENT)) {
            player.displayClientMessage(
                Component.literal("Requires Warden Fragment!")
                    .withStyle(ChatFormatting.DARK_GREEN),
                true
            )
            return
        }

        sendActivateText(player as ServerPlayer, Component.literal("♫ Sonic Resonance Activated! ♫").withStyle(ChatFormatting.GREEN))
        // Warden's sound beam
    }

    override fun inventoryTick(
        stack: ItemStack,
        level: ServerLevel,
        entity: Entity,
        equipmentSlot: EquipmentSlot?,
    ) {
        super.inventoryTick(stack, level, entity, equipmentSlot)
        if (level.gameTime % 20 == 0L) {
            if (entity is ServerPlayer) {
                val history = entity.getAttachedOrCreate(ModComponents.TEMPORAL_HISTORY)
                history.addFirst(
                    TemporalRecord(
                        entity.position().x,
                        entity.position().y,
                        entity.position().z,
                        entity.health
                    )
                )
                if (history.size > 5) {
                    history.removeLast()
                }

                if (isNearBeacon(level, entity.blockPosition())) {
                    rechargeEnergy(stack)
                }
            }
        }
    }

    private fun rechargeEnergy(stack: ItemStack) {
        val energy = getEnergy(stack)
        if (energy < MAX_ENERGY) {
            val newEnergy = min(energy + RECHARGE_RATE, MAX_ENERGY)
            setEnergy(stack, newEnergy)
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipDisplay: TooltipDisplay,
        consumer: Consumer<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, flag)

        val energy = getEnergy(stack)
        val fragments = getFragments(stack)
        val comboStep = getComboStep(stack) ?: 0

        when (comboStep) {
            1.toByte() -> consumer.accept(
                Component.literal("Combo: Right → ")
                    .withStyle(ChatFormatting.YELLOW)
            )
            2.toByte() -> {
                val sequence = getComboSequence(stack) ?: 0
                val firstMove = if (sequence.and(1).toInt() == 0) "Right" else "Left"
                consumer.accept(
                    Component.literal("Combo: Right → $firstMove → ")
                        .withStyle(ChatFormatting.YELLOW)
                )
            }
        }

        consumer.accept(Component.literal("⏰ Temporal Blade").withStyle(ChatFormatting.DARK_AQUA))
        consumer.accept(
            Component.literal("Energy: $energy/$MAX_ENERGY")
                .withStyle(if (energy > MAX_ENERGY * 0.2) ChatFormatting.BLUE else ChatFormatting.RED)
        )

        val fragmentCount = Integer.bitCount(fragments.toInt() and 0xFF)
        consumer.accept(
            Component.literal("Fragments: $fragmentCount/4")
                .withStyle(ChatFormatting.GRAY)
        )

        if (isFragmentAquired(stack, ENDER_FRAGMENT)) {
            consumer.accept(Component.literal("  ✓ Ender").withStyle(ChatFormatting.LIGHT_PURPLE))
        }
        if (isFragmentAquired(stack, WITHER_FRAGMENT)) {
            consumer.accept(Component.literal("  ✓ Wither").withStyle(ChatFormatting.DARK_GRAY))
        }
        if (isFragmentAquired(stack, GUARDIAN_FRAGMENT)) {
            consumer.accept(Component.literal("  ✓ Guardian").withStyle(ChatFormatting.AQUA))
        }
        if (isFragmentAquired(stack, WARDEN_FRAGMENT)) {
            consumer.accept(Component.literal("  ✓ Warden").withStyle(ChatFormatting.GREEN))
        }

        consumer.accept(Component.literal(""))
        consumer.accept(Component.literal("Combo System:").withStyle(ChatFormatting.GOLD))
        consumer.accept(Component.literal("  R,R,R → Ender Recall").withStyle(ChatFormatting.LIGHT_PURPLE))
        consumer.accept(Component.literal("  R,R,L → Wither Decay").withStyle(ChatFormatting.DARK_GRAY))
        consumer.accept(Component.literal("  R,L,R → Tidal Barrier").withStyle(ChatFormatting.AQUA))
        consumer.accept(Component.literal("  R,L,L → Sonic Pulse").withStyle(ChatFormatting.GREEN))
        consumer.accept(Component.literal("Sneak + Right-click: Fragment GUI")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
        consumer.accept(Component.literal("Requires Beacon for Energy")
            .withStyle(ChatFormatting.DARK_AQUA))
    }


    override fun isBarVisible(stack: ItemStack): Boolean {
        return true
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val energyPercent = getEnergy(stack).toFloat() / MAX_ENERGY
        return Math.round(energyPercent * 13)
    }

    override fun getBarColor(stack: ItemStack): Int {
        val energyPercent = getEnergy(stack).toFloat() / MAX_ENERGY
        if (energyPercent > 0.7f) return 0x00FF00 // Green
        if (energyPercent > 0.3f) return 0xFFFF00 // Yellow
        return 0xFF0000 // Red
    }
}