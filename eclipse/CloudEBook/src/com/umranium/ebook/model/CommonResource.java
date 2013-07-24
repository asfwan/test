package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "common_resources")
public class CommonResource extends BaseEntity<CommonResource> implements Serializable {

    private static final long serialVersionUID = 5789012280645988102L;

    public static final String RESOURCE_JQUERY = "jquery";
    public static final String RESOURCE_ANDROID_SELECTION = "androidselection";
    public static final String RESOURCE_DOM_INTEROP = "domInterop";
    public static final String RESOURCE_SYSTEM_STYLES = "systemStyles";
    public static final String RESOURCE_EBOOK_DART = "ebookDart";
    public static final String RESOURCE_DART = "dart";

    public static final String COLUMN_NAME_NAME = "name";

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false, length = DbCommon.MAX_CONTENT_LEN)
    public String content;

    public CommonResource() {
    }

    public CommonResource(String name, String content) {
        this.name = name;
        this.content = content;
    }

}
