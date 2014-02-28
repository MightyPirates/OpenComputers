package ic2.api.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.Cancelable;

import net.minecraftforge.event.world.WorldEvent;

/**
 * A bunch of Events to handle the power of the Mining Laser.
 */
@Cancelable
public class LaserEvent extends WorldEvent {
	// the Laser Entity
	public final Entity lasershot;

	// the following variables can be changed and the Laser will adjust to them

	// the Player firing the Laser. If the Laser gets "reflected" the Player could change.
	public EntityLivingBase owner;
	// Range of the Laser Shot. Determine the amount of broken blocks, as well, as each broken block will subtract ~1F from range.
	public float range, power;
	public int blockBreaks;
	// Determines whether the laser will explode upon hitting something
	public boolean explosive, smelt;

	public LaserEvent(World world1, Entity lasershot1, EntityLivingBase owner1, float range1, float power1, int blockBreaks1, boolean explosive1, boolean smelt1) {
		super(world1);
		this.lasershot = lasershot1;
		this.owner = owner1;
		this.range = range1;
		this.power = power1;
		this.blockBreaks = blockBreaks1;
		this.explosive = explosive1;
		this.smelt = smelt1;
	}

	/**
	 * Event when the Laser is getting shot by a Player.
	 * 
	 * The Item is the Laser Gun which the "Player" has shot
	 */
	public static class LaserShootEvent extends LaserEvent {
		ItemStack laseritem;

		public LaserShootEvent(World world1, Entity lasershot1, EntityLivingBase owner1, float range1, float power1, int blockBreaks1, boolean explosive1, boolean smelt1, ItemStack laseritem1) {
			super(world1, lasershot1, owner1, range1, power1, blockBreaks1, explosive1, smelt1);
			this.laseritem = laseritem1;
		}
	}

	/**
	 * Event when the Laser is exploding for some Reason.
	 * 
	 * The Laser will no longer exist after this Event is posted as it either Explodes or despawns after the Event is fired.
	 */
	public static class LaserExplodesEvent extends LaserEvent {
		// explosion strength, even that can be changed!
		public float explosionpower, explosiondroprate, explosionentitydamage;

		public LaserExplodesEvent(World world1, Entity lasershot1, EntityLivingBase owner1, float range1, float power1, int blockBreaks1, boolean explosive1, boolean smelt1, float explosionpower1, float explosiondroprate1, float explosionentitydamage1) {
			super(world1, lasershot1, owner1, range1, power1, blockBreaks1, explosive1, smelt1);
			this.explosionpower = explosionpower1;
			this.explosiondroprate = explosiondroprate1;
			this.explosionentitydamage = explosionentitydamage1;
		}
	}

	/**
	 * Event when the Laser is hitting a Block
	 * x, y and z are the Coords of the Block.
	 * 
	 * Canceling this Event stops the Laser from attempting to break the Block, what is very useful for Glass.
	 * Use lasershot.setDead() to remove the Shot entirely.
	 */
	public static class LaserHitsBlockEvent extends LaserEvent {
		// targeted block, even that can be changed!
		public int x, y, z;
		public int side;
		// removeBlock determines if the Block will be removed. dropBlock determines if the Block should drop something.
		public boolean removeBlock, dropBlock;
		public float dropChance;

		public LaserHitsBlockEvent(World world1, Entity lasershot1, EntityLivingBase owner1, float range1, float power1, int blockBreaks1, boolean explosive1, boolean smelt1, int x1, int y1, int z1, int side1, float dropChance1, boolean removeBlock1, boolean dropBlock1) {
			super(world1, lasershot1, owner1, range1, power1, blockBreaks1, explosive1, smelt1);
			this.x = x1;
			this.y = y1;
			this.z = z1;
			this.side = side1;
			this.removeBlock = removeBlock1;
			this.dropBlock = dropBlock1;
			this.dropChance = dropChance1;
		}
	}

	/**
	 * Event when the Laser is getting at a Living Entity
	 * 
	 * Canceling this Event ignores the Entity
	 * Use lasershot.setDead() to remove the Shot entirely.
	 */
	public static class LaserHitsEntityEvent extends LaserEvent {
		// the Entity which the Laser has shot at, even the target can be changed!
		public Entity hitentity;

		public LaserHitsEntityEvent(World world1, Entity lasershot1, EntityLivingBase owner1, float range1, float power1, int blockBreaks1, boolean explosive1, boolean smelt1, Entity hitentity1) {
			super(world1, lasershot1, owner1, range1, power1, blockBreaks1, explosive1, smelt1);
			this.hitentity = hitentity1;
		}
	}
}
