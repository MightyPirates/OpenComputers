package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentAware;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;

public final class DriverSign extends DriverTileEntity implements EnvironmentAware {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntitySign.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntitySign) world.getTileEntity(x, y, z));
    }

    @Override
    public Class<? extends li.cil.oc.api.network.Environment> providedEnvironment(ItemStack stack) {
        if (stack != null && stack.getItem() == Items.sign)
            return Environment.class;
        return null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntitySign> implements NamedBlock {
        public Environment(final TileEntitySign tileEntity) {
            super(tileEntity, "sign");
        }

        @Override
        public String preferredName() {
            return "sign";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the text currently being displayed on the sign, as a multi-line string.")
        public Object[] getValue(final Context context, final Arguments args) {
            return new Object[]{getSignText()};
        }

        @Callback(doc = "function(value:string):string -- Set the multi-line text the sign should display. This is clamped as necessary.")
        public Object[] setValue(final Context context, final Arguments args) {
            final String value = args.checkString(0);
            final String[] lines = value.split("\n");
            for (int i = 0; i < 4; ++i) {
                if (lines.length <= i || lines[i] == null || lines[i].isEmpty()) {
                    tileEntity.signText[i] = "";
                } else if (lines[i].length() > 15) {
                    tileEntity.signText[i] = lines[i].substring(0, 15);
                } else {
                    tileEntity.signText[i] = lines[i];
                }
            }
            tileEntity.getWorldObj().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            return new Object[]{getSignText()};
        }

        private String getSignText() {
            final StringBuilder value = new StringBuilder();
            boolean first = true;
            for (String line : tileEntity.signText) {
                if (first) {
                    first = false;
                } else {
                    value.append("\n'");
                }
                value.append(line);
            }
            return value.toString();
        }
    }
}
