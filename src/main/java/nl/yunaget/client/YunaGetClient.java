package nl.yunaget.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class YunaGetClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        Keybinds.load();
        YunaGetConfig.load();
        OreEspRenderer.register();
        ChestGlowRenderer.register();
        NavigationRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TaskManager::tick);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> WorldScanner.reset());
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() && ChestManager.marking) {
                ChestManager.mark(hit.getBlockPos());
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("yunagetstop").executes(ctx -> {
                TaskManager.stop();
                return 1;
            })));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("yunagetui").executes(ctx -> {
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new YunaGetScreen()));
                return 1;
            })));
    }
}

