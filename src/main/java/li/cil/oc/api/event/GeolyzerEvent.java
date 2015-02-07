package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import li.cil.oc.api.driver.EnvironmentHost;

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
     * Note: the y coordinate is computed as <tt>geolyzer.y - 32 + data.index</tt>.
     */
    public static class Scan extends GeolyzerEvent {
        /**
         * The <em>relative</em> x coordinate of the column being scanned.
         */
        public final int scanX;

        /**
         * The <em>relative</em> z coordinate of the column being scanned.
         */
        public final int scanZ;

        /**
         * The data for the column of blocks being scanned, which is an
         * interval around the geolyzer itself, with the geolyzer block
         * being at index 32.
         */
        public final float[] data = new float[64];

        public Scan(EnvironmentHost host, Map<?, ?> options, int scanX, int scanZ) {
            super(host, options);
            this.scanX = scanX;
            this.scanZ = scanZ;
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
