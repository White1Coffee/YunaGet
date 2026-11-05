package nl.yunaget.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import nl.yunaget.client.InventoryTransactionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ContainerPacketsMixin {
    @Inject(method="handleTakeItemEntity",at=@At("TAIL"))
    private void yunaget$pickup(ClientboundTakeItemEntityPacket packet,CallbackInfo ci){
        nl.yunaget.client.DropCollectorManager.serverPickup(packet.getItemId(),packet.getPlayerId(),packet.getAmount());
    }
    @Inject(method="handleBlockUpdate",at=@At("TAIL"))
    private void yunaget$block(ClientboundBlockUpdatePacket packet,CallbackInfo ci){
        nl.yunaget.client.NavigationManager.serverBlockUpdate(packet.getPos(),packet.getBlockState());
    }
    @Inject(method="handleChunkBlocksUpdate",at=@At("TAIL"))
    private void yunaget$blocks(ClientboundSectionBlocksUpdatePacket packet,CallbackInfo ci){
        packet.runUpdates(nl.yunaget.client.NavigationManager::serverBlockUpdate);
    }
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void yunaget$content(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        InventoryTransactionManager.content(packet.containerId(),packet.items());
    }
    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void yunaget$slot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        InventoryTransactionManager.slot(packet.getContainerId(),packet.getSlot(),packet.getItem());
    }
}
