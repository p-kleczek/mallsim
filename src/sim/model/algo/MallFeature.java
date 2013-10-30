package sim.model.algo;

import sim.model.Agent;

public abstract class MallFeature {
	public static int DEFAULT_PIXEL_VALUE = 0xFFFFFF;
	
    public abstract int modifyHeuristicEstimate(int score);

    public abstract void performAction(Agent a);

    public int getPixelValue() {
        return DEFAULT_PIXEL_VALUE;
    }
}