package nl.yunaget.test;

import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import nl.yunaget.client.*;
import nl.yunaget.client.navigation.*;
import nl.yunaget.client.core.*;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Development-only client+integrated-server fixtures. Never packaged into the production mod. */
public final class NavigationClientTest implements FabricClientGameTest {
    private static final Set<BlockPos> DENIED=ConcurrentHashMap.newKeySet();
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("yunaget.acceptance");
    private static void check(boolean success,String message){if(!success)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context){
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->!DENIED.contains(pos));
        try(var game=context.worldBuilder().create()){
            game.getConnection().waitForChunksDownload();
            prepare(context,game);
            AtomicBoolean input=new AtomicBoolean();
            context.runOnClient(mc->NavigationManager.request(new BlockPos(8,100,1),"Mining"));
            context.waitFor(mc->{
                if(mc.player.input.getMoveVector().y>.01)input.set(true);
                return NavigationManager.state()==NavigationState.ARRIVED;
            },500);
            check(input.get(),"No real ClientInput forward vector observed");
            check(game.getServer().computeOnServer(s->game.getConnection().getServerPlayer().getX()>7),"Server player did not actually move");
            pass("open corridor: real client input and server-validated displacement");

            prepare(context,game);wall(game,Blocks.STONE.defaultBlockState(),false);
            context.runOnClient(mc->NavigationManager.request(new BlockPos(8,100,1),"Mining"));
            context.waitFor(mc->NavigationManager.state()==NavigationState.ARRIVED,900);
            check(context.computeOnClient(mc->NavigationManager.routeDistance()>9),"Expected route with 90-degree turns");pass("corner and obstacle detour");

            prepare(context,game);
            game.getServer().runOnServer(server->{var level=game.getConnection().getServerLevel();
                for(int x=2;x<=3;x++)level.setBlock(new BlockPos(x,99,1),Blocks.DIRT_PATH.defaultBlockState(),3);
                for(int z=0;z<=2;z++){
                    level.setBlock(new BlockPos(4,100,z),Blocks.STONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST),3);
                    for(int x=5;x<=9;x++){level.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),3);level.setBlock(new BlockPos(x,101,z),Blocks.STONE.defaultBlockState(),3);}
                }
            });
            game.getConnection().waitForClientboundPackets();context.waitTicks(5);
            context.runOnClient(mc->NavigationManager.request(new BlockPos(8,102,1),"Mining"));
            context.waitFor(mc->NavigationManager.state()==NavigationState.ARRIVED,700);
            check(context.computeOnClient(mc->mc.player.getY()>101.7),"Player did not ascend");pass("path block, stairs and jump/height difference");

            prepare(context,game);wall(game,Blocks.STONE.defaultBlockState(),false);
            game.getServer().runOnServer(s->game.getConnection().getServerLevel().setBlock(new BlockPos(8,100,1),Blocks.DIAMOND_ORE.defaultBlockState(),3));
            game.getConnection().waitForClientboundPackets();context.waitTicks(5);
            context.runOnClient(mc->{YunaGetConfig.get.onlyVisible=true;YunaGetConfig.get.selectedItems.add("minecraft:diamond");MiningTask.start();YunaGetConfig.get.collector=true;});
            context.waitFor(mc->InventoryTracker.count("minecraft:diamond")>0,1200);pass("hidden target detected despite onlyVisible; normal mining and pickup");

            prepare(context,game);wall(game,Blocks.STONE.defaultBlockState(),true);targetOre(game);
            context.runOnClient(mc->{YunaGetConfig.get.navigationMode=NavigationMode.WALK_AND_MINE;YunaGetConfig.get.tunnelMining=true;NavigationManager.requestBlock(new BlockPos(8,100,1));});
            context.waitFor(mc->NavigationManager.state()==NavigationState.ARRIVED,1200);
            check(context.computeOnClient(mc->NavigationManager.obstaclesRemoved()>=2),"No confirmed tunnel excavation");pass("stone tunnel and authoritative server block confirmations");

            prepare(context,game);wall(game,Blocks.BEDROCK.defaultBlockState(),true);targetOre(game);
            context.runOnClient(mc->{YunaGetConfig.get.navigationMode=NavigationMode.WALK_AND_MINE;YunaGetConfig.get.tunnelMining=true;NavigationManager.requestBlock(new BlockPos(8,100,1));});
            context.waitFor(mc->NavigationManager.state()==NavigationState.FAILED,800);
            check(context.computeOnClient(mc->mc.player.getX()<5&&NavigationManager.obstaclesRemoved()==0),"Bedrock was not respected");pass("unmineable barrier");

            prepare(context,game);wall(game,Blocks.LAVA.defaultBlockState(),true);targetOre(game);
            context.runOnClient(mc->{YunaGetConfig.get.navigationMode=NavigationMode.WALK_AND_MINE;YunaGetConfig.get.tunnelMining=true;NavigationManager.requestBlock(new BlockPos(8,100,1));});
            context.waitFor(mc->NavigationManager.state()==NavigationState.FAILED,800);
            check(context.computeOnClient(mc->!mc.player.isInLava()&&!mc.player.isOnFire()),"Unsafe lava route executed");pass("lava route refused");

            prepare(context,game);int before=DropCollectorManager.confirmedPickups;
            game.getServer().runOnServer(s->{var level=game.getConnection().getServerLevel();var drop=new net.minecraft.world.entity.item.ItemEntity(level,8.5,100.2,1.5,new ItemStack(Items.DIAMOND,1));drop.setDefaultPickUpDelay();level.addFreshEntity(drop);});
            game.getConnection().waitForClientboundPackets();
            context.runOnClient(mc->{YunaGetConfig.get.selectedItems.add("minecraft:diamond");YunaGetConfig.get.collector=true;});
            context.waitTicks(20);
            game.getServer().runOnServer(s->{for(var e:game.getConnection().getServerLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-4,99,-4,17,105,17)))e.setPos(10.5,100.2,4.5);});
            context.waitFor(mc->InventoryTracker.count("minecraft:diamond")>0&&DropCollectorManager.confirmedPickups>before,900);pass("moving drop: reachable pickup position and actual inventory/pickup acknowledgement");

            prepare(context,game);
            game.getServer().runOnServer(s->{var level=game.getConnection().getServerLevel();level.setBlock(new BlockPos(8,100,1),Blocks.CHEST.defaultBlockState(),3);game.getConnection().getServerPlayer().getInventory().setItem(9,new ItemStack(Items.DIAMOND,16));});
            game.getConnection().waitForClientboundPackets();context.waitTicks(10);
            context.runOnClient(mc->{var r=new StorageRule();r.name="Test Diamonds";r.x=8;r.y=100;r.z=1;r.scope=ChestManager.scope();r.dimension=ChestManager.dimension();r.filters.add("minecraft:diamond");YunaGetConfig.get.chests.add(r);YunaGetConfig.get.walkToChest=true;YunaGetConfig.get.navigationMode=NavigationMode.WALK_AND_MINE;YunaGetConfig.get.tunnelMining=true;AutoSorterManager.start();});
            context.waitFor(mc->!AutoSorterManager.active()&&TaskManager.state==TaskState.COMPLETE,1200);
            check(game.getServer().computeOnServer(s->{var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)game.getConnection().getServerLevel().getBlockEntity(new BlockPos(8,100,1));int count=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.DIAMOND))count+=chest.getItem(i).getCount();return count==16;}),"Chest contents not confirmed on server");pass("chest walk-only route and existing sorter transaction workflow");

            prepare(context,game);context.runOnClient(mc->NavigationManager.request(new BlockPos(12,100,1),"Mining"));
            context.waitFor(mc->NavigationManager.state()==NavigationState.WALKING,400);
            context.getInput().holdKey(o->o.keyUp);context.waitTicks(3);context.getInput().releaseKey(o->o.keyUp);context.waitTicks(3);
            check(context.computeOnClient(mc->TaskManager.paused&&!NavigationManager.active()&&!mc.player.input.keyPresses.forward()),"Synthetic input not released after manual takeover");pass("manual movement cancels synthetic input");

            prepare(context,game);wall(game,Blocks.STONE.defaultBlockState(),true);targetOre(game);
            for(int z=-4;z<=16;z++)for(int y=100;y<=104;y++)DENIED.add(new BlockPos(5,y,z));
            context.runOnClient(mc->{YunaGetConfig.get.navigationMode=NavigationMode.WALK_AND_MINE;YunaGetConfig.get.tunnelMining=true;NavigationManager.requestBlock(new BlockPos(8,100,1));});
            context.waitFor(mc->NavigationManager.state()==NavigationState.FAILED,2200);
            check(context.computeOnClient(mc->NavigationManager.obstaclesRemoved()==0&&mc.player.getX()<5),"Rejected mining was treated as successful");pass("server mining refusal prevents movement and is not bypassed");
            context.takeScreenshot("yunaget-smart-navigation-v0.3");
            context.runOnClient(mc->TaskManager.stop());
        }finally{DENIED.clear();}
    }
    private static void prepare(ClientGameTestContext context,TestSingleplayerContext game){
        DENIED.clear();context.runOnClient(mc->{TaskManager.stop();NavigationManager.resetWorld();var c=YunaGetConfig.get;c.collector=c.sorter=c.oreEsp=c.targetXray=c.tunnelMining=false;c.selectedItems.clear();c.chests.clear();c.navigationMode=NavigationMode.WALK_ONLY;c.routeBudget=250;c.maxRouteNodes=8192;c.maxRouteDistance=64;c.debugNavigation=true;c.maxTunnelBlocks=24;c.minDurability=10;c.autoTool=true;});
        game.getServer().runOnServer(server->{var level=game.getConnection().getServerLevel();
            for(int x=-4;x<=16;x++)for(int z=-4;z<=16;z++){for(int y=100;y<=106;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);level.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);}
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-5,98,-5,18,108,18)))drop.discard();
            var player=game.getConnection().getServerPlayer();player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));player.setHealth(20);
        });
        game.getServer().runCommand("gamemode survival @a");game.getServer().runCommand("tp @a 1.5 100 1.5 0 0");
        game.getConnection().waitForClientboundPackets();context.waitTicks(10);context.runOnClient(mc->{TaskManager.paused=false;NavigationManager.resetWorld();});
    }
    private static void wall(TestSingleplayerContext game,BlockState state,boolean sealed){
        game.getServer().runOnServer(s->{var level=game.getConnection().getServerLevel();for(int z=-4;z<=(sealed?16:5);z++)for(int y=100;y<=104;y++)level.setBlock(new BlockPos(5,y,z),state,3);});
        game.getConnection().waitForClientboundPackets();
    }
    private static void targetOre(TestSingleplayerContext game){game.getServer().runOnServer(s->game.getConnection().getServerLevel().setBlock(new BlockPos(8,100,1),Blocks.DIAMOND_ORE.defaultBlockState(),3));game.getConnection().waitForClientboundPackets();}
    private static void pass(String name){LOG.info("NAV_SMOKE_PASS: {}",name);}
}
