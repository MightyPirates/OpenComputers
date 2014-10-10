package li.cil.oc.integration;

public interface Mod {
    String id();

    boolean isAvailable();

    boolean providesPower();
}
