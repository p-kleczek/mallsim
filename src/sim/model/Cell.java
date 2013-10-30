package sim.model;

import sim.model.algo.Empty;
import sim.model.algo.MallFeature;
import sim.model.algo.MovementAlgorithm;
import sim.model.algo.Ped4.LaneDirection;

public class Cell {
	public static final Cell WALL = new Cell(Type.BLOCKED, Empty.getInstance());

	public enum Type {
		PASSABLE, BLOCKED
	}

	private final Type type;
	private Agent agent = null;
	private MovementAlgorithm algorithm = null;
	private MallFeature feature = null;

	private LaneDirection laneDirection = LaneDirection.EMPTY;

	/**
	 * Licznik odwiedzin - ile razy agenci wchodzili na dane pole.
	 */
	private int visitsCounter;

	private int forceValue;
	private int forceValue4Rendering;

	public Cell(Type type, MovementAlgorithm algo, MallFeature feature) {
		this(type, algo);
		setFeature(feature);
	}

	public Cell(Type type, MovementAlgorithm algo) {
		super();
		this.type = type;
		this.algorithm = algo;
		forceValue = 0;
		clearVisitsCounter();
	}

	public Agent getAgent() {
		return agent;
	}

	public void setAgent(Agent agent) {
		this.agent = agent;
	}

	public MallFeature getFeature() {
		return feature;
	}

	public void setFeature(MallFeature feature) {
		this.feature = feature;
	}

	public Type getType() {
		return type;
	}

	public MovementAlgorithm getAlgorithm() {
		return algorithm;
	}

	public boolean isPassable() {
		return (type != Type.BLOCKED);
	}

	public int getForceValue() {
		return forceValue;
	}

	public int getForceValue4Rendering() {
		return forceValue4Rendering;
	}

	public void setForceValue(int forceValue) {
		if (type == Type.BLOCKED)
			return;
		this.forceValue = forceValue;
	}

	public void flipForceValue() {
		forceValue4Rendering = forceValue;
	}

	void changeForce(int forceValue) {
		if (type == Type.BLOCKED)
			return;
		this.forceValue += forceValue;
	}

	public void setAlgorithm(MovementAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	public int getVisitsCounter() {
		return visitsCounter;
	}

	public void clearVisitsCounter() {
		visitsCounter = 0;
	}

	public void incrementVisitsCounter() {
		visitsCounter++;
	}

	public LaneDirection getLaneDirection() {
		return laneDirection;
	}

	public void setLaneDirection(LaneDirection laneDirection) {
		this.laneDirection = laneDirection;
	}

}
