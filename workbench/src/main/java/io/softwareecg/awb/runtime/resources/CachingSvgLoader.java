package io.softwareecg.awb.runtime.resources;

import afester.javafx.svg.SvgLoader;
import javafx.scene.Group;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Helper that loads SVG files into an JFX object representation and
 * cache the loaded result.
 *
 * @since 22.08.2018
 */
public class CachingSvgLoader {

    private final HashMap<String, SVGOMDocument> svgDataCache = new HashMap<>();

    private final boolean useCache;

    private final Method handleMethod;

    private final Field parentNodeField;

    private double xScale = 1.0;

    private double yScale = 1.0;

    //=================================================================================================================
    // Public API of the CachingSvgLoader
    //=================================================================================================================

    /**
     * Creates a new instance of CachingSvgLoader
     * the active cache as default
     */
    public CachingSvgLoader() {
        this(true);
    }

    /**
     * Creates a new instance of CachingSvgLoader
     * with caller defined cache state (active or not)
     *
     * @param useCache a boolean flag that controls if the cache is active or not
     */
    public CachingSvgLoader(boolean useCache) {
        this.useCache = useCache;

        handleMethod = MethodUtils.getMatchingMethod(SvgLoader.class, "handle", Node.class);
        handleMethod.setAccessible(true);
        parentNodeField = FieldUtils.getField(SvgLoader.class, "parentNode", true);
    }

    /**
     * Loads the SVG from the specified resource path and return it in a JFX object representation wrapped
     * as Group instance.
     * If cache flag is active (true) the algorithm tries to resolve the instance from the cache
     * instead creating a new one. Caution: an JFX object instance can only use once in the UI. Use
     * it multiple time with different parents wont work.
     *
     * @param absoluteSvgResourcePath the absolute path to the SVG file that must be reachable from the caller of this method
     * @return an JFX object representation of the SVG graphic
     */
    public Group getSvgImage(String absoluteSvgResourcePath) {

        if (!useCache) {
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            Group group = new SvgLoader().loadSvg(caller.getResourceAsStream(absoluteSvgResourcePath));
            group.setScaleX(xScale);
            group.setScaleY(yScale);

            return new Group(group);
        }

        SVGOMDocument svgData = svgDataCache.get(absoluteSvgResourcePath);

        SvgLoader loader = new SvgLoader();

        if (svgData == null) {
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            InputStream svgInputStream = caller.getResourceAsStream(absoluteSvgResourcePath);
            svgData = loader.loadSvgDocument(svgInputStream);
            svgDataCache.put(absoluteSvgResourcePath, svgData);
        }

        if (svgData == null) {
            throw new IllegalStateException("Unable to resolve the icon SVG data specified with: " + absoluteSvgResourcePath);
        }

        try {
            FieldUtils.writeField(parentNodeField, loader, new Group());
            handleMethod.invoke(loader, svgData);
            Group group =  (Group) FieldUtils.readField(parentNodeField, loader);
            group.setScaleX(xScale);
            group.setScaleY(yScale);

            return new Group(group);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sets the scale factor of the graphic (default is 1.0) and return
     * the CachingSvgLoader instance to use this setter method in a fluent way.
     *
     * @param scale the scale factor that controls which size the (vector) graphic will have as JFX node representation
     * @return this CachingSvgLoader instance with new internal configured scale factor.
     */
    public CachingSvgLoader setDefaultScale(double scale) {

        this.xScale = scale;
        this.yScale = scale;

        return this;
    }
}
