package nl.yunaget.client.navigation;

import net.minecraft.core.BlockPos;
public record RouteStep(RouteAction action,Surface destination,BlockPos obstacle) {
    public boolean mining() { return obstacle!=null; }
}
