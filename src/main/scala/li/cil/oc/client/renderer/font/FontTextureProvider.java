package li.cil.oc.client.renderer.font;

public interface FontTextureProvider {
    interface Receiver {
        void draw(float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2);
    }

    int getCharWidth();
    int getCharHeight();

    boolean isDynamic();
    int getTextureCount();
    void begin(int texture);
    void loadCodePoint(int codePoint);
    void drawCodePoint(int codePoint, float tx, float ty, Receiver receiver);
    void end(int texture);
}
