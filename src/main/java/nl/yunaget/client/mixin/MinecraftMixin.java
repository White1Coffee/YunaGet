package nl.yunaget.client.mixin;

import net.minecraft.client.Minecraft;
import nl.yunaget.client.MiningTask;
import nl.yunaget.client.ItemGlow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vanilla's released attack key from cancelling an aura-owned mining action each tick. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void yunaget$itemGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ItemGlow.shouldHighlight(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void yunaget$continueAttack(boolean attacking, CallbackInfo ci) {
        if (MiningTask.controlsAttack()) ci.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void yunaget$startAttack(CallbackInfoReturnable<Boolean> cir) {
        if (MiningTask.controlsAttack()) cir.setReturnValue(false);
    }
}
