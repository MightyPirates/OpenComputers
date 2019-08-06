package li.cil.oc.client.renderer.font;

public interface FontTextureProvider {
    interface Receiver {
        void draw(double x1, double x2, double y1, double y2, double u1, double u2, double v1, double v2);
    }

    int getCharWidth();
    int getCharHeight();

    int getTextureCount();
    void begin(int texture);
    void drawCodePoint(int codePoint, float tx, float ty, Receiver receiver);
    void end(int texture);
}
