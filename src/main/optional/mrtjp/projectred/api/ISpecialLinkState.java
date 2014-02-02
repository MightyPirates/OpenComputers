package mrtjp.projectred.api;

import java.util.List;

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
     * This method should utilize the given tile to find all other tiles that
     * connects back and forth. For example, if we have TesseractA (param tile)
     * connected to TesseractB, which connects to a few pipes, this method
     * should return the tile of all pipes connected to TesseractB.
     * 
     * The given tile is what the pipes found, the returned list is what the
     * pipe should consider as found.
     * 
     * @param te The tile in question.
     * @return A list of all connected pipes (as tiles, should be
     *         TileMultipart). This MUST be null if there are no connections of
     *         this special type.
     */
    public List<TileEntity> getLinks(TileEntity te);
}
