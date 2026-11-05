package nl.yunaget.client.navigation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SteeringTest {
    @Test void forwardAndStrafeMapToTheActualMinecraftCoordinateSystem(){
        for(float yaw:new float[]{0,90,180,270}){
            double angle=Math.toRadians(yaw),dx=-Math.sin(angle),dz=Math.cos(angle);
            var c=Steering.towards(dx,dz,yaw,false);assertEquals(1,c.forward(),1e-5);assertEquals(0,c.left(),1e-5);
        }
    }
    @Test void turningIsBoundedAndDoesNotRunBackwardIntoUnknownTerrain(){
        var c=Steering.towards(0,-1,0,false);assertTrue(Math.abs(c.yaw())<=28);
        assertEquals(0,c.forward());assertEquals(0,c.left());
    }
    @Test void aRightAngleTurnProducesRealInputAfterTurning(){
        var c=Steering.towards(-1,0,0,false);assertEquals(28,c.yaw(),1e-5);
        assertTrue(c.forward()>0);assertTrue(Math.abs(c.left())>0);
        assertTrue(c.forward()*c.forward()+c.left()*c.left()<=1.00001);
    }
    @Test void arrivalRequiresActualHeightAndPositionNotOnlyBlockCoordinates(){
        var goal=new Surface(2,1600,2,0);
        assertTrue(Steering.arrived(2.5,100,2.5,goal,true));
        assertFalse(Steering.arrived(2.01,100,2.01,goal,true));
        assertFalse(Steering.arrived(2.5,99,2.5,goal,true));
        assertFalse(Steering.arrived(2.5,100,2.5,goal,false));
        assertTrue(Steering.arrived(2.5,99.9375,2.5,goal,true));
    }
    @Test void zeroDistanceProducesNoMovement(){var c=Steering.towards(0,0,42,false);assertEquals(0,c.forward());assertEquals(0,c.left());}
}
