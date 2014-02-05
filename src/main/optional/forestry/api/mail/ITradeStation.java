package forestry.api.mail;

import net.minecraft.inventory.IInventory;

public interface ITradeStation extends ILetterHandler, IInventory {

	String getMoniker();

	boolean isValid();

	void invalidate();

	void setVirtual(boolean isVirtual);

	boolean isVirtual();

	TradeStationInfo getTradeInfo();

}
