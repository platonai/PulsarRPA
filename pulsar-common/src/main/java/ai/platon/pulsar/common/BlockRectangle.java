package ai.platon.pulsar.common;

import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.awt.*;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * Created by vincent on 16-6-15.
 *
 * the screen coordinate system:
 * 0------>x
 * |
 * |   *
 * v
 * y
 * the coordinate of the star is (x, y)
 */
public class BlockRectangle extends Rectangle implements Comparable<BlockRectangle> {

    public BlockRectangle() {
        super();
    }

    public BlockRectangle(Rectangle rect) {
        super(rect);
    }

    public BlockRectangle(int left, int top) {
        super(left, top);
    }

    public BlockRectangle(int left, int top, int width, int height) {
        this.x = left;
        this.y = top;
        this.width = width;
        this.height = height;
    }

    public BlockRectangle(double left, double top, double width, double height) {
        this.x = (int) left;
        this.y = (int) top;
        this.width = (int) width;
        this.height = (int) height;
    }

    public BlockRectangle(Point pos, Dimension dim) {
        super(pos, dim);
    }

    public BlockRectangle(Point pos) {
        super(pos);
    }

    public BlockRectangle(Dimension dim) {
        super(dim);
    }

    // String format: [left, top, width, height]
    public static BlockRectangle parse(String simpleString) {
        String[] parts = simpleString.replace(SPACE, EMPTY).split(",");
        Integer[] vi = new Integer[4];

        for (int i = 0; i < parts.length; ++i) {
            vi[i] = NumberUtils.toInt(parts[i], -1);
        }

        return new BlockRectangle(vi[0], vi[1], vi[2], vi[3]);
    }

    /**
     * Align to grid
     */
    public static int alignToGrid(int n, int windowSize) {
        if (windowSize == 1) {
            return n;
        }

        return n / windowSize * windowSize;
    }

    public double getLeft() {
        return getX();
    }

    public double getTop() {
        return getY();
    }

//    public void setTop(int t) {
//        setLocation(t, y);
//    }

//    public void setLeft(int l) {
//        setLocation(x, l);
//    }

    public double getArea() {
        return width * height;
    }

    public void setWidth(int w) {
        setSize(w, this.height);
    }

    public void setHeight(int h) {
        setSize(this.width, h);
    }

    public BlockRectangle clone() {
        return new BlockRectangle(x, y, width, height);
    }

    public BlockRectangle trim(int windowSize) {
        return trim(windowSize, 1, 1, 1);
    }

    public BlockRectangle trim(int windowSize, int windowSize2) {
        return trim(windowSize, windowSize2, 1, 1);
    }

    public BlockRectangle trim(int windowSize, int windowSize2, int windowSize3) {
        return trim(windowSize, windowSize2, windowSize3, 1);
    }

    public BlockRectangle trim(int xGrid, int yGrid, int wGrid, int hGrid) {
        int x2 = alignToGrid(x, xGrid);
        int y2 = alignToGrid(y, yGrid);
        int width2 = alignToGrid(width, wGrid);
        int height2 = alignToGrid(height, hGrid);

        return new BlockRectangle(x2, y2, width2, height2);
    }

    public BlockRectangle trim(BlockRectangle grid) {
        return trim(grid.x, grid.y, grid.width, grid.height);
    }

    public BlockRectangle trimAll(int windowSize) {
        return trim(windowSize, windowSize, windowSize, windowSize);
    }

    @Override
    public int compareTo(@Nonnull BlockRectangle blockRectangle) {
        int r = x - blockRectangle.x;

        if (r == 0) {
            r = y - blockRectangle.y;
        }

        if (r == 0) {
            r = width - blockRectangle.width;
        }

        if (r == 0) {
            r = height - blockRectangle.height;
        }

        return r;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public String format() {
        return "left:" + x + ", top:" + y + ", width:" + width + ", height:" + height;
    }

    public String toStyle() {
        return "left:" + x + "px; top:" + y + "px; width:" + width + "px; height:" + height + "px";
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + width + ", " + height;
    }
}
