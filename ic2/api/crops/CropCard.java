package ic2.api.crops;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base agriculture crop.
 * 
 * Any crop extending this can be registered using registerCrop to be added into the game.
 */
public abstract class CropCard
{
	/**
	 * Plant name. Will be displayed to the player.
	 * 
	 * @return Plant name
	 */
	public abstract String name();

	/**
	 * Your name here, will be shown in "Discovered by:" when analyzing seeds.
	 * 
	 * @return Your name
	 */
	public String discoveredBy() {return "Alblaka";}

	/**
	 * Description of your plant. Keep it short, a few characters per line for up to two lines.
	 * Default is showing attributes of your plant, 2 per line.
	 * 
	 * @param i line to get, starting from 0
	 * @return The line
	 */
	public String desc(int i)
	{
		String[] att = attributes();
		if (att == null || att.length==0) return "";
		if (i == 0)
		{
			String s = att[0];
			if (att.length >= 2)
			{
				s+=", "+att[1];
				if (att.length >= 3) s+=",";
			}
			return s;
		}
		else
		{
			if (att.length < 3) return "";
			String s = att[2];
			if (att.length >= 4) s+=", "+att[3];
			return s;
		}
	}

	/**
	 * Tier of the plant. Ranges from 1 to 16, 0 is Weed.
	 * Valuable and powerful crops have higher tiers, useless and weak ones have lower tiers.
	 * 
	 * @return Tier
	 */
	public abstract int tier();

	/**
	 * Describe the plant through a set of stats, influencing breeding.
	 * Plants sharing stats and attributes will tend to cross-breed more often.
	 * 
	 * Stats:
	 * - 0: Chemistry (Industrial uses based on chemical plant components)
	 * - 1: Consumable (Food, potion ingredients, stuff meant to be eaten or similarly used)
	 * - 2: Defensive (Plants with defense capabilities (damaging, explosive, chemical) or special abilities in general)
	 * - 3: Colorful (How colorful/aesthetically/beautiful is the plant, like dye-plants or plants without actual effects)
	 * - 4: Weed (Is this plant weed-like and rather unwanted/quick-spreading? Rare super-breed plants should have low values here)
	 * 
	 * @param n index of the requested stat
	 * @return The requested value of the stats
	 */
	public abstract int stat(int n);

	/**
	 * Additional attributes of the plant, also influencing breeding.
	 * Plants sharing stats and attributes will tend to cross-breed more often.
	 * 
	 * @return Attributes as an array of strings
	 */
	public abstract String[] attributes();

	/**
	 * Determine the max crop size.
	 * 
	 * Currently only used for texture allocation.
	 */
	public abstract int maxSize();

	/**
	 * Instantiate your Icons here.
	 * 
	 * This method will get called by IC2, don't call it yourself.
	 */
	@SideOnly(Side.CLIENT)
	public void registerSprites(IconRegister iconRegister) {
		textures = new Icon[maxSize()];

		for (int i = 1; i <= textures.length; i++) {
			textures[i-1] = iconRegister.registerIcon("ic2:crop/blockCrop."+name()+"."+i);
		}
	}

	/**
	 * Sprite the crop is meant to be rendered with.
	 * 
	 * @param crop reference to ICropTile
	 * @return 0-255, representing the sprite index on the crop's spritesheet.
	 */
	@SideOnly(Side.CLIENT)
	public Icon getSprite(ICropTile crop) {
		if (crop.getSize() <= 0 || crop.getSize() > textures.length) return null;

		return textures[crop.getSize() - 1];
	}

	/**
	 * Get the crop's spritesheet.
	 * Per default crops_0.png of ic2-sprites
	 * @return Texture file path
	 */
	public String getTextureFile() {
		return "/ic2/sprites/crops_0.png";
	}

	/**
	 * Amount of growth points needed to increase the plant's size.
	 * Default is 200 * tier.
	 */
	public int growthDuration(ICropTile crop)
	{
		return tier()*200;
	}

	/**
	 * Check whether the plant can grow further.
	 * 
	 * Consider:
	 * - Humidity, nutrients and air quality
	 * - Current size
	 * - Light level
	 * - Special biomes or conditions, accessible through crop.worldObj
	 * 
	 * This method will be called upon empty upgraded crops to check whether a neighboring plant can cross onto it! Don't check if the size is greater than 0 and if the ID is real.
	 * 
	 * @param crop reference to ICropTile
	 * @return Whether the crop can grow
	 */
	public abstract boolean canGrow(ICropTile crop);

	/**
	 * Calculate the influence for the plant to grow based on humidity, nutrients and air.
	 * Normal behavior is rating the three stats "normal", with each of them ranging from 0-30.
	 * Basic rule: Assume everything returns 10. All together must equal 30. Add the factors to your likings, for example (humidity*0.7)+(nutrients*0.9)+(air*1.4)
	 * 
	 * Default is humidity + nutrients + air (no factors).
	 * 
	 * @param crop reference to ICropTile
	 * @param humidity ground humidity, influenced by hydration
	 * @param nutrients nutrient quality in ground, based on fertilizers
	 * @param air air quality, influences by open gardens and less crops surrounding this one
	 * @return 0-30
	 */
	public int weightInfluences(ICropTile crop, float humidity, float nutrients, float air)
	{
		return (int) (humidity+nutrients+air);
	}

