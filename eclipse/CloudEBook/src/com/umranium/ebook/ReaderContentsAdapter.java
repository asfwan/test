package com.umranium.ebook;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.umranium.ebook.model.BookTocEntry;

public class ReaderContentsAdapter extends BaseAdapter {

    private Context context;
    private List<ContentItem> items;
    private int maxDepth;
    private Bitmap emptyBm;

    public ReaderContentsAdapter(Context context, TocEntryGroupHelper entryGroupHelper) {
        super();

        this.context = context;
        this.items = new ArrayList<ContentItem>(entryGroupHelper.getTotalCount());
        this.maxDepth = 0;

        this.emptyBm = BitmapFactory.decodeResource(context.getResources(), R.drawable.blank_gray);

        fill(0, entryGroupHelper.getRootEntries(), entryGroupHelper);
    }

    private void fill(int depth, List<com.umranium.ebook.model.BookTocEntry> entries, TocEntryGroupHelper entryGroupHelper) {
        if (depth > maxDepth) {
            maxDepth = depth;
        }
        for (int i = 0; i < entries.size(); ++i) {
            com.umranium.ebook.model.BookTocEntry entry = entries.get(i);
            items.add(new ContentItem(entry, depth, entryGroupHelper.isLeaf(entry)));
            List<com.umranium.ebook.model.BookTocEntry> childEntries =
                    entryGroupHelper.getChildEntries(entry);
            fill(depth + 1, childEntries, entryGroupHelper);
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position).entry;
    }

    public com.umranium.ebook.model.BookTocEntry getEntry(int position) {
        return items.get(position).entry;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).entry.getId();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ContentItem item = items.get(position);
        if (convertView == null) {
            convertView = inflateViewForItem(item);
        }
        processView(convertView, item);
        return convertView;
    }

    private void processView(View view, ContentItem item) {
        float scale = context.getResources().getDisplayMetrics().densityDpi / 160.0f;
        float depthImgAdj = item.depth * 5 * scale;

        ImageView thumbnail = (ImageView) view.findViewById(R.id.reader_content_row_thumbnail);
        if (item.entry.thumbnail != null) {
            thumbnail.setImageBitmap(BitmapFactory.decodeByteArray(
                    item.entry.thumbnail, 0, item.entry.thumbnail.length));
        } else {
            thumbnail.setImageBitmap(emptyBm);
        }

        thumbnail.setMaxWidth((int) (
                context.getResources().getDimension(R.dimen.reader_content_row_thumbnail_maxwidth)
                        - depthImgAdj));
        thumbnail.setMaxHeight((int) (
                context.getResources().getDimension(R.dimen.reader_content_row_thumbnail_maxheight)
                        - depthImgAdj));

//		thumbnail.setVisibility(View.INVISIBLE);

        TextView title = (TextView) view.findViewById(R.id.reader_content_row_title);
        title.setText(item.entry.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20 - item.depth * 2);

        view.setPadding(20 + item.depth * 20, 0, 0, 0);
    }

    private View inflateViewForItem(ContentItem item) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return layoutInflater.inflate(R.layout.reader_content_row, null);
    }

}

class ContentItem {
    final com.umranium.ebook.model.BookTocEntry entry;
    final int depth;
    final boolean isLeaf;

    public ContentItem(BookTocEntry entry, int depth, boolean isLeaf) {
        this.entry = entry;
        this.depth = depth;
        this.isLeaf = isLeaf;
    }
}
