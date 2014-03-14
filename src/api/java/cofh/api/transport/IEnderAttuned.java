package cofh.api.transport;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IEnderAttuned {

	public enum EnderTypes {
		ITEM, FLUID, REDSTONE_FLUX
	}

	String getOwnerString();

	int getFrequency();

	public boolean setFrequency(int frequency);

	public boolean clearFrequency();

	boolean canSendItems();

	boolean canSendFluid();

	boolean canSendEnergy();

	boolean canReceiveItems();

	boolean canReceiveFluid();

	boolean canReceiveEnergy();

	boolean currentlyValidToReceiveItems(IEnderAttuned asker);

	boolean currentlyValidToReceiveFluid(IEnderAttuned asker);

	boolean currentlyValidToReceiveEnergy(IEnderAttuned asker);

	boolean currentlyValidToSendItems(IEnderAttuned asker);

	boolean currentlyValidToSendFluid(IEnderAttuned asker);

	boolean currentlyValidToSendEnergy(IEnderAttuned asker);

	ItemStack receiveItem(ItemStack item);

	FluidStack receiveFluid(FluidStack fluid, boolean doFill);

	int receiveEnergy(int energy, boolean simulate);

}
