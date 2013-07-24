package com.umranium.ebook.model;

import java.io.Serializable;
import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "books")
public class Book extends BaseEntity<Book> implements Serializable {

    private static final long serialVersionUID = -4997666894215798088L;

    @DatabaseField(canBeNull = false, unique = true, index = true)
    protected String identifier;

    @Column(nullable = false, length = DbCommon.MAX_DESCR_STR_LEN)
    public String title;

    @Column(nullable = true, length = DbCommon.MAX_DESCR_STR_LEN)
    public String authors;

    @Column(nullable = true, length = DbCommon.MAX_DESCR_STR_LEN)
    public String publisher;

    @Column(nullable = true, length = DbCommon.MAX_DESCR_STR_LEN)
    public String edition;

    @Column(nullable = true, length = DbCommon.MAX_CONTENT_URL_LEN)
    public String coverResourceHref;

    @Column(nullable = false)
    public long lastIssuedId;

    //@OneToMany(mappedBy="book")
    @ForeignCollectionField(eager = false, foreignFieldName = "book", orderColumnName = "id")
    public ForeignCollection<BookResource> resources;

    @ForeignCollectionField(eager = false, foreignFieldName = "book", orderColumnName = "id")
    public ForeignCollection<BookSegment> segments;

    //@OneToMany(mappedBy="section")
    @ForeignCollectionField(eager = false, foreignFieldName = "book", orderColumnName = "id")
    public ForeignCollection<BookTocEntry> tableOfContentsEntries;

    public Book() {
    }

    public Book(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BookSegment findBookSegment(String href) {
        CloseableWrappedIterable<BookSegment> it = segments.getWrappedIterable();
        try {
            for (BookSegment seg : it) {
                if (seg.href.equals(href)) {
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
