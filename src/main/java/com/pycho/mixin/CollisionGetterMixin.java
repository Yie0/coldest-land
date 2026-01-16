package com.pycho.mixin;

import com.google.common.collect.ImmutableList;
import com.pycho.systems.barrier.BarrierData;
import com.pycho.systems.barrier.BarrierManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.CollisionGetter; // or LevelReader/BlockGetter depending on mapping
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

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

    @Inject(
            method = "clipIncludingBorder",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onClipIncludingBorder(ClipContext clipContext, CallbackInfoReturnable<BlockHitResult> cir) {
        if (!(this instanceof ServerLevel level)) return;

        Vec3 start = clipContext.getFrom();
        Vec3 end = clipContext.getTo();
        AABB sweep = new AABB(start, end).inflate(1.0);

        List<BarrierData> barriers = BarrierManager.INSTANCE.getBarriersInArea(level, sweep);
        if (barriers.isEmpty()) return;

        BlockHitResult vanilla = cir.getReturnValue();
        double vanillaDistSq = vanilla.getType() != HitResult.Type.MISS
                ? vanilla.getLocation().distanceToSqr(start)
                : Double.MAX_VALUE;

        BlockHitResult closestHit = null;

        for (BarrierData barrier : barriers) {
            for (AABB box : barrier.boundingBoxes) {
                Optional<Vec3> hitOpt = box.clip(start, end);
                if (hitOpt.isPresent()) {
                    Vec3 hitLoc = hitOpt.get();
                    double distSq = hitLoc.distanceToSqr(start);

                    if (distSq < vanillaDistSq) {
                        vanillaDistSq = distSq;
                        Direction face = getHitFace(box, hitLoc);
                        BlockPos hitPos = BlockPos.containing(hitLoc);
                        closestHit = new BlockHitResult(hitLoc, face, hitPos, false);
                    }
                }
            }
        }

        if (closestHit != null) {
            cir.setReturnValue(closestHit);
        }
    }

    @Unique
    private static Direction getHitFace(AABB box, Vec3 hit) {
        double eps = 0.0001;
        if (Math.abs(hit.x - box.minX) < eps) return Direction.WEST;
        if (Math.abs(hit.x - box.maxX) < eps) return Direction.EAST;
        if (Math.abs(hit.y - box.minY) < eps) return Direction.DOWN;
        if (Math.abs(hit.y - box.maxY) < eps) return Direction.UP;
        if (Math.abs(hit.z - box.minZ) < eps) return Direction.SOUTH;
        if (Math.abs(hit.z - box.maxZ) < eps) return Direction.NORTH;
        return Direction.UP;
    }
}