package com.umranium.ebook.sharedres;

import static com.umranium.ebookextra.Constants.TAG;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import android.content.res.AssetManager;
import android.util.Log;

import com.umranium.ebook.epub.DomHelper;
import com.umranium.ebook.model.CommonResource;

public class AssetsSharedResources extends SharedResources {

    static final Charset UTF8 = Charset.forName("UTF-8");

    private AssetManager assetManager;
    private boolean cacheValues;
    private static final Map<String, String> resources = new HashMap<String, String>(10);

    public AssetsSharedResources(AssetManager assetManager, boolean cacheValues) {
        this.assetManager = assetManager;
        this.cacheValues = cacheValues;
    }

    private static String getAssetOf(String name) {
        if (name.equals(CommonResource.RESOURCE_ANDROID_SELECTION)) {
            return "android.selection.js";
        }
        if (name.equals(CommonResource.RESOURCE_DART)) {
            return "dart.js";
        }
        if (name.equals(CommonResource.RESOURCE_DOM_INTEROP)) {
            return "dom_interop_js.js";
        }
        if (name.equals(CommonResource.RESOURCE_EBOOK_DART)) {
            return "ebook.dart.js";
        }
        if (name.equals(CommonResource.RESOURCE_JQUERY)) {
            return "jquery-2.0.0.min.js";
        }
        if (name.equals(CommonResource.RESOURCE_SYSTEM_STYLES)) {
            return "system_styles.css";
        }
        throw new RuntimeException("Unsupported resource: " + name);
    }

    private String loadContent(String name) {
        try {
            String content = DomHelper.streamToString(
                    assetManager.open(getAssetOf(name)),
                    UTF8);
            if (name.equals(CommonResource.RESOURCE_DART) ||
                    name.equals(CommonResource.RESOURCE_EBOOK_DART)) {
                content = quoteEndScript(content);
            }
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getResource(String name) {
        if (cacheValues) {
            if (!resources.containsKey(name)) {
                synchronized (resources) {
                    if (!resources.containsKey(name)) {
                        Log.d(TAG, "\tLoading shared resource '" + name + "' from asset");
                        String content = loadContent(name);
                        resources.put(name, content);
                    }
                }
            }
            return resources.get(name);
        } else {
            return loadContent(name);
        }
    }

}