package li.cil.oc.api.manual;

/**
 * Created by fnuecke on 4/9/2015.
 */
public interface ImageRenderer {
    int getWidth();

    int getHeight();

    void render(int maxWidth);
}
