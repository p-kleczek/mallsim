package sim.control;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import sim.model.Agent;
import sim.model.Agent.MovementBehavior;
import sim.model.Board;
import sim.model.Cell;
import sim.model.Mall;
import sim.model.algo.Attractor;
import sim.model.algo.MallFeature;
import sim.model.algo.Ped4;
import sim.model.algo.SocialForce;
import sim.model.algo.Spawner;
import sim.model.helpers.Rand;
import sim.util.Logger;

public class ResourceManager {
	public static final int MALL_WALL = 0x0;
	public static final int MALL_PED4 = 0x7F;
	public static final int MALL_SOCIAL_FORCE = 0xFF;

	public static final int MAP_ATTRACTOR = 0x7F;
	public static final int MAP_SPAWNER = 0xFF;

	public static final String MAPS_FOLDER_PATH = "./data/malls/";

	/**
	 * Loads shopping mall data from an image file.
	 * 
	 * @param mallName
	 *            name of a map to be loaded
	 */
	public static Mall loadShoppingMall(String mapName) {
		String mallFile = MAPS_FOLDER_PATH + mapName + "_map.bmp";
		String featureMap = MAPS_FOLDER_PATH + mapName + "_feat.bmp";

		Mall mall = new Mall();

		Logger.log("Loading mall: " + mallFile + " with featuremap: "
				+ featureMap);

		BufferedImage mallImage = null;
		BufferedImage mapImage = null;

		Raster mallRaster = null;
		Raster mapRaster = null;

		Cell[][] grid = null;
		Board b = null;

		int h = 0;
		int w = 0;

		try {
			mallImage = ImageIO.read(new File(mallFile));
			mallRaster = mallImage.getData();

			mapImage = ImageIO.read(new File(featureMap));
			mapRaster = mapImage.getData();

			h = mallRaster.getHeight();
			w = mallRaster.getWidth();

			assert mapRaster.getHeight() == h;
			assert mapRaster.getWidth() == w;

			if (mapRaster.getHeight() != h || mapRaster.getWidth() != w) {
				throw new RuntimeException(
						"Mall file and fearturemap size do not match!");
			}

			int[] pixel = new int[3];
			grid = new Cell[h][w];

			b = new Board(grid);
			mall.setBoard(b);

			// Used to cache Attractors
			HashMap<Integer, MallFeature> features = new HashMap<Integer, MallFeature>();

			Logger.log("Creating board...");


			int accessibleFieldsCounter = 0;
			List<Point> ioPoints = new ArrayList<>();
			Point p = new Point();

			for (int i = 0; i < h; ++i) {
				for (int j = 0; j < w; ++j) {
					mallRaster.getPixel(j, i, pixel);

					// [type][context data 0][contex data 1]
					switch (pixel[0]) {
					case MALL_WALL:
						grid[i][j] = Cell.WALL;
						continue; // Skips also the feature map dispatch.

					case MALL_PED4:
						grid[i][j] = new Cell(Cell.Type.PASSABLE,
								Ped4.getInstance());
						break;

					case MALL_SOCIAL_FORCE:
						grid[i][j] = new Cell(Cell.Type.PASSABLE,
								SocialForce.getInstance());
						break;

					default:
						throw new RuntimeException("Invalid mall file value.");
					}

					mapRaster.getPixel(j, i, pixel);

					// TODO: bit-shift + OR
					int hash = pixel[0] * 255 * 255 + pixel[1] * 255 + pixel[2];

					// [type][context data 0][contex data 1]
					if (features.get(hash) != null) {
						grid[i][j].setFeature(features.get(hash));
					} else {
						switch (pixel[0]) {
						case MAP_ATTRACTOR:
							MallFeature att = new Attractor(0xff - pixel[1],
									0xff - pixel[2], hash);
							features.put(hash, att);
							grid[i][j].setFeature(att);
							break;

						case MAP_SPAWNER:
							MallFeature spawn = new Spawner(hash, b);
							features.put(hash, spawn);
							grid[i][j].setFeature(spawn);
							break;
						default:
							break;
						}
					}
					
					if (grid[i][j].isPassable())
						accessibleFieldsCounter++;
					
					p.setLocation(j, i);
					if (grid[i][j].getFeature() instanceof Spawner)
						ioPoints.add(new Point(j, i));
				}
			}
			
			mall.getBoard().setAccessibleFieldCount(accessibleFieldsCounter);
			mall.getBoard().setIoPoints(ioPoints);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		Logger.log("Board created!");

		Logger.log("Randomizing board...");

		randomize(b, h * w / 250);

		Logger.log("Board randomized!");

		Logger.log("Mall loaded!");

		return mall;
	}

	public static void randomize(Board b, int nAgents) {

		for (int i = 0; i < nAgents; i++) {
			Point p = new Point(Rand.nextInt(b.getWidth()), Rand.nextInt(b
					.getHeight()));

			if (b.getCell(p).isPassable()) {
				MovementBehavior mb = MovementBehavior.values()[Rand
						.nextInt(MovementBehavior.values().length)];
				b.setAgent(new Agent(mb), p);
			}
		}

	}
}