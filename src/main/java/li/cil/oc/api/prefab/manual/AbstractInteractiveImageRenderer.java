package li.cil.oc.api.prefab.manual;

import li.cil.oc.api.manual.InteractiveImageRenderer;

/**
 * Simple base implementation of {@link li.cil.oc.api.manual.InteractiveImageRenderer}.
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractInteractiveImageRenderer implements InteractiveImageRenderer {
    @Override
    public String getTooltip(final String tooltip) {
        return tooltip;
    }

    @Override
    public boolean onMouseClick(final int mouseX, final int mouseY) {
        return false;
    }
}
