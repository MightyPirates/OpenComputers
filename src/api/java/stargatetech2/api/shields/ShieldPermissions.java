package stargatetech2.api.shields;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;


public class ShieldPermissions {
	// Permission Flags
	public static final int PERM_FRIEND		= 0x01;
	public static final int PERM_PLAYER		= 0x02;
	public static final int PERM_VILLAGER	= 0x04;
	public static final int PERM_ANIMAL		= 0x08;
	public static final int PERM_MONSTER	= 0x10;
	public static final int PERM_VESSEL		= 0x20;
	
	private int permValue = 0;
	private LinkedList<String> playerExceptions = new LinkedList<String>();
	
	/**
	 * @return A default ShieldPermissions object.
	 */
	public static ShieldPermissions getDefault(){
		ShieldPermissions perm = new ShieldPermissions();
		perm.allow(PERM_FRIEND | PERM_PLAYER);
		return perm;
	}
	
	/**
	 * @param perm Make shields allow this PERM_* flag.
	 * Combinations (binary or) are allowed.
	 */
	public void allow(int perm){
		permValue |= perm;
	}
	
	/**
	 * @param perm Make shields disallow this PERM_* flag.
	 * Combinations (binary or) are allowed.
	 */
	public void disallow(int perm){
		permValue &= ~perm;
	}
	
	/**
	 * Add an exception to the current player permission setting.
	 * @param player The name of the player to add to the exceptions.
	 */
	public void setPlayerException(String player){
		if(!playerExceptions.contains(player))
			playerExceptions.add(player);
	}
	
	/**
	 * Remove an exception from the current player permission setting.
	 * @param player The name of the player to remove from the exceptions.
	 */
	public void removePlayerException(String player){
		playerExceptions.remove(player);
	}
	
	/**
	 * @return A list of strings containing the names of all the players
	 * who currently are exceptions to the player permission setting.
	 */
	public List<String> getExceptionList(){
		return playerExceptions;
	}
	
	/**
	 * Check if this entity is allowed to go through the shields.
	 * 
	 * @param entity The entity to be checked.
	 * @param doDismount If set to true, when an allowed entity is being ridden
	 * by a disallowed one, it's rider will be dismounted so this entity can pass.
	 * @return Whether this entity can go through or not.
	 */
	public boolean isEntityAllowed(Entity entity, boolean doDismount){
		boolean allow = false;
		if(entity instanceof EntityPlayer){
			if(hasBit(PERM_PLAYER)){
				allow = true;
			}
			if(playerExceptions.contains(entity.getEntityName())){
				allow = !allow;
			}
		}else if(entity instanceof EntityVillager){
			allow = hasBit(PERM_VILLAGER);
		}else if(entity instanceof EntityAnimal){
			allow = hasBit(PERM_ANIMAL);
		}else if(entity instanceof EntityMob){
			allow = hasBit(PERM_MONSTER);
		}else if(entity instanceof EntityMinecart){
			allow = hasBit(PERM_VESSEL);
		}
		if(allow && entity.riddenByEntity != null && doDismount && entity.worldObj.isRemote == false){
			if(!isEntityAllowed(entity.riddenByEntity, true)){
				Entity rider = entity.riddenByEntity;
				if(rider instanceof EntityPlayer){
					rider.mountEntity(null);
				}else{
					rider.ridingEntity = null;
					rider.prevPosY += 1;
					rider.posY += 1;
					entity.riddenByEntity = null;
				}
			}
		}
		return allow;
	}
	
	/**
	 * @param bit A binary flag to check against these permissions.
	 * While usually this is a PERM_* flag, any combination of bits is allowed.
	 * @return Whether this flag exists in these permissions or not.
	 */
	public boolean hasBit(int bit){
		return (permValue & bit) != 0;
	}
	
	/**
	 * @return A deep clone of this object.
	 */
	public ShieldPermissions deepClone(){
		ShieldPermissions clone = new ShieldPermissions();
		clone.permValue = this.permValue;
		for(String player : playerExceptions){
			clone.playerExceptions.add(player);
		}
		return clone;
	}
	
	// This really doesn't need an explanation...
	public static ShieldPermissions readFromNBT(NBTTagCompound nbt){
		ShieldPermissions permissions = getDefault();
		if(nbt != null){
			int exceptions = nbt.getInteger("exceptions");
			permissions.permValue = nbt.getInteger("permValue");
			permissions.playerExceptions = new LinkedList<String>();
			for(int i = 0; i < exceptions; i++){
				permissions.setPlayerException(nbt.getString("pex" + i));
			}
		}
		return permissions;
	}
	
	// ... does it?
	public NBTTagCompound writeToNBT(){
		NBTTagCompound nbt = new NBTTagCompound();
		int exceptions = playerExceptions.size();
		nbt.setInteger("permValue", permValue);
		nbt.setInteger("exceptions", exceptions);
		for(int i = 0; i < exceptions; i++){
			nbt.setString("pex" + i, playerExceptions.get(i));
		}
		return nbt;
	}
}