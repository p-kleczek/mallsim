package sim.model.algo;

import java.util.Map;

import sim.model.Agent;
import sim.model.Board;

public class Empty implements MovementAlgorithm {

	private static MovementAlgorithm instance = new Empty();

	private Empty() {
	}

	public static MovementAlgorithm getInstance() {
		return instance;
	}

	@Override
	public void prepare(Board b, Agent a) {
	}

	@Override
	public void nextIterationStep(Board b, Agent a, Map<Agent, Integer> mpLeft) {
	}

}