	/**
	 * Used to determine whether the plant can crossbreed with another crop.
	 * Default is allow crossbreeding if the size is greater or equal than 3.
	 * 
	 * @param crop crop to crossbreed with
	 */
	public boolean canCross(ICropTile crop)
	{
		return crop.getSize() >= 3;
	}


	/**
	 * Called when the plant is rightclicked by a player.
	 * Default action is harvesting.
	 * 
	 * @param crop reference to ICropTile
	 * @param player player rightclicking the crop
	 * @return Whether the plant has changed
	 */
	public boolean rightclick(ICropTile crop, EntityPlayer player)
	{
		return crop.harvest(true);
	}

	/**
	 * Check whether the crop can be harvested.
	 * 
	 * @param crop reference to ICropTile
	 * @return Whether the crop can be harvested in its current state.
	 */
	public abstract boolean canBeHarvested(ICropTile crop);

	/**
	 * Base chance for dropping the plant's gains, specify values greater than 1 for multiple drops.
	 * Default is 0.95^tier.
	 * 
	 * @return Chance to drop the gains
	 */
	public float dropGainChance()
	{
		float base = 1F;
		for (int i = 0; i < tier(); i++) {base*=0.95;}
		return base;
	}

	/**
	 * Item obtained from harvesting the plant.
	 * 
	 * @param crop reference to ICropTile
	 * @return Item obtained
	 */
	public abstract ItemStack getGain(ICropTile crop);

	/**
	 * Get the size of the plant after harvesting.
	 * Default is 1.
	 * 
	 * @param crop reference to ICropTile
	 * @return Plant size after harvesting
	 */
	public byte getSizeAfterHarvest(ICropTile crop) {return 1;}


	/**
	 * Called when the plant is leftclicked by a player.
	 * Default action is picking the plant.
	 * 
	 * @param crop reference to ICropTile
	 * @param player player leftclicked the crop
	 * @return Whether the plant has changed
	 */
	public boolean leftclick(ICropTile crop, EntityPlayer player)
	{
		return crop.pick(true);
	}

	/**
	 * Base chance for dropping seeds when the plant is picked.
	 * Default is 0.5*0.8^tier with a bigger chance for sizes greater than 2 and absolutely no chance for size 0.
	 * 
	 * @param crop reference to ICropTile
	 * @return Chance to drop the seeds
	 */
	public float dropSeedChance(ICropTile crop)
	{
		if (crop.getSize() == 1) return 0;
		float base = 0.5F;
		if (crop.getSize() == 2) base/=2F;
		for (int i = 0; i < tier(); i++) {base*=0.8;}
		return base;
	}

	/**
	 * Obtain seeds dropped when the plant is picked.
	 * Multiple drops of the returned ItemStack can occur.
	 * Default action is generating a seed from this crop.
	 * 
	 * @param crop reference to ICropTile
	 * @return Seeds
	 */
	public ItemStack getSeeds(ICropTile crop)
	{
		return crop.generateSeeds(crop.getID(), crop.getGrowth(), crop.getGain(), crop.getResistance(), crop.getScanLevel());
	}

	/**
	 * Called when a neighbor block to the crop has changed.
	 * 
	 * @param crop reference to ICropTile
	 */
	public void onNeighbourChange(ICropTile crop){}

	/**
	 * Check if the crop should emit redstone.
	 * 
	 * @return Whether the crop should emit redstone
	 */
	public int emitRedstone(ICropTile crop){return 0;}

	/**
	 * Called when the crop is destroyed.
	 * 
	 * @param crop reference to ICropTile
	 */
	public void onBlockDestroyed(ICropTile crop){}

	/**
	 * Get the light value emitted by the plant.
	 * 
	 * @param crop reference to ICropTile
	 * @return Light value emitted
	 */
	public int getEmittedLight(ICropTile crop) {return 0;}

	/**
	 * Default is true if the entity is an EntityLiving in jumping or sprinting state.
	 * 
	 * @param crop reference to ICropTile
	 * @param entity entity colliding
	 * @return Whether trampling calculation should happen, return false if the plant is no longer valid.
	 */
	public boolean onEntityCollision(ICropTile crop, Entity entity)
	{
		if (entity instanceof EntityLivingBase)
		{
			return ((EntityLivingBase)entity).isSprinting();
		}
		return false;
	}


	/**
	 * Called every time the crop ticks.
	 * Should be called every 256 ticks or around 13 seconds.
	 * 
	 * @param crop reference to ICropTile
	 */
	public void tick(ICropTile crop) {}

	/**
	 * Check whether this plant spreads weed to surrounding tiles.
	 * Default is true if the plant has a high growth stat (or is weeds) and size greater or equal than 2.
	 * 
	 * @param crop reference to ICropTile
	 * @return Whether the plant spreads weed
	 */
	public boolean isWeed(ICropTile crop)
	{
		return crop.getSize()>=2 && (crop.getID()==0 || crop.getGrowth()>=24);
	}


	/**
	 * Get this plant's ID.
	 * 
	 * @return ID of this CropCard or -1 if it's not registered
	 */
	public final int getId()
	{
		return Crops.instance.getIdFor(this);
	}

	@SideOnly(Side.CLIENT)
	protected Icon textures[];
}
