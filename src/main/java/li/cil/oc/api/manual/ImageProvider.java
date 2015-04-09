package li.cil.oc.api.manual;

/**
 * This allows implementing custom image providers for rendering custom content
 * in manual pages. Each provider can be registered for one or more prefixes,
 * and will be selected based on the prefix it was registered with. It is
 * expected to return an image renderer, which essentially represents the
 * area it will render to.
 */
public interface ImageProvider {
    /**
     * Gets an image renderer for the specified data.
     * <p/>
     * The data passed here will be part of the image URL following the prefix
     * that the provider was registered with. So for example, if the provider
     * was registered for the prefix <tt>custom</tt>, and the image to be
     * rendered in the Markdown document was <tt>[blah](custom:the data]</tt>,
     * then the string passed where would be <tt>the data</tt>.
     * <p/>
     * If there is no appropriate image renderer (for example, for the built-in
     * item stack renderers: if the item definition is invalid), this should
     * return <tt>null</tt>, it should <em>never</em> throw an exception.
     *
     * @param data the data part of the image definition.
     * @return the image renderer for the data.
     */
    ImageRenderer getImage(String data);
}
