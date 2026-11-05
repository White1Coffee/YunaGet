package nl.yunaget.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.*;
import java.util.*;

/** Uses the 26.3 gizmo extraction pipeline, not legacy immediate-mode GL. */
public final class OreEspRenderer {
    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            var c = YunaGetConfig.get;
            if (!c.oreEsp&&!c.targetXray) return;
            var collector = new SimpleGizmoCollector();
            try (var ignored = Gizmos.withCollector(collector)) {
                int rendered = 0;
                if(c.targetXray)for(BlockPos p:WorldScanner.mining){
                    if(p.distSqr(context.camera().blockPosition())>c.renderDistance*c.renderDistance||!context.level().hasChunkAt(p)
                        ||!TargetManager.matches(context.level().getBlockState(p)))continue;
                    Gizmos.cuboid(p,GizmoStyle.stroke(0xFF53E7D0,2)).setAlwaysOnTop();
                    if(++rendered>=512)break;
                }
                // Extraction and world cache are accessed on the Minecraft main thread.
                for (var entry : WorldScanner.ores.entrySet()) {
                    if(!c.oreEsp||rendered>=512)break;
                    BlockPos p = entry.getKey();
                    if (p.distSqr(context.camera().blockPosition()) > c.renderDistance*c.renderDistance || !context.level().hasChunkAt(p)) continue;
                    var properties = Gizmos.cuboid(p, GizmoStyle.stroke(c.oreColors.getOrDefault(entry.getValue(),0xFFFFFFFF),2));
                    if (c.throughWalls) properties.setAlwaysOnTop();
                    if (++rendered >= 512) break;
                }
            }
            context.levelRenderer().addMainThreadGizmos(collector.drainGizmos());
        });
    }
}
