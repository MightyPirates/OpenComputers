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
 * entirely on how you call {@link #changeBuffer}, i.e. on whether you
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
public interface Connector extends Node {
    /**
     * The energy stored in the local buffer.
     */
    double localBuffer();

    /**
     * The size of the local buffer.
     */
    double localBufferSize();

    /**
     * The accumulative energy stored across all buffers in the node's network.
     */
    double globalBuffer();

    /**
     * The accumulative size of all buffers in the node's network.
     */
    double globalBufferSize();

    /**
     * Try to apply the specified delta to the <em>global</em> buffer.
     * <p/>
     * This can be used to apply reactionary power changes. For example, a
     * screen may require a certain amount of energy to refresh its display when
     * a program tries to display text on it. For running costs just apply the
     * same delta each tick.
     * <p/>
     * If the specified delta cannot be completely applied to the buffer, the
     * remaining delta will be returned. This means that for negative values
     * a part of the energy will have been consumed, though.
     * <p/>
     * If there is enough energy or no overflow this will return <tt>0</tt>.
     * <p/>
     * Keep in mind that this change is applied to the <em>global</em> buffer,
     * i.e. energy from multiple buffers may be consumed / multiple buffers may
     * be filled. The buffer for which this method is called (i.e. this node
     * instance) will be prioritized, though.
     *
     * @param delta the amount of energy to consume or store.
     * @return the remainder of the delta that could not be applied.
     */
    double changeBuffer(double delta);

    /**
     * Like {@link #changeBuffer}, but will only store/consume the specified
     * amount of energy if there is enough capacity/energy available.
     *
     * @param delta the amount of energy to consume or store.
     * @return <tt>true</tt> if the energy was successfully consumed or stored.
     */
    boolean tryChangeBuffer(double delta);

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
    void setLocalBufferSize(double size);
}
