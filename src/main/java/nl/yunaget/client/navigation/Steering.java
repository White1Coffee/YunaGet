package nl.yunaget.client.navigation;

/** Ordinary input in the player's current coordinate system, with bounded turning and arrival tolerance. */
public final class Steering {
    private Steering() { }
    public record Command(float yaw,float forward,float left,boolean jump) { }
    public static Command towards(double dx,double dz,float currentYaw,boolean jump) {
        if(Math.hypot(dx,dz)<1e-6)return new Command(currentYaw,0,0,jump);
        float desired=(float)Math.toDegrees(Math.atan2(dz,dx))-90;
        float error=wrap(desired-currentYaw), yaw=currentYaw+Math.clamp(error,-28,28);
        if(Math.abs(wrap(desired-yaw))>75)return new Command(yaw,0,0,false);
        double length=Math.hypot(dx,dz),r=Math.toRadians(yaw);
        float forward=(float)((-dx*Math.sin(r)+dz*Math.cos(r))/length);
        float left=(float)((dx*Math.cos(r)+dz*Math.sin(r))/length);
        return new Command(yaw,forward,left,jump);
    }
    public static boolean arrived(double x,double y,double z,Surface target,boolean grounded) {
        return grounded&&Math.hypot(x-(target.x()+.5),z-(target.z()+.5))<=.24&&Math.abs(y-target.y())<=.28;
    }
    private static float wrap(float angle){float a=angle%360;return a>=180?a-360:a< -180?a+360:a;}
}
