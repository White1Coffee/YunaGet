package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoToolManager {
    private static int original=-1,selected=-1;
    public record Choice(int slot,double speed,double ticks) { }
    /** Read-only: route planning never switches the actual selected slot. */
    public static Choice evaluate(BlockState state) {
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        var inventory=mc.player.getInventory();Choice best=null;
        int current=inventory.getSelectedSlot();
        for(int i=0;i<9;i++) {
            if(!YunaGetConfig.get.autoTool&&i!=current)continue;
            var stack=inventory.getItem(i);
            if(state.requiresCorrectToolForDrops()&&!stack.isCorrectToolForDrops(state))continue;
            if(stack.isDamageableItem()&&stack.getMaxDamage()-stack.getDamageValue()<=YunaGetConfig.get.minDurability)continue;
            double speed=stack.getDestroySpeed(state);
            for(var e:stack.getEnchantments().entrySet())if(speed>1&&e.getKey().is(Identifier.parse("minecraft:efficiency")))speed+=e.getIntValue()*e.getIntValue()+1;
            // Cost estimate only; execution always uses the normal game mode's breaking progress.
            double ticks=Math.max(1,30*Math.max(0,state.getDestroySpeed(mc.level,mc.player.blockPosition()))/Math.max(.01,speed));
            if(best==null||speed>best.speed()||speed==best.speed()&&i==current)best=new Choice(i,speed,ticks);
        }
        return best;
    }
    public static boolean choose(BlockState state) {
        var mc=Minecraft.getInstance();var choice=evaluate(state);if(choice==null)return false;
        var inventory=mc.player.getInventory();
        if(original>=0&&selected>=0&&inventory.getSelectedSlot()!=selected)original=inventory.getSelectedSlot();
        if(original<0)original=inventory.getSelectedSlot();selected=choice.slot();inventory.setSelectedSlot(selected);return true;
    }
    public static void restore() {
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&original>=0&&YunaGetConfig.get.restoreSlot&&mc.player.getInventory().getSelectedSlot()==selected)mc.player.getInventory().setSelectedSlot(original);
        original=selected=-1;
    }
}
