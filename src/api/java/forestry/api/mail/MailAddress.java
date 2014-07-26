/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.mail;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;

import com.mojang.authlib.GameProfile;

import forestry.api.core.INBTTagable;

public class MailAddress implements INBTTagable {
	private String type;
	private GameProfile profile;

	private MailAddress() {
	}

	public MailAddress(GameProfile profile) {
		this(profile, "player");
	}

	public MailAddress(GameProfile profile, String type) {
		if (profile == null) throw new NullPointerException("profile can't be null.");

		this.profile = profile;
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public GameProfile getProfile() {
		return profile;
	}

	public boolean isPlayer() {
		return "player".equals(type);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		if(nbttagcompound.hasKey("TP"))
			type = nbttagcompound.getString("TP");
		else
			type = nbttagcompound.getShort("TYP") == 0 ? "player" : "trader";

		if (nbttagcompound.hasKey("profile")) {
			profile = NBTUtil.func_152459_a(nbttagcompound.getCompoundTag("profile"));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("TP", type);

		NBTTagCompound profileNbt = new NBTTagCompound();
		NBTUtil.func_152460_a(profileNbt, profile);
		nbttagcompound.setTag("profile", profileNbt);
	}

	public static MailAddress loadFromNBT(NBTTagCompound nbttagcompound) {
		MailAddress address = new MailAddress();
		address.readFromNBT(nbttagcompound);
		return address;
	}
}
