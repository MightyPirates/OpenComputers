package li.cil.oc.api.network;

/**
 * This is an extended version of the {@link SimpleComponent}. Use it to take
 * control over your components' visibility.
 * <p/>
 * Originally all <tt>SimpleComponent</tt>s were network visible. This lead to
 * problems when the interface was implemented at 'top-level' in the class
 * hierarchy, however. For example, many mods have a 'base' tile entity
 * implementation that adds some custom stuff. If implemented in those, all
 * tile entities in the mod would become components, quickly leading to a
 * component overflow (causing computers not to start anymore). This is
 * particularly problematic if the mod contains cable-like parts, such as
 * the shafts in RotaryCraft. To avoid breaking mods too badly this is a
 * separate interface instead of a change to <tt>SimpleComponent</tt>.
 */
public interface SimpleComponentWithVisibility extends SimpleComponent {
    /**
     * The 'visibility' of the component. This determines how the computer
     * has to be connected to the component for it to be usable by it.
     * <p/>
     * You will want to use either <tt>Visibility.Neighbors</tt>, in which
     * case the computer has to be placed directly next to the component,
     * or an Adapter has to be placed next to it, or <tt>Visibility.Network</tt>,
     * in which case any computer, even indirectly connected to it (e.g. via
     * Cable or other OC component blocks), can interact with it.
     * <p/>
     * Note: placing an Adapter block next to a simple component block with
     * neighbor visibility will actually change the component's visibility
     * to network. Breaking all Adapter blocks adjacent to it will reset the
     * visibility to neighbors only.
     * <p/>
     * <em>Important</em>: do <em>not</em> choose <tt>Visibility.Network</tt>
     * here if you implement the interface in some base class tile entity.
     * Only use network visibility for select tile entities. Computers will
     * not boot if too many components are connected to them, meaning that
     * network visibility can make your components unusable if used unwisely!
     *
     * @return the component's visibility.
     */
    Visibility getComponentVisibility();
}
