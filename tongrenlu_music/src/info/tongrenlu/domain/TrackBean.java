package info.tongrenlu.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackBean implements Parcelable {

    private String articleId = null;

    private String fileId = null;

    private String artist = null;

    private String title = null;

    private boolean downloaded = false;

    public String getFileId() {
        return this.fileId;
    }

    public void setFileId(final String fileId) {
        this.fileId = fileId;
    }

    public String getArtist() {
        return this.artist;
    }

    public void setArtist(final String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getArticleId() {
        return this.articleId;
    }

    public void setArticleId(final String articleId) {
        this.articleId = articleId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeStringArray(new String[] { this.articleId,
                this.fileId,
                this.title,
                this.artist });
    }

    public boolean isDownloaded() {
        return this.downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public static final Parcelable.Creator<TrackBean> CREATOR = new Parcelable.Creator<TrackBean>() {
        @Override
        public TrackBean createFromParcel(final Parcel in) {
            final String[] data = new String[4];
            in.readStringArray(data);

            final TrackBean trackBean = new TrackBean();
            trackBean.articleId = data[0];
            trackBean.fileId = data[1];
            trackBean.title = data[2];
            trackBean.artist = data[3];
            return trackBean;
        }

        @Override
        public TrackBean[] newArray(final int size) {
            return new TrackBean[size];
        }
    };

}
