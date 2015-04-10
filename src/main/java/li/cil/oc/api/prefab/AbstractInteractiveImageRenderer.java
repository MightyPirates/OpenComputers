package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.InteractiveImageRenderer;

/**
 * Simple base implementation of {@link li.cil.oc.api.manual.InteractiveImageRenderer}.
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractInteractiveImageRenderer implements InteractiveImageRenderer {
    @Override
    public String getTooltip(String tooltip) {
        return tooltip;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        return false;
    }
}
