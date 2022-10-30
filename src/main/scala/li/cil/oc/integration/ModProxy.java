package li.cil.oc.integration;

public interface ModProxy {
    Mod getMod();

    default void preInitialize()
    {
    }

    default void initialize()
    {
    }
}
