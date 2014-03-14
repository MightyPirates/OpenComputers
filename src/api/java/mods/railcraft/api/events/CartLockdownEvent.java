/*
 * Copyright (c) CovertJaguar, 2011 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.events;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraftforge.event.Event;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class CartLockdownEvent extends Event {

    public final EntityMinecart cart;
    public final int x;
    public final int y;
    public final int z;

    private CartLockdownEvent(EntityMinecart cart, int x, int y, int z) {
        this.cart = cart;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * This event is posted every tick that a LockType Track (Lockdown, Holding,
     * Boarding) is holding onto a minecart.
     */
    public static class Lock extends CartLockdownEvent {

        public Lock(EntityMinecart cart, int x, int y, int z) {
            super(cart, x, y, z);
        }
    }

    /**
     * This event is posted every tick that a LockType Track (Lockdown, Holding,
     * Boarding) is releasing a minecart.
     */
    public static class Release extends CartLockdownEvent {

        public Release(EntityMinecart cart, int x, int y, int z) {
            super(cart, x, y, z);
        }
    }
}
