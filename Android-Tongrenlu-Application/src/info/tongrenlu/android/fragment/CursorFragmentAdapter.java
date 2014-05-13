package info.tongrenlu.android.fragment;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public abstract class CursorFragmentAdapter extends FragmentPagerAdapter {

    protected boolean mDataValid;
    protected Cursor mCursor;
    protected int mRowIDColumn;

    public CursorFragmentAdapter(final FragmentManager fm, Cursor cursor) {
        super(fm);
        this.init(cursor);
    }

    void init(Cursor c) {
        boolean cursorPresent = c != null;
        this.mCursor = c;
        this.mDataValid = cursorPresent;
        this.mRowIDColumn = cursorPresent ? c.getColumnIndexOrThrow("_id") : -1;
    }

    @Override
    public int getCount() {
        if (this.mDataValid && this.mCursor != null) {
            return this.mCursor.getCount();
        } else {
            return 0;
        }
    }

    public Cursor getCursor() {
        return this.mCursor;
    }

    @Override
    public Fragment getItem(int position) {
        if (this.mDataValid && this.mCursor != null) {
            this.mCursor.moveToPosition(position);
            return this.newFragment(this.mCursor);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if (this.mDataValid && this.mCursor != null) {
            if (this.mCursor.moveToPosition(position)) {
                return this.mCursor.getLong(this.mRowIDColumn);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    protected abstract Fragment newFragment(Cursor cursor);

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == this.mCursor) {
            return null;
        }
        Cursor oldCursor = this.mCursor;
        this.mCursor = newCursor;
        if (newCursor != null) {
            this.mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
            this.mDataValid = true;
            // notify the observers about the new cursor
            this.notifyDataSetChanged();
        } else {
            this.mRowIDColumn = -1;
            this.mDataValid = false;
        }
        return oldCursor;
    }
}
