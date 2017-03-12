package li.cil.oc.api.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * These packets represent messages sent using a network card or wireless
 * network card, and can be relayed by the switch and access point blocks.
 * <p/>
 * These will be sent as the payload of <tt>network.message</tt> messages.
 * <p/>
 * <em>Important</em>: do <em>not</em> implement this interface. Use the factory
 * methods in {@link li.cil.oc.api.Network} instead.
 */
public interface Packet extends INBTSerializable<NBTTagCompound> {
    /**
     * The address of the <em>original</em> sender of this packet.
     */
    String getSource();

    /**
     * The address of the destination of the packet. This is <tt>null</tt> for
     * broadcast packets.
     */
    String getDestination();

    /**
     * The port this packet is being sent to.
     */
    int getPort();

    /**
     * The payload of the packet. This will usually only contain simple types,
     * to allow persisting the packet.
     */
    Object[] getData();

    /**
     * The size of the packet's payload.
     * <p/>
     * This is computed based on the types in the data array, but is only defined
     * for primitive types, i.e. null, boolean, integer, boolean byte array and
     * string. All other types do <em>not</em> contribute to the packet's size.
     */
    int getSize();

    /**
     * Generates a copy of the packet, with a reduced time to live.
     * <p/>
     * This is called by switches and access points to generate relayed packets.
     *
     * @return a copy of this packet with a reduced TTL.
     */
    @Nullable
    Packet getHop(final Node via);
}
