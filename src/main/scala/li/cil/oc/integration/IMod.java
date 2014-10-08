package li.cil.oc.integration;

import li.cil.oc.util.mods.Mods;

public interface IMod {
    Mods.Mod getMod();

    void initialize();
}
