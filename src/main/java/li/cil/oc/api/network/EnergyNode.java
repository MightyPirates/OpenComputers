package li.cil.oc.api.network;

/**
 * Interface for nodes that act as power connectors between their network and
 * some power producer or consumer.
 * <p/>
 * For each connector a buffer is managed. Its size is initialized via the
 * factory function in the network API, but can also be configured later on.
 * Its current fill level can be queried and manipulated as desired.
 * <p/>
 * Each connector can take two roles: it can be a <em>producer</em>, feeding
 * power into the network, or it can be a <em>consumer</em>, requiring power
 * from the network to power something (or it can be both). This depends
 * entirely on how you call {@link #changeEnergy}, i.e. on whether you
 * fill up the connectors buffer or drain it.
 * <p/>
 * To feed power into the network, simply fill up the buffer, to consume power
 * take power from the buffer. The network will balance the power between all
 * buffers connected to it. The algorithm goes as follows: if there was a change
 * to some buffer, computer the average power available in all buffers. Build
 * two sets: those of buffers with above-average level, and those with below-
 * average fill. From all above-average buffers take so much energy that they
 * remain just above average fill (but only take integral values - this is to
 * avoid floating point errors causing trouble). Distribute the collected energy
 * equally among the below-average buffers (as good as possible).
 */
public interface EnergyNode extends Node {
    /**
     * The energy stored in the local buffer.
     */
    double getEnergyStored();

    /**
     * The size of the local buffer.
     */
    double getEnergyCapacity();

    /**
     * Change the size of the connectors local buffer.
     * <p/>
     * If the size is reduced, any superfluous energy is distributed across
     * other connectors' buffers in the network, if possible. Any surplus
     * energy that cannot be stored in other buffers will be lost.
     * <p/>
     * Note that this automatically called when the connector is disconnected
     * from its network to set its buffer size to zero and distribute its
     * energy to other connectors in the network.
     *
     * @param size the new size of the local buffer. Note that this is capped
     *             to a minimum of zero, i.e. if a negative value is passed the
     *             size will be set to zero.
     */
    void setEnergyCapacity(final double size);
}
