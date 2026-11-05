package nl.yunaget.client;

import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import nl.yunaget.client.core.StorageRule;

public final class ChestFilterManager {
    public static StorageRule destination(ItemStack stack) {
        return StorageRule.choose(ChestManager.current(), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
            tag -> {
                var id = Identifier.tryParse(tag);
                return id != null && stack.is(holder -> holder.is(TagKey.create(Registries.ITEM, id)));
            }, YunaGetConfig.get.categories, YunaGetConfig.get.useFallback);
    }
    public static boolean validToken(String token) {
        if (token.startsWith("@")) return token.length() > 1 && token.substring(1).matches("[A-Za-z0-9_ -]{1,64}");
        return Identifier.tryParse(token.startsWith("#") ? token.substring(1) : token) != null;
    }
}
