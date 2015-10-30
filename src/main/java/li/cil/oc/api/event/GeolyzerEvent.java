package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import li.cil.oc.api.network.EnvironmentHost;

import java.util.HashMap;
import java.util.Map;

/**
 * This event is fired by the geolyzer block/upgrade.
 * <p/>
 * When cancelling this event, the respective method will bail and report
 * that the operation failed.
 */
@Cancelable
public abstract class GeolyzerEvent extends Event {
    /**
     * The container of the geolyzer component. This can either be the
     * geolyzer block, or something with the geolyzer upgrade (a robot).
     */
    public final EnvironmentHost host;

    /**
     * The options the operation was invoked with.
     */
    public final Map<?, ?> options;

    protected GeolyzerEvent(EnvironmentHost host, Map<?, ?> options) {
        this.host = host;
        this.options = options;
    }

    /**
     * Long-distance scan, getting quantified information about blocks around
     * the geolyzer. By default this will yield a (noisy) listing of the
     * hardness of the blocks.
     * <p/>
     * The bounds are guaranteed to not define a volume larger than 64.
     * Resulting data should be written to the {@link #data} array such that
     * <code>index = x + z*w + y*w*d</code>, with <code>w = maxX - minX</code>
     * and <code>d = maxZ - minZ</code> (<tt>h</tt> meaning height, <tt>d</tt>
     * meaning depth).
     */
    public static class Scan extends GeolyzerEvent {
        /**
         * The <em>relative</em> minimal x coordinate of the box being scanned (inclusive).
         */
        public final int minX;

        /**
         * The <em>relative</em> minimal y coordinate of the box being scanned (inclusive).
         */
        public final int minY;

        /**
         * The <em>relative</em> minimal z coordinate of the box being scanned (inclusive).
         */
        public final int minZ;

        /**
         * The <em>relative</em> maximal x coordinate of the box being scanned (inclusive).
         */
        public final int maxX;

        /**
         * The <em>relative</em> maximal y coordinate of the box being scanned (inclusive).
         */
        public final int maxY;

        /**
         * The <em>relative</em> maximal z coordinate of the box being scanned (inclusive).
         */
        public final int maxZ;

        /**
         * The data for the column of blocks being scanned, which is an
         * interval around the geolyzer itself, with the geolyzer block
         * being at index 32.
         */
        public final float[] data = new float[64];

        public Scan(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            super(host, options);
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    /**
     * Zero-range scan, getting in-depth information about blocks directly
     * adjacent to the geolyzer. By default this will yield the block's
     * name, metadata, hardness and harvest information.
     */
    public static class Analyze extends GeolyzerEvent {
        /**
         * The position of the block to scan.
         * <p/>
         * Note: get the world via the host if you need it.
         */
        public final int x, y, z;

        /**
         * The retrieved data for the block being scanned.
         */
        public final Map<String, Object> data = new HashMap<String, Object>();

        public Analyze(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
            super(host, options);
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
