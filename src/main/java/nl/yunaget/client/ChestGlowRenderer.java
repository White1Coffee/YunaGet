package nl.yunaget.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.gizmos.*;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

public final class ChestGlowRenderer {
    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            var c = YunaGetConfig.get;
            if (!c.chestGlow) return;
            var collector = new SimpleGizmoCollector();
            try (var ignored = Gizmos.withCollector(collector)) {
                for (var rule : ChestManager.current()) {
                    var p = ChestManager.pos(rule);
                    if (!context.level().hasChunkAt(p) || p.distSqr(context.camera().blockPosition()) > c.renderDistance*c.renderDistance) continue;
                    var s = context.level().getBlockState(p);
                    if (!(s.getBlock() instanceof ChestBlock)) continue;
                    AABB bounds = new AABB(p);
                    if (s.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        var other = ChestBlock.getConnectedBlockPos(p,s);
                        if (context.level().hasChunkAt(other) && context.level().getBlockState(other).getBlock() instanceof ChestBlock)
                            bounds = bounds.minmax(new AABB(other));
                    }
                    int color = rule.enabled ? 0xFF44EE88 : 0xFFFFAA44;
                    var properties = Gizmos.cuboid(bounds,GizmoStyle.stroke(color,2));
                    if (c.chestGlowThroughWalls) properties.setAlwaysOnTop();
                }
            }
            context.levelRenderer().addMainThreadGizmos(collector.drainGizmos());
        });
    }
}
