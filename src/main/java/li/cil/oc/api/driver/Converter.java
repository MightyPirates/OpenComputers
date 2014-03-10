package li.cil.oc.api.driver;

import java.util.Map;

/**
 * A converter is a callback that can be used to transparently convert Java
 * types to something that can be pushed to a machine's architecture.
 * <p/>
 * Note that converters operating on the same object type may override each
 * other when using the same keys in the resulting <tt>Map</tt>. The order in
 * which converters are called depends on the order they were registered in.
 */
public interface Converter {
    /**
     * Converts a type to a Map that only contains valid values, i.e. values
     * that can be directly pushed to an architecture, without further
     * conversion steps.
     * <p/>
     * This is primarily enforced to avoid cycles in conversion steps. If the
     * returned map contains any unsupported values, they will not be retained.
     * <p/>
     * The conversion result should be placed into the the passed map, i.e. the
     * map will represent the original object. For example, if the value had a
     * field <tt>name</tt>, add a key <tt>name</tt> to the map with the value
     * of that field.
     *
     * @param value  the value to convert.
     * @param output the map conversion results are accumulated into.
     */
    void convert(Object value, Map<Object, Object> output);
}
