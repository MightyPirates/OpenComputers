package mrtjp.projectred.api;

/**
 * Interface implemented by face parts to connect to various types of wires.
 */
public interface IConnectable
{
    /**
     * Called to check whether a wire/logic part can connect to this. If this
     * returns true it is expected to immediately reflect the fact that it is
     * now connected to the part.
     *
     * @param part The part asking for connection.
     * @param r The clockwise rotation about the attached face for face part,
     *          ForgeDirection for non-face part or tile.
     * @param edgeRot The clockwise rotation of the common edge about the
     *                connecting face. -1 unless this is a non-part.
     * @return True to allow the wire connection.
     */
    public boolean connectStraight(IConnectable part, int r, int edgeRot);

    /**
     * Connect for internals. If r is -1 for a face part, return true for a
     * connection to the center part of the block.
     */
    public boolean connectInternal(IConnectable part, int r);

    /**
     * Connect for corners
     */
    public boolean connectCorner(IConnectable part, int r, int edgeRot);

    /**
     * @return True if this part can reach around a corner to another part.
     */
    public boolean canConnectCorner(int r);
}