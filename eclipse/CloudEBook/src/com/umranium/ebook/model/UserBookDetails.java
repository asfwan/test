package com.umranium.ebook.model;

import java.io.Serializable;
import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "user_book_details")
public class UserBookDetails extends BaseEntity<UserBookDetails> implements Serializable {

    private static final long serialVersionUID = 835034469413260354L;

    public static final String COLUMN_NAME_USER_LIBRARY = "userLibrary_id";
    public static final String COLUMN_NAME_BOOK = "book_id";

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected UserLibrary userLibrary;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    protected Book book;

    @Column(nullable = false)
    public long latestIssuedMetaId;

    public UserBookDetails() {
    }

    public UserBookDetails(UserLibrary userLibrary, Book book) {
        this.userLibrary = userLibrary;
        this.book = book;
    }

    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    public Book getBook() {
        return book;
    }

    @ForeignCollectionField(eager = false, foreignFieldName = "userBookDetails", orderColumnName = "id")
    public ForeignCollection<UserBookSegmentDetails> userBookSegmentDetails;


    public UserBookSegmentDetails findUserBookSegmentDetails(BookSegment bookSegment) {
        CloseableWrappedIterable<UserBookSegmentDetails> it = userBookSegmentDetails.getWrappedIterable();
        try {
            for (UserBookSegmentDetails seg : it) {
                if (seg.bookSegment.id == bookSegment.id) {
                    return seg;
                }
            }
            return null;
        } finally {
            try {
                it.close();
            } catch (SQLException e) {
                //	ignore
            }
        }
    }
}
