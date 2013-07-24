package com.umranium.ebook.sharedres;

import com.umranium.ebook.model.CommonResource;

public abstract class SharedResources {

    public abstract String getResource(String name);

    public String getJquery() {
        return getResource(CommonResource.RESOURCE_JQUERY);
    }

    public String getAndroidSelection() {
        return getResource(CommonResource.RESOURCE_ANDROID_SELECTION);
    }

    public String getDomInterop() {
        return getResource(CommonResource.RESOURCE_DOM_INTEROP);
    }

    public String getSystemStyles() {
        return getResource(CommonResource.RESOURCE_SYSTEM_STYLES);
    }

    public String getEbookDart() {
        return getResource(CommonResource.RESOURCE_EBOOK_DART);
    }

    public String getDart() {
        return getResource(CommonResource.RESOURCE_DART);
    }

    protected static String quoteEndScript(String s) {
        return s.replaceAll("</script", "<\\\\/script");
    }

}