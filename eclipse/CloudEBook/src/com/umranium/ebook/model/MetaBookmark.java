package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.j256.ormlite.field.DatabaseField;

@Entity(name = "meta_bookmark")
public class MetaBookmark extends BaseMetaEntity<MetaBookmark> implements Serializable {

    private static final long serialVersionUID = 2071541831504892034L;

    public static final String COLUMN_NAME_BOOK_SEGMENT = "bookSegment_id";
    public static final String COLUMN_NAME_START_LOC = "startLoc";
    public static final String COLUMN_NAME_START_TEXT_LOC = "startTextLoc";

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected UserBookSegmentDetails bookSegment;

    @Column(nullable = false, length = DbCommon.MAX_SUMMARY_LEN)
    protected String summary;

    @Column(nullable = false, length = 1024)
    public String pointJson;

    public MetaBookmark() {
    }

    public MetaBookmark(UserBookSegmentDetails bookSegment, String identifier, String summary, String startLoc, Integer startTextLoc) {
        super(identifier, startLoc, startTextLoc);
        this.bookSegment = bookSegment;
        this.summary = DbCommon.summarize(summary);
    }

    public UserBookSegmentDetails getBookSegment() {
        return bookSegment;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "{id=" + getId() +
                ", bookSegment=" + bookSegment.getId() +
                ", identifier=" + identifier +
                ", summary=" + summary +
                ", pointJson=" + pointJson +
                ", startLoc=" + startLoc +
                "}";
    }

}
