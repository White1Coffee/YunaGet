package nl.yunaget.client.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import nl.yunaget.client.NavigationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void yunaget$walk(CallbackInfo ci) {
        if (NavigationManager.move(keyPresses)) {
            keyPresses = new Input(true,false,false,false,NavigationManager.jump,false,false);
            moveVector = new Vec2(0,1);
        }
    }
}
