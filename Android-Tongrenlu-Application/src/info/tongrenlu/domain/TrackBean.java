package info.tongrenlu.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackBean implements Parcelable {

    private String articleId = null;

    private String fileId = null;

    private String album = null;

    private String artist = null;

    private String name = null;

    private String original = null;

    private int trackNumber = 0;

    private String downloadFlg = "0";

    public String getAlbum() {
        return this.album;
    }

    public void setAlbum(final String album) {
        this.album = album;
    }

    public String getOriginal() {
        return this.original;
    }

    public void setOriginal(final String original) {
        this.original = original;
    }

    public int getTrackNumber() {
        return this.trackNumber;
    }

    public void setTrackNumber(final int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getDownloadFlg() {
        return this.downloadFlg;
    }

    public void setDownloadFlg(final String downloadFlg) {
        this.downloadFlg = downloadFlg;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.getArticleId() == null) ? 0 : this.getArticleId().hashCode());
        result = prime * result
                + ((this.getFileId() == null) ? 0 : this.getFileId().hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final TrackBean other = (TrackBean) obj;
        if (this.getArticleId() == null) {
            if (other.getArticleId() != null) {
                return false;
            }
        } else if (!this.getArticleId().equals(other.getArticleId())) {
            return false;
        }
        if (this.getFileId() == null) {
            if (other.getFileId() != null) {
                return false;
            }
        } else if (!this.getFileId().equals(other.getFileId())) {
            return false;
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeStringArray(new String[] { String.valueOf(this.getArticleId()),
                String.valueOf(this.getFileId()),
                this.album,
                this.name,
                this.artist,
                this.original,
                String.valueOf(this.trackNumber),
                String.valueOf(this.downloadFlg) });
    }

    public String getArtist() {
        return this.artist;
    }

    public void setArtist(final String artist) {
        this.artist = artist;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public static final Parcelable.Creator<TrackBean> CREATOR = new Parcelable.Creator<TrackBean>() {
        @Override
        public TrackBean createFromParcel(final Parcel in) {
            final String[] data = new String[8];
            in.readStringArray(data);

            final TrackBean trackBean = new TrackBean();
            trackBean.setArticleId(data[0]);
            trackBean.setFileId(data[1]);
            trackBean.album = data[2];
            trackBean.name = data[3];
            trackBean.artist = data[4];
            trackBean.original = data[5];
            trackBean.trackNumber = Integer.valueOf(data[6]);
            trackBean.downloadFlg = data[7];
            return trackBean;
        }

        @Override
        public TrackBean[] newArray(final int size) {
            return new TrackBean[size];
        }
    };
}
