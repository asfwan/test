package com.umranium.ebook.services;

import java.util.List;

import com.umranium.ebook.model.Book;
import com.umranium.ebook.model.BookSegment;
import com.umranium.ebook.model.Settings;
import com.umranium.ebook.model.UserBookDetails;
import com.umranium.ebook.model.UserBookSegmentDetails;
import com.umranium.ebook.model.UserDetails;

public interface IWebHostService {

    public void addEventListener(WebHostEventListener listener);

    public void removeEventListener(WebHostEventListener listener);

    public boolean isServerStarted();

    public String getHostUrl();

    public int getPort();

    public DatabaseHelper getDatabaseHelper();

    public Settings getSettings();

    public UserDetails getLoggedInUser();

    public UserDetails getUser(String username);

    public UserDetails createUser(String username);

    public void logInUser(UserDetails user);

    public UserBookDetails getUserBookDetails(UserDetails user, Book book);

    public UserBookDetails createUserBookDetails(UserDetails user, Book book);

    public UserBookSegmentDetails createUserBookSegmentDetails(UserBookDetails userBookDetails, BookSegment bookSegment);

    public List<com.umranium.ebook.model.Book> getLibraryBooks();

    public List<com.umranium.ebook.model.BookResource> getResource(String href);

    public com.umranium.ebook.model.Book getBook(long id);

}
