package mrtjp.projectred.api;

/**
 * Interface implemented by face parts to connect to various types of wires.
 */
public interface IConnectable
{
    /**
     * Called to check whether a wire/logic part can connect to this. If a part
     * returns true it is expected to immediately reflect the fact that it is
     * now connected to wire.
     * 
     * @param part The part asking for connection.
     * @param r The clockwise rotation about the attached face to
     * @return True to allow the wire connection.
     */
    public boolean connectStraight(IConnectable part, int r);

    /**
     * Connect for internals. If r is -1 for a face part. Return true for a
     * connection to the center part of the block.
     */
    public boolean connectInternal(IConnectable part, int r);

    /**
     * Connect for corners
     */
    public boolean connectCorner(IConnectable part, int r);

    /**
     * @return True if this part can reach around a corner to another part.
     */
    public boolean canConnectCorner(int r);
}
