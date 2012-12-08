package sim.control;

import java.util.HashMap;
import java.util.Random;
import javax.imageio.*;
import java.io.*;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;

import sim.util.Logger;
import sim.model.Agent.MovementBehavior;
import sim.model.Mall;
import sim.model.Board;
import sim.model.Cell;
import sim.model.Agent;
import sim.model.algo.Ped4;
import sim.model.algo.SocialForce;
import sim.model.algo.MallFeature;
import sim.model.algo.Attractor;
import sim.model.helpers.Misc;

public class ResourceManager {
    public static final int MALL_WALL = 0x0;
    public static final int MALL_PED4 = 0x7F;
    public static final int MALL_SOCIAL_FORCE = 0xFF;

    public static final int MAP_QUEUE = 0xFF;
    public static final int MAP_ATTRACTOR = 0x77;
    public static final int MAP_HOLDER = 0xFF;

    /**
     * Loads shopping mall data from an image file.
     */

    public void loadShoppingMall(String mallFile, String featureMap) {
        Logger.log("Loading mall: " + mallFile + " with featuremap: " + featureMap);

        BufferedImage mallImage = null;
        BufferedImage mapImage = null;

        Raster mall = null;
        Raster map = null;

        Cell[][] grid = null;

        int h = 0;
        int w = 0;

        try {
            mallImage = ImageIO.read(new File(mallFile));
            mall = mallImage.getData();

            mapImage = ImageIO.read(new File(featureMap));
            map = mapImage.getData();

            h = mall.getHeight();
            w = mall.getWidth();

            assert map.getHeight() == h;
            assert map.getWidth() == w;

            if(map.getHeight() != h || map.getWidth() != w) {
                throw new RuntimeException("Mall file and fearturemap size do not match!");
            }

            int[] pixel = new int[3];
            grid = new Cell[h][w];

            // Used to cache Attractors
            HashMap<Integer, MallFeature> attractors = new HashMap<Integer, MallFeature>();

            Logger.log("Creating board...");

            for(int i = 0; i < h; ++i) {
                for(int j = 0; j < w; ++j) {
                    mall.getPixel(j, i, pixel);

                    // [type][context data 0][contex data 1]
                    switch(pixel[0]) {
                        case MALL_WALL:
                            grid[i][j] = Cell.WALL;
                        continue; // Skips also the feature map dispatch.

                        case MALL_PED4:
                            grid[i][j] = new Cell(Cell.Type.PASSABLE, Ped4.getInstance());
                        break;

                        case MALL_SOCIAL_FORCE:
                            grid[i][j] = new Cell(Cell.Type.PASSABLE, SocialForce.getInstance());
                        break;

                        default: throw new RuntimeException("Invalid mall file value.");
                    }

                    map.getPixel(j, i, pixel);

                    // [type][context data 0][contex data 1]
                    switch(pixel[0]) {
                        case MAP_ATTRACTOR:
                            if(attractors.get(pixel[1]) != null) {
                                grid[i][j].setFeature(attractors.get(pixel[1]));
                            }
                            else {
                                MallFeature att = new Attractor(pixel[1]);
                                attractors.put(pixel[1], att);
                                grid[i][j].setFeature(att);
                            }
                        break;

                        default:
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Board b = new Board(grid);
        Mall.getInstance().setBoard(b);

        Logger.log("Board created!");

        Logger.log("Randomizing board...");

        randomize(b, h*w/25);

        Logger.log("Board randomized!");

        Logger.log("Mall loaded!");
    }


    public Agent loadAgent(String agentFile) {
        return new Agent(MovementBehavior.DYNAMIC);
    }


    private void randomize(Board b, int nAgents) {
        Random r = new Random();
        Dimension d = b.getDimension();

        for (int i = 0; i < nAgents; i++) {
            Point p = new Point(r.nextInt(d.width), r.nextInt(d.height));

            if(b.getCell(p).isPassable()) {
                Misc.setAgent(new Agent(MovementBehavior.DYNAMIC), p);
            }
        }

    }
}