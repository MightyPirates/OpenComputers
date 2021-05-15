package li.cil.oc.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

public class OldScaledResolution {
    private final int scaledWidth;
    private final int scaledHeight;

    public OldScaledResolution(Minecraft minecraft, int width, int height) {
        int scaleFactor = 1;
        int guiScale = minecraft.gameSettings.guiScale;

        if (guiScale == 0) {
            guiScale = 1000;
        }

        while (scaleFactor < guiScale && width / (scaleFactor + 1) >= 320 && height / (scaleFactor + 1) >= 240) {
            ++scaleFactor;
        }

        if (minecraft.isUnicode() && scaleFactor % 2 != 0 && scaleFactor != 1) {
            --scaleFactor;
        }

        this.scaledWidth = MathHelper.ceil((double) width / (double) scaleFactor);
        this.scaledHeight = MathHelper.ceil((double) height / (double) scaleFactor);
    }

    public int getScaledWidth() {
        return this.scaledWidth;
    }

    public int getScaledHeight() {
        return this.scaledHeight;
    }
}
