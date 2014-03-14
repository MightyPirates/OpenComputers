package forestry.api.mail;

import net.minecraft.nbt.NBTTagCompound;
import forestry.api.core.INBTTagable;

public class MailAddress implements INBTTagable {
	private String type;
	private String identifier;

	private MailAddress() {
	}

	public MailAddress(String identifier) {
		this(identifier, "player");
	}

	public MailAddress(String identifier, String type) {
		this.identifier = identifier;
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getIdentifier() {
		return identifier;
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
		identifier = nbttagcompound.getString("ID");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("TP", type);
		nbttagcompound.setString("ID", identifier);
	}

	public static MailAddress loadFromNBT(NBTTagCompound nbttagcompound) {
		MailAddress address = new MailAddress();
		address.readFromNBT(nbttagcompound);
		return address;
	}
}
