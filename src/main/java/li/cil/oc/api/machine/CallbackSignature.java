package li.cil.oc.api.machine;

@FunctionalInterface
public interface CallbackSignature {
    Object[] invoke(final Context context, final Arguments arguments);
}
