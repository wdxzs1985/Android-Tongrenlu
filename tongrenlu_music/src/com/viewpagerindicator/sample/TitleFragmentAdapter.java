package com.viewpagerindicator.sample;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


public class TitleFragmentAdapter extends FragmentPagerAdapter {

    private List<TitleFragment> mContents = null;

    public TitleFragmentAdapter(final FragmentManager fm) {
        super(fm);
        this.mContents = new ArrayList<TitleFragment>();
    }

    @Override
    public Fragment getItem(final int position) {
        return this.mContents.get(position);
    }

    @Override
    public int getCount() {
        return this.mContents.size();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        return this.mContents.get(position).getTitle();
    }

    public void addItem(final TitleFragment fragment) {
        this.mContents.add(fragment);
    }
}