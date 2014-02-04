/*
 * Copyright (C) 2012 Lucas Rocha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.adapter.MusicListAdapter;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.support.HttpHelper;

import org.lucasr.smoothie.SimpleItemLoader;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.widget.Adapter;

public class GalleryLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    private final Context mContext;
    private final BitmapLruCache mCache;

    public GalleryLoader(Context context, BitmapLruCache cache) {
        this.mContext = context;
        this.mCache = cache;
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        ArticleBean articleBean = (ArticleBean) adapter.getItem(position);
        String articleId = articleBean.getArticleId();
        return HttpConstants.getCoverUrl(this.mContext,
                                         articleId,
                                         HttpConstants.M_COVER);
    }

    @Override
    public CacheableBitmapDrawable loadItemFromMemory(String url) {
        return this.mCache.getFromMemoryCache(url);
    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {
        CacheableBitmapDrawable wrapper = this.mCache.get(url);
        if (wrapper == null) {
            wrapper = this.mCache.put(url, HttpHelper.loadImage(url));
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        MusicListAdapter.ViewHolder holder = (MusicListAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        if (fromMemory) {
            holder.coverView.setImageDrawable(result);
        } else {
            BitmapDrawable emptyDrawable = new BitmapDrawable(itemView.getResources());
            TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                    result });

            holder.coverView.setImageDrawable(fadeInDrawable);
            fadeInDrawable.startTransition(200);
        }
    }
}
