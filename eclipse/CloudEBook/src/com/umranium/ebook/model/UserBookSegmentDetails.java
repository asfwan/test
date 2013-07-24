package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "user_book_segment_details")
public class UserBookSegmentDetails extends BaseEntity<UserBookSegmentDetails> implements Serializable {

    private static final long serialVersionUID = -2153903156080607074L;

    public static final String COLUMN_NAME_USER_BOOK_DETAILS = "userBookDetails_id";
    public static final String COLUMN_NAME_BOOK_SEGMENT = "bookSegment_id";

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected UserBookDetails userBookDetails;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected BookSegment bookSegment;

    public UserBookSegmentDetails() {
    }

    public UserBookSegmentDetails(UserBookDetails userBookDetails, BookSegment bookSegment) {
        this.userBookDetails = userBookDetails;
        this.bookSegment = bookSegment;
    }

    public UserBookDetails getUserBookDetails() {
        return userBookDetails;
    }

    public BookSegment getBookSegment() {
        return bookSegment;
    }

    @ForeignCollectionField(eager = false, foreignFieldName = "bookSegment", orderColumnName = "id")
    public ForeignCollection<MetaAnnotation> annotations;

    @ForeignCollectionField(eager = false, foreignFieldName = "bookSegment", orderColumnName = "id")
    public ForeignCollection<MetaBookmark> bookmarks;

}
