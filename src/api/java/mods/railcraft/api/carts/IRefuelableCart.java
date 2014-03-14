/*
 * Copyright (c) CovertJaguar, 2011 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.carts;

/**
 * This is used for the "NeedsRefuel" routing conditional.
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public interface IRefuelableCart {

    boolean needsRefuel();

}
