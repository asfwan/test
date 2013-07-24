package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.j256.ormlite.field.DatabaseField;

@Entity(name = "book_segment")
public class BookSegment extends BaseEntity<BookSegment> implements Serializable {

    private static final long serialVersionUID = -2362430133018914355L;

    @ManyToOne
    @Column(nullable = false)
    protected Book book;

    //@Column(nullable=false, length=DbCommon.MAX_CONTENT_URL_LEN)
    @DatabaseField(width = DbCommon.MAX_CONTENT_URL_LEN, canBeNull = false, unique = true, index = true)
    public String href;

    public BookSegment() {
    }

    public BookSegment(Book book) {
        this.book = book;
    }

    public Book getBook() {
        return book;
    }


}
