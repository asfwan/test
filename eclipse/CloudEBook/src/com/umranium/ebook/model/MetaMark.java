package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.j256.ormlite.field.DatabaseField;

@SuppressWarnings("unused")
@Entity(name = "meta_mark")
public class MetaMark extends BaseMetaEntity<MetaMark> implements Serializable {

    private static final long serialVersionUID = 3687495652781486251L;

    public static final String COLUMN_NAME_ANNOTATION = "annotation_id";
    public static final String COLUMN_NAME_START_LOC = "startLoc";
    public static final String COLUMN_NAME_START_TEXT_LOC = "startTextLoc";

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected MetaAnnotation annotation;

    @Column(nullable = false, length = 1024)
    protected String json;

    public MetaMark() {
    }

    public MetaMark(MetaAnnotation annotation, String identifier, String json, String startLoc, Integer startTextLoc) {
        super(identifier, startLoc, startTextLoc);
        this.annotation = annotation;
        this.json = json;
    }

    public MetaAnnotation getAnnotation() {
        return annotation;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "{id=" + getId() +
                ", annotation=" + annotation.getId() +
                ", identifier=" + identifier +
                ", json=" + json +
                ", startLoc=" + startLoc +
                "}";
    }

}
