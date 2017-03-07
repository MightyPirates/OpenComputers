package li.cil.oc.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public final class WorldUtils {
    public static final int FLAG_BLOCK_UPDATE = 1;
    public static final int FLAG_SEND_TO_CLIENTS = 2;
    public static final int FLAG_SUPPRESS_RENDER_UPDATE = 4;
    public static final int FLAG_FORCE_RENDER_MAIN_THREAD = 8;
    public static final int FLAG_SUPPRESS_OBSERVER_UPDATE = 16;

    /**
     * Typical update, combination of flags used most often.
     */
    public static final int FLAG_REGULAR_UPDATE = 3;

    @Nullable
    public static TileEntity getTileEntityThreadsafe(final IBlockAccess world, final BlockPos pos) {
        return world instanceof ChunkCache ? ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : world.getTileEntity(pos);
    }

    // ----------------------------------------------------------------------- //

    private WorldUtils() {
    }
}
