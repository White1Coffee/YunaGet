package nl.yunaget.client.navigation;
/** Feet height uses sixteenths of a block, including paths, slabs, snow and stair tops. */
public record Surface(int x,int height16,int z,int mined) {
    public double y() { return height16/16.0; }
    public Surface withoutMining() { return new Surface(x,height16,z,0); }
    public double horizontalDistance(Surface other) { return Math.abs(x-other.x)+Math.abs(z-other.z); }
}
