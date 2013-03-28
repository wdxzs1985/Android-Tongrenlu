package com.viewpagerindicator.sample;

import android.support.v4.app.Fragment;

public class TitleFragment extends Fragment {

    private String mTitle = "???";

    public void setTitle(final String title) {
        this.mTitle = title;
    }

    public String getTitle() {
        return this.mTitle;
    }
}
