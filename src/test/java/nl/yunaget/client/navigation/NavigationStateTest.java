package nl.yunaget.client.navigation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavigationStateTest {
    @Test void tunnelWorkflowRequiresExplicitMiningAndConfirmationPhases(){
        var path=new NavigationState[]{NavigationState.IDLE,NavigationState.PLANNING,NavigationState.WALKING,NavigationState.MINING_OBSTACLE,NavigationState.WAITING_FOR_BLOCK_UPDATE,NavigationState.WALKING,NavigationState.ARRIVED};
        for(int i=1;i<path.length;i++)assertTrue(path[i-1].allows(path[i]));
        assertFalse(NavigationState.PLANNING.allows(NavigationState.MINING_OBSTACLE));
        assertFalse(NavigationState.WAITING_FOR_BLOCK_UPDATE.allows(NavigationState.ARRIVED));
    }
    @Test void allPhasesCanBeCancelledPausedOrFailed(){
        for(var state:NavigationState.values()){assertTrue(state.allows(NavigationState.IDLE));assertTrue(state.allows(NavigationState.PAUSED));assertTrue(state.allows(NavigationState.FAILED));}
        assertFalse(NavigationState.PAUSED.active());assertFalse(NavigationState.ARRIVED.active());assertFalse(NavigationState.FAILED.active());
    }
    @Test void costsPreferToolsAndOrdinaryWalking(){
        assertTrue(RouteCosts.mining(5,true)<RouteCosts.mining(100,true));
        assertTrue(RouteCosts.movement(0)<RouteCosts.movement(1));
        assertTrue(RouteCosts.movement(0)<RouteCosts.mining(0,true));
        assertTrue(RouteCosts.mining(10,false)<RouteCosts.mining(10,true));
        assertThrows(IllegalArgumentException.class,()->RouteCosts.mining(Double.POSITIVE_INFINITY,true));
    }
    @Test void ownershipPriorityKeepsSorterAndCollectorFromBeingSilentlyOverwritten(){
        assertTrue(NavigationOwner.AUTO_SORTER.priority>NavigationOwner.DROP_COLLECTOR.priority);
        assertTrue(NavigationOwner.DROP_COLLECTOR.priority>NavigationOwner.MINING.priority);
        assertEquals(NavigationOwner.AUTO_SORTER,NavigationOwner.legacy("Sorter"));
    }
}
