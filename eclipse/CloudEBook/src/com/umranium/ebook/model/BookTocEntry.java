package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "book_tocentries")
public class BookTocEntry extends BaseEntity<BookTocEntry> implements Serializable {

    private static final long serialVersionUID = -255902562355463548L;

    @ManyToOne
    @Column(nullable = false)
    protected Book book;

    @ManyToOne
    @Column(nullable = true)
//	@DatabaseField(foreign=true, canBeNull=true)
    public BookTocEntry parent;

    @Column(nullable = false, length = DbCommon.MAX_DESCR_STR_LEN)
    public String title;

    @Column(nullable = false, length = DbCommon.MAX_CONTENT_URL_LEN)
    public String href;

    //@Column(nullable=true, length=DbCommon.MAX_THUMBNAIL_LEN)
    @DatabaseField(canBeNull = true, width = DbCommon.MAX_THUMBNAIL_LEN, dataType = DataType.BYTE_ARRAY)
    public byte[] thumbnail;

    //@OneToMany(mappedBy="parent")
    @ForeignCollectionField(eager = true, foreignFieldName = "parent")
    public ForeignCollection<BookTocEntry> childSubSections;

    public BookTocEntry() {
    }

    public BookTocEntry(Book book) {
        this.book = book;
    }

    public Book getBook() {
        return book;
    }

}
