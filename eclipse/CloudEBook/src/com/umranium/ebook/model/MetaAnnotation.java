package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "meta_annotation")
public class MetaAnnotation extends BaseMetaEntity<MetaAnnotation> implements Serializable {

    private static final long serialVersionUID = -8839440146869422165L;

    public static final String COLUMN_NAME_USER_BOOK_SEGMENT_DETAILS = "bookSegment_id";
    public static final String COLUMN_NAME_START_LOC = "startLoc";
    public static final String COLUMN_NAME_START_TEXT_LOC = "startTextLoc";

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected UserBookSegmentDetails bookSegment;

    @Column(nullable = false, length = DbCommon.MAX_SUMMARY_LEN)
    protected String summary;

    @Column(nullable = true)
    public String markColor;

    @Column(nullable = true, length = DbCommon.MAX_NOTE_LEN)
    public String note;

    public MetaAnnotation() {
    }

    public MetaAnnotation(UserBookSegmentDetails bookSegment, String identifier, String summary) {
        super(identifier, BaseMetaEntity.DEF_START_LOC, null);
        this.bookSegment = bookSegment;
        this.summary = DbCommon.summarize(summary);
    }

    public UserBookSegmentDetails getBookSegment() {
        return bookSegment;
    }

    public String getSummary() {
        return summary;
    }


    public void setStartLoc(String startLoc) {
        this.startLoc = startLoc;
    }

    public void setStartTextLoc(Integer startTextLoc) {
        this.startTextLoc = startTextLoc;
    }

    @ForeignCollectionField(eager = false, foreignFieldName = "annotation", orderColumnName = "id")
    public ForeignCollection<MetaMark> marks;

}
