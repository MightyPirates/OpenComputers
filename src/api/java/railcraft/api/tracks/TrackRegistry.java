package mods.railcraft.api.tracks;

import cpw.mods.fml.common.FMLLog;
import java.util.*;
import mods.railcraft.api.core.ITextureLoader;
import org.apache.logging.log4j.Level;

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

    private static final Map<Short, TrackSpec> trackSpecsFromID = new HashMap<Short, TrackSpec>();
    private static final Map<String, TrackSpec> trackSpecsFromTag = new HashMap<String, TrackSpec>();
    private static final Set<Short> invalidSpecIDs = new HashSet<Short>();
    private static final Set<String> invalidSpecTags = new HashSet<String>();
    private static final List<ITextureLoader> iconLoaders = new ArrayList<ITextureLoader>();

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

    public static class TrackSpecConflictException extends RuntimeException {

        public TrackSpecConflictException(String msg) {
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
        if (trackSpecsFromID.put(trackSpec.getTrackId(), trackSpec) != null)
            throw new TrackSpecConflictException("TrackId conflict detected, please adjust your config or contact the author of the " + trackSpec.getTrackTag());
        if (trackSpecsFromTag.put(trackSpec.getTrackTag(), trackSpec) != null)
            throw new TrackSpecConflictException("TrackTag conflict detected, please adjust your config or contact the author of the " + trackSpec.getTrackTag());
    }

    /**
     * Returns a cached copy of a TrackSpec object.
     *
     * @param trackId
     * @return
     */
    public static TrackSpec getTrackSpec(int trackId) {
        Short id = (short) trackId;
        TrackSpec spec = trackSpecsFromID.get(id);
        if (spec == null) {
            if (!invalidSpecIDs.contains(id)) {
                FMLLog.log("Railcraft", Level.WARN, "Unknown Track Spec ID(%d), reverting to normal track", trackId);
                invalidSpecIDs.add(id);
            }
            id = -1;
            spec = trackSpecsFromID.get(id);
        }
        return spec;
    }

    /**
     * Returns a cached copy of a TrackSpec object.
     *
     * @param trackTag
     * @return
     */
    public static TrackSpec getTrackSpec(String trackTag) {
        trackTag = trackTag.toLowerCase(Locale.ENGLISH);
        TrackSpec spec = trackSpecsFromTag.get(trackTag);
        if (spec == null) {
            if (!invalidSpecTags.contains(trackTag)) {
                FMLLog.log("Railcraft", Level.WARN, "Unknown Track Spec Tag(%s), reverting to normal track", trackTag);
                invalidSpecTags.add(trackTag);
            }
            spec = trackSpecsFromTag.get("railcraft:default");
        }
        return spec;
    }

    /**
     * Returns all Registered TrackSpecs.
     *
     * @return list of TrackSpecs
     */
    public static Map<Short, TrackSpec> getTrackSpecIDs() {
        return trackSpecsFromID;
    }

    /**
     * Returns all Registered TrackSpecs.
     *
     * @return list of TrackSpecs
     */
    public static Map<String, TrackSpec> getTrackSpecTags() {
        return trackSpecsFromTag;
    }

}
