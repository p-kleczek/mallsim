package sim.control;

import java.util.Random;
import javax.imageio.*;
import java.io.*;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;

import sim.util.Logger;
import sim.model.Mall;
import sim.model.Board;
import sim.model.Cell;
import sim.model.Agent;
import sim.model.algo.Ped4;

public class ResourceManager {

    /**
     * Loads shopping mall data from an image file.
     * TODO: Needs some decent logging.
     */
    public Mall loadShoppingMall(String filename) {
        Logger.log("Loading mall: " + filename);

        BufferedImage bi = null;
        Raster img = null;
        Cell[][] grid = null;

        int h = 0;
        int w = 0;

        try {
            bi = ImageIO.read(new File(filename));
            img = bi.getData();

            h = img.getHeight();
            w = img.getWidth();
            int[] pixel = new int[3];

            grid = new Cell[h][w];

            Logger.log("Creating board...");

            for(int i = 0; i < h; ++i) {
                for(int j = 0; j < w; ++j) {
                    img.getPixel(j, i, pixel);

                    if(pixel[0] == 0) {
                        grid[i][j] = Cell.WALL;
                    }
                    else {
                        grid[i][j] = new Cell(Cell.Type.PASSABLE, Ped4.getInstance());
                    }

                    // TODO Dispatch on the pixel value:
                    // TODO Holders
                    // TODO Queues
                    // TODO Attractors

                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Board b = new Board(grid);
        Logger.log("Board created!");

        Logger.log("Randomizing board...");

        randomize(b, h*w/15);

        Logger.log("Board randomized!");

        Logger.log("Mall loaded!");
        return new Mall(b);
    }

    // TODO
    public Agent loadAgent(String filename) {
        return new Agent();
    }

    private void randomize(Board b, int n) {
        Random r = new Random();
        Dimension d = b.getDimension();
        for (int i = 0; i < n; i++) {
            Cell c = b.getCell(new Point(r.nextInt(d.width), r.nextInt(d.height)));

            if(c != Cell.WALL)
              c.setAgent(new Agent());
        }

    }
}