package info.tongrenlu.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackBean implements Parcelable {

    private String articleId = null;

    private String fileId = null;

    private String artist = null;

    private String title = null;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.articleId == null) ? 0 : this.articleId.hashCode());
        result = prime * result
                + ((this.fileId == null) ? 0 : this.fileId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        TrackBean other = (TrackBean) obj;
        if (this.articleId == null) {
            if (other.articleId != null) {
                return false;
            }
        } else if (!this.articleId.equals(other.articleId)) {
            return false;
        }
        if (this.fileId == null) {
            if (other.fileId != null) {
                return false;
            }
        } else if (!this.fileId.equals(other.fileId)) {
            return false;
        }
        return true;
    }

}
