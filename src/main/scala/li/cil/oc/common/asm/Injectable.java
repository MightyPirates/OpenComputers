package li.cil.oc.common.asm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This interface is kind of the opposite to FML's Optional annotations.
 * <br>
 * Instead of stripping interfaces if they are not present, it will inject them
 * when they <em>are</em> present. This helps with some strange cases where
 * stripping does not work as it should.
 */
public final class Injectable {
    /**
     * Not constructable.
     */
    private Injectable() {
    }

    /**
     * Mark a list of interfaces as injectable.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface InterfaceList {
        public Interface[] value();
    }

    /**
     * Used to inject optional interfaces.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Interface {
        /**
         * The fully qualified name of the interface to inject.
         */
        public String value();

        /**
         * The modid that is required to be present for the injecting to occur.
         * <br>
         * Note that injection will not occur if the interface is not fully
         * implemented.
         */
        public String modid();
    }
}
