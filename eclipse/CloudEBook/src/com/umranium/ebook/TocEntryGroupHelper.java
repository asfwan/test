package com.umranium.ebook;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.umranium.ebook.epub.DomHelper;
import com.umranium.ebook.model.BookTocEntry;

public class TocEntryGroupHelper {

    Map<Long, com.umranium.ebook.model.BookTocEntry> entryMap;
    Map<Long, List<Long>> entryTree;
    List<com.umranium.ebook.model.BookTocEntry> rootEntries;

    public TocEntryGroupHelper(com.umranium.ebook.model.Book book) {
        int size = book.tableOfContentsEntries.size();
        entryMap = new HashMap<Long, com.umranium.ebook.model.BookTocEntry>(
                size);
        entryTree = new HashMap<Long, List<Long>>(size);
        rootEntries = new ArrayList<com.umranium.ebook.model.BookTocEntry>(size);

        CloseableWrappedIterable<com.umranium.ebook.model.BookTocEntry> entries = book.tableOfContentsEntries
                .getWrappedIterable();
        try {
            for (com.umranium.ebook.model.BookTocEntry entry : entries) {
                if (!entryMap.containsKey(entry.getId())) {
                    entryMap.put(entry.getId(), entry);
                }
                if (!entryTree.containsKey(entry.getId())) {
                    entryTree.put(entry.getId(), new ArrayList<Long>());
                }
                if (entry.parent != null) {
                    // Log.d(TAG,
                    // "TOC Entry '"+entry.title+"'("+entry.id+") has parent "+entry.parent.id);
                    if (!entryTree.containsKey(entry.parent.getId())) {
                        entryTree.put(entry.parent.getId(), new ArrayList<Long>());
                    }
                    entryTree.get(entry.parent.getId()).add(entry.getId());
                    // entryTree.get(entry.id).add(entry.parent.id);
                } else {
                    // Log.d(TAG,
                    // "TOC Entry '"+entry.title+"'("+entry.id+")  has NO parent");
                    rootEntries.add(entry);
                }
            }
        } finally {
            try {
                entries.close();
            } catch (SQLException e) {
                // ignore exception
            }
        }
    }

    public List<com.umranium.ebook.model.BookTocEntry> getRootEntries() {
        return rootEntries;

        // List<com.umranium.ebook.model.BookTocEntry> entries =
        // new
        // ArrayList<com.umranium.ebook.model.BookTocEntry>(rootEntries.size());
        // for (int i=0; i<rootEntries.size(); ++i) {
        // com.umranium.ebook.model.BookTocEntry orig = rootEntries.get(i);
        //
        // com.umranium.ebook.model.BookTocEntry fin = new BookTocEntry();
        // fin.id = orig.id;
        // fin.href = orig.href;
        // fin.title = "item " + (i+1);
        // entries.add(fin);
        // }
        // return entries;
    }

    public boolean isLeaf(com.umranium.ebook.model.BookTocEntry entry) {
        return entryTree.get(entry.getId()).isEmpty();
    }

    public List<com.umranium.ebook.model.BookTocEntry> getChildEntries(
            com.umranium.ebook.model.BookTocEntry entry) {
        List<Long> entryIds = entryTree.get(entry.getId());
        List<com.umranium.ebook.model.BookTocEntry> entries = new ArrayList<com.umranium.ebook.model.BookTocEntry>(
                entryIds.size());
        for (int i = 0; i < entryIds.size(); ++i) {
            com.umranium.ebook.model.BookTocEntry orig = entryMap.get(entryIds
                    .get(i));

            // com.umranium.ebook.model.BookTocEntry fin = new BookTocEntry();
            // fin.id = orig.id;
            // fin.href = orig.href;
            // fin.title = entry.title + "." + (i+1);
            // fin.parent = orig.parent;
            // entries.add(fin);

            entries.add(orig);
        }
        return entries;
    }

    public int getTotalCount() {
        return entryMap.size();
    }

    public BookTocEntry findNearestEntry(String href) {
        String pageHref = DomHelper.removeAnyAnchor(href);

        BookTocEntry foundEntry = null;
        for (BookTocEntry entry : entryMap.values()) {
            if (href.equals(entry.href)) {
                foundEntry = entry;
                break;
            }
            if (DomHelper.removeAnyAnchor(href).equals(pageHref)) {
                foundEntry = entry;
            }
        }

        return foundEntry;
    }

}