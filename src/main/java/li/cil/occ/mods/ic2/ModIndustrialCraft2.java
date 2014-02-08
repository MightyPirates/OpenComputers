package li.cil.occ.mods.ic2;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModIndustrialCraft2 implements IMod {
    @Override
    public String getModId() {
        return "IC2";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverReactor());
        Driver.add(new DriverReactorChamber());
        Driver.add(new DriverEnergyConductor());
        Driver.add(new DriverEnergySink());
        Driver.add(new DriverEnergySource());
        Driver.add(new DriverMassFab());
        Driver.add(new DriverEnergyStorage());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
        final Item item = stack.getItem();
        if (item instanceof IElectricItem) {
            final IElectricItem electricItem = (IElectricItem) item;
            map.put("canProvideEnergy", electricItem.canProvideEnergy(stack));
            map.put("charge", ElectricItem.manager.getCharge(stack));
            map.put("maxCharge", electricItem.getMaxCharge(stack));
            map.put("tier", electricItem.getTier(stack));
            map.put("transferLimit", electricItem.getTransferLimit(stack));
        }
    }
}
