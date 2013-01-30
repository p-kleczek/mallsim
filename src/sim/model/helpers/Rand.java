package sim.model.helpers;

import java.util.Random;

/*
 * Klasa używana, aby za każdym razem używać tego samego generatora (dzięki czemu wyniki symulacji są powtarzalne).
 *
 */
public class Rand {
	public static long seed = 0L;
	private static Random r = new Random(seed);

	public static void reseed() {
		r = new Random(seed);
	}

	public static int nextInt(int n) {
		return r.nextInt(n);
	}
	
	public static double nextDouble() {
		return r.nextDouble();
	}
	
	public static void setSeed(long seed) {
		r.setSeed(seed);
	}
}
