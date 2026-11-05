package nl.yunaget.client.navigation;
public enum NavigationState {
    IDLE, PLANNING, WALKING, MINING_OBSTACLE, WAITING_FOR_BLOCK_UPDATE, REPLANNING, ARRIVED, PAUSED, FAILED;
    public boolean active() { return switch(this) { case PLANNING,WALKING,MINING_OBSTACLE,WAITING_FOR_BLOCK_UPDATE,REPLANNING -> true; default -> false; }; }
    public boolean allows(NavigationState next) {
        if(next==this||next==IDLE||next==PAUSED||next==FAILED)return true;
        return switch(this) {
            case IDLE,ARRIVED,FAILED,PAUSED -> next==PLANNING;
            case PLANNING,REPLANNING -> next==WALKING||next==ARRIVED||next==PLANNING;
            case WALKING -> next==MINING_OBSTACLE||next==REPLANNING||next==ARRIVED;
            case MINING_OBSTACLE -> next==WAITING_FOR_BLOCK_UPDATE||next==REPLANNING||next==WALKING;
            case WAITING_FOR_BLOCK_UPDATE -> next==WALKING||next==REPLANNING;
        };
    }
}
