package mods.railcraft.api.tracks;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import java.util.*;
import java.util.logging.Level;
import mods.railcraft.api.core.ITextureLoader;

/**
 * The TrackRegistry is part of a system that allows 3rd party addons to simply,
 * quickly, and easily define new Tracks with unique behaviors without requiring
 * that any additional block ids be used.
 *
 * All the tracks in RailcraftProxy are implemented using this system 100%
 * (except for Gated Tracks and Switch Tracks which have some custom render
 * code).
 *
 * To define a new track, you need to define a TrackSpec and create a
 * ITrackInstance.
 *
 * The TrackSpec contains basic constant information about the Track, while the
 * TrackInstace controls how an individual Track block interact with the world.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see TrackSpec
 * @see ITrackInstance
 * @see TrackInstanceBase
 */
public class TrackRegistry {

    private static Map<Short, TrackSpec> trackSpecs = new HashMap<Short, TrackSpec>();
    private static Set<Short> invalidSpecs = new HashSet<Short>();
    private static List<ITextureLoader> iconLoaders = new ArrayList<ITextureLoader>();

    /**
     * Provides a means to hook into the texture loader of my Track block.
     *
     * You should load your track textures in the ITextureLoader and put them
     * someplace to be fed to the TrackSpec/TrackInstance later.
     *
     * This should be called before the Post-Init Phase, during the Init or
     * Pre-Init Phase.
     *
     * @param iconLoader
     */
    public static void registerIconLoader(ITextureLoader iconLoader) {
        iconLoaders.add(iconLoader);
    }

    public static List<ITextureLoader> getIconLoaders() {
        return iconLoaders;
    }

    public static class TrackIdConflictException extends RuntimeException {

        public TrackIdConflictException(String msg) {
            super(msg);
        }

    }

    /**
     * Registers a new TrackSpec. This should be called before the Post-Init
     * Phase, during the Init or Pre-Init Phase.
     *
     * @param trackSpec
     */
    public static void registerTrackSpec(TrackSpec trackSpec) {
        if (trackSpecs.put(trackSpec.getTrackId(), trackSpec) != null) {
            throw new TrackIdConflictException("TrackId conflict detected, please adjust your config or contact the author of the " + trackSpec.getTrackTag());
        }
    }

    /**
     * Returns a cached copy of a TrackSpec object.
     *
     * @param trackId
     * @return
     */
    public static TrackSpec getTrackSpec(int trackId) {
        Short id = (short) trackId;
        TrackSpec spec = trackSpecs.get(id);
        if (spec == null) {
            if (!invalidSpecs.contains(id)) {
                FMLRelaunchLog.log("Railcraft", Level.WARNING, "Unknown Track Spec ID(%d), reverting to normal track", trackId);
                invalidSpecs.add(id);
            }
            id = -1;
            spec = trackSpecs.get(id);
        }
        return spec;
    }

    /**
     * Returns all Registered TrackSpecs.
     *
     * @return list of TrackSpecs
     */
    public static Map<Short, TrackSpec> getTrackSpecs() {
        return trackSpecs;
    }

}
