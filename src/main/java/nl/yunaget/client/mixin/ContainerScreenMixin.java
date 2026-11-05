package nl.yunaget.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import nl.yunaget.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void yunaget$manualMouse(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (AutoSorterManager.active()) { AutoSorterManager.manualIntervention(); cir.setReturnValue(true); }
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void yunaget$manualKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Keybinds.matching(event.key()) == Keybinds.Action.STOP||Keybinds.matching(event.key())==Keybinds.Action.PANIC) {
            TaskManager.stop(); cir.setReturnValue(true);
        } else if (AutoSorterManager.active()) { AutoSorterManager.manualIntervention(); cir.setReturnValue(true); }
    }
}
