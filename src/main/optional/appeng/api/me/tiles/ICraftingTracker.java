package appeng.api.me.tiles;

import appeng.api.me.util.ICraftingPattern;
import appeng.api.me.util.ITileCraftingProvider;

public interface ICraftingTracker {

    void addCraftingOption( ITileCraftingProvider provider, ICraftingPattern api );
    
}
