package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryMathTest {
    @Test void sumsPartialStacksAndIgnoresDifferentItems() {
        assertEquals(74,InventoryMath.count(List.of(new InventoryMath.Stack("diamond",64),new InventoryMath.Stack("diamond",10),new InventoryMath.Stack("coal",64)),"diamond"));
        assertEquals(0,InventoryMath.count(List.of(),"diamond"));
    }
    @Test void protectsBothReserveAndSelectedGoal() {
        assertEquals(36,InventoryMath.movable(100,64,20,64));
        assertEquals(0,InventoryMath.movable(42,32,0,64));
        assertEquals(64,InventoryMath.movable(128,64,64,16));
        assertEquals(0,InventoryMath.movable(32,32,64,0));
    }
    @Test void progressClampsAndHandlesEmptyGoal() {
        assertEquals(0.5,InventoryMath.progress(32,64));assertEquals(1,InventoryMath.progress(100,64));
        assertEquals(0,InventoryMath.progress(-1,64));assertEquals(1,InventoryMath.progress(0,0));
    }
}
