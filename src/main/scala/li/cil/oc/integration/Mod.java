package li.cil.oc.integration;

public interface Mod {
    String id();

    boolean isModAvailable();

    boolean providesPower();
}
