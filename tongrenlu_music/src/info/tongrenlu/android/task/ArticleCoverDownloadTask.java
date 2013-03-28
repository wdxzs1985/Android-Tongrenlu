package info.tongrenlu.android.task;

import info.tongrenlu.app.HttpConstants;

import java.io.File;

import android.widget.ImageView;

public class ArticleCoverDownloadTask extends FileDownloadTask {

    private final ImageView mView;
    private final String mArticleId;

    public ArticleCoverDownloadTask(final ImageView view, final String articleId) {
        this.mView = view;
        this.mArticleId = articleId;
    }

    @Override
    protected void onPreExecute() {
        this.mView.setTag(this.mArticleId);
    }

    @Override
    protected void onPostExecute(final File data) {
        if (data == null || !data.isFile()) {
            // Download Failed;
            return;
        }
        if (this.mView == null || this.mView.getTag() != this.mArticleId) {
            // View Changed;
            return;
        }
        HttpConstants.setImage(this.mView, data);
    }

}
