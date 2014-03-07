package mrtjp.projectred.api;

import net.minecraft.tileentity.TileEntity;

/**
 * Allows adding of special tile entities that point to other BasicPipeParts.
 * Used for allowing things such as TE Tesseracts to be used as via points for
 * Link-State path finding, which is used to establish a connection from one
 * routed pipe to another. This is registered through the ProjectRedAPI.
 */
public interface ISpecialLinkState
{
    /**
     * If this special link state should be used for the given tile, this
     * should return true.
     *
     * @param tile The tile in question
     * @return True if this special link state applies to the tile.
     */
    public boolean matches(TileEntity tile);

    /**
     * This method should utilize the given tile to find all other tiles that
     * connects back and forth. For example, if we have TesseractA (param tile)
     * connected to TesseractB, which connects to a pipe, this method
     * should return the tile (TileMultipart) of that pipe.
     *
     * The given tile is what the pipes found, the returned list is what the
     * pipe should consider as found.
     *
     * @param tile The tile in question.
     * @return A list of all connected pipes (as TileMultiparts).
     */
    public TileEntity getLink(TileEntity tile);
}
