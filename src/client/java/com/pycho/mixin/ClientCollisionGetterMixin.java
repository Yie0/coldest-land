package com.pycho.mixin;

import com.google.common.collect.ImmutableList;
import com.pycho.client.systems.ClientBarrierManager;
import com.pycho.systems.barrier.BarrierData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CollisionGetter.class)
public interface ClientCollisionGetterMixin {

    @Inject(
            method = "getBlockCollisions",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetBlockCollisions(@Nullable Entity entity, AABB aabb, CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
        if (!(this instanceof Level level)) return;
        if (!level.isClientSide())
            return;

        List<BarrierData> barriers = ClientBarrierManager.INSTANCE.getBarriersInArea(level, aabb);
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