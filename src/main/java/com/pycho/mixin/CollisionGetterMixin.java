package com.pycho.mixin;

import com.google.common.collect.ImmutableList;
import com.pycho.systems.barrier.BarrierData;
import com.pycho.systems.barrier.BarrierManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter; // or LevelReader/BlockGetter depending on mapping
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {

    @Inject(
            method = "getBlockCollisions",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetBlockCollisions(@Nullable Entity entity, AABB aabb, CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
        if (!(this instanceof ServerLevel level)) return;

        List<BarrierData> barriers = BarrierManager.INSTANCE.getBarriersInArea(level, aabb);
        if (barriers.isEmpty()) return;

        Iterable<VoxelShape> original = cir.getReturnValue();
        ImmutableList.Builder<@NotNull VoxelShape> builder = ImmutableList.builder();

        if (original != null) {
            builder.addAll(original);
        }

        for (BarrierData barrier : barriers) {
            for(VoxelShape shape : barrier.collisionShape) {
                builder.add(shape);
            }
        }

        cir.setReturnValue(builder.build());
    }
}