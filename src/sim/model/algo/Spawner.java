package sim.model.algo;

import sim.model.Agent;
import sim.model.Board;

public class Spawner extends MallFeature {
    private int pixelValue;
    private final Board board;

    public Spawner(int pixelValue, Board board) {
        this.pixelValue = pixelValue;
        this.board = board;
    }

    public int modifyHeuristicEstimate(int score) {
        return score;
    }

    public void performAction(Agent a) {
        assert a != null;
        board.setAgent(null, a.getPosition());
        a.setDead(true);
    }

    public int getPixelValue() {
        return pixelValue;
    }
}
