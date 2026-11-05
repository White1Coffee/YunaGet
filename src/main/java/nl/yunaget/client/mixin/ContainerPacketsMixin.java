package nl.yunaget.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import nl.yunaget.client.InventoryTransactionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ContainerPacketsMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void yunaget$content(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        InventoryTransactionManager.content(packet.containerId(),packet.items());
    }
    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void yunaget$slot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        InventoryTransactionManager.slot(packet.getContainerId(),packet.getSlot(),packet.getItem());
    }
}
