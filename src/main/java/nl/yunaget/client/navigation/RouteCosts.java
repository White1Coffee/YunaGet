package nl.yunaget.client.navigation;
public final class RouteCosts {
    private RouteCosts() { }
    public static double movement(double rise){return 1+Math.max(0,rise)*.5+Math.max(0,-rise)*.2;}
    public static double mining(double expectedTicks,boolean preferExisting){
        if(!Double.isFinite(expectedTicks)||expectedTicks<0)throw new IllegalArgumentException("Invalid mining estimate");
        return (preferExisting?8:3)+expectedTicks*.1;
    }
}
