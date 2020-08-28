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
 * 0------\>x
 * |
 * |   *
 * v
 * y
 * the coordinate of the star is (x, y)
 *
 * @author vincent
 * @version $Id: $Id
 */
public class BlockRectangle extends Rectangle implements Comparable<BlockRectangle> {

    /**
     * <p>Constructor for BlockRectangle.</p>
     */
    public BlockRectangle() {
        super();
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param rect a {@link java.awt.Rectangle} object.
     */
    public BlockRectangle(Rectangle rect) {
        super(rect);
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param left a int.
     * @param top a int.
     */
    public BlockRectangle(int left, int top) {
        super(left, top);
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param left a int.
     * @param top a int.
     * @param width a int.
     * @param height a int.
     */
    public BlockRectangle(int left, int top, int width, int height) {
        this.x = left;
        this.y = top;
        this.width = width;
        this.height = height;
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param left a double.
     * @param top a double.
     * @param width a double.
     * @param height a double.
     */
    public BlockRectangle(double left, double top, double width, double height) {
        this.x = (int) left;
        this.y = (int) top;
        this.width = (int) width;
        this.height = (int) height;
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param pos a {@link java.awt.Point} object.
     * @param dim a {@link java.awt.Dimension} object.
     */
    public BlockRectangle(Point pos, Dimension dim) {
        super(pos, dim);
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param pos a {@link java.awt.Point} object.
     */
    public BlockRectangle(Point pos) {
        super(pos);
    }

    /**
     * <p>Constructor for BlockRectangle.</p>
     *
     * @param dim a {@link java.awt.Dimension} object.
     */
    public BlockRectangle(Dimension dim) {
        super(dim);
    }

    // String format: [left, top, width, height]
    /**
     * <p>parse.</p>
     *
     * @param simpleString a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
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
     *
     * @param n a int.
     * @param windowSize a int.
     * @return a int.
     */
    public static int alignToGrid(int n, int windowSize) {
        if (windowSize == 1) {
            return n;
        }

        return n / windowSize * windowSize;
    }

    /**
     * <p>getLeft.</p>
     *
     * @return a double.
     */
    public double getLeft() {
        return getX();
    }

    /**
     * <p>getTop.</p>
     *
     * @return a double.
     */
    public double getTop() {
        return getY();
    }

//    public void setTop(int t) {
//        setLocation(t, y);
//    }

//    public void setLeft(int l) {
//        setLocation(x, l);
//    }

    /**
     * <p>getArea.</p>
     *
     * @return a double.
     */
    public double getArea() {
        return width * height;
    }

    /**
     * <p>setWidth.</p>
     *
     * @param w a int.
     */
    public void setWidth(int w) {
        setSize(w, this.height);
    }

    /**
     * <p>setHeight.</p>
     *
     * @param h a int.
     */
    public void setHeight(int h) {
        setSize(this.width, h);
    }

    /**
     * <p>clone.</p>
     *
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle clone() {
        return new BlockRectangle(x, y, width, height);
    }

    /**
     * <p>trim.</p>
     *
     * @param windowSize a int.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trim(int windowSize) {
        return trim(windowSize, 1, 1, 1);
    }

    /**
     * <p>trim.</p>
     *
     * @param windowSize a int.
     * @param windowSize2 a int.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trim(int windowSize, int windowSize2) {
        return trim(windowSize, windowSize2, 1, 1);
    }

    /**
     * <p>trim.</p>
     *
     * @param windowSize a int.
     * @param windowSize2 a int.
     * @param windowSize3 a int.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trim(int windowSize, int windowSize2, int windowSize3) {
        return trim(windowSize, windowSize2, windowSize3, 1);
    }

    /**
     * <p>trim.</p>
     *
     * @param xGrid a int.
     * @param yGrid a int.
     * @param wGrid a int.
     * @param hGrid a int.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trim(int xGrid, int yGrid, int wGrid, int hGrid) {
        int x2 = alignToGrid(x, xGrid);
        int y2 = alignToGrid(y, yGrid);
        int width2 = alignToGrid(width, wGrid);
        int height2 = alignToGrid(height, hGrid);

        return new BlockRectangle(x2, y2, width2, height2);
    }

    /**
     * <p>trim.</p>
     *
     * @param grid a {@link ai.platon.pulsar.common.BlockRectangle} object.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trim(BlockRectangle grid) {
        return trim(grid.x, grid.y, grid.width, grid.height);
    }

    /**
     * <p>trimAll.</p>
     *
     * @param windowSize a int.
     * @return a {@link ai.platon.pulsar.common.BlockRectangle} object.
     */
    public BlockRectangle trimAll(int windowSize) {
        return trim(windowSize, windowSize, windowSize, windowSize);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * <p>format.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String format() {
        return "left:" + x + ", top:" + y + ", width:" + width + ", height:" + height;
    }

    /**
     * <p>toStyle.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toStyle() {
        return "left:" + x + "px; top:" + y + "px; width:" + width + "px; height:" + height + "px";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return x + ", " + y + ", " + width + ", " + height;
    }
}
