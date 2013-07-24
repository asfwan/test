package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

@Entity(name = "book_resources")
public class BookResource extends BaseEntity<BookResource> implements Serializable {

    private static final long serialVersionUID = -7414226625597902531L;

    @ManyToOne
    @Column(nullable = false)
    protected Book book;

    //@Column(nullable=false, length=DbCommon.MAX_CONTENT_URL_LEN)
    @DatabaseField(width = DbCommon.MAX_CONTENT_URL_LEN, canBeNull = false, unique = true, index = true)
    public String href;

    @Column(nullable = false, length = 50)
    public String encoding;

    @Column(nullable = false, length = 50)
    public String mediaType;

    //@Column(nullable=false, length=DbCommon.MAX_DATA_LEN)
    @DatabaseField(canBeNull = false, dataType = DataType.BYTE_ARRAY, width = DbCommon.MAX_DATA_LEN)
    public byte[] data;

    public BookResource() {
    }

    public BookResource(Book book) {
        this.book = book;
    }

    public Book getBook() {
        return book;
    }

}
