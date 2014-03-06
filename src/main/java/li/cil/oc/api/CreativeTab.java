package li.cil.oc.api;

import net.minecraft.creativetab.CreativeTabs;

/**
 * Allows access to the creative tab used by OpenComputers.
 */
public final class CreativeTab {
    /**
     * The creative tab used by OpenComputers.
     * <p/>
     * Changed to the actual tab if OC is present.
     */
    public static CreativeTabs Instance = CreativeTabs.tabRedstone;

    private CreativeTab() {
    }
}
