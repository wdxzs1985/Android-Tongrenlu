package info.tongrenlu.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackBean implements Parcelable {

    private String articleId = null;

    private String fileId = null;

    private String album = null;

    private String leadArtist = null;

    private String songTitle = null;

    private String original = null;

    private int trackNumber = 0;

    private int downloadFlg = 0;

    public String getFileId() {
        return this.fileId;
    }

    public void setFileId(final String fileId) {
        this.fileId = fileId;
    }

    public String getArticleId() {
        return this.articleId;
    }

    public void setArticleId(final String articleId) {
        this.articleId = articleId;
    }

    public String getAlbum() {
        return this.album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getLeadArtist() {
        return this.leadArtist;
    }

    public void setLeadArtist(String leadArtist) {
        this.leadArtist = leadArtist;
    }

    public String getSongTitle() {
        return this.songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public String getOriginal() {
        return this.original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public int getTrackNumber() {
        return this.trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public int getDownloadFlg() {
        return this.downloadFlg;
    }

    public void setDownloadFlg(int downloadFlg) {
        this.downloadFlg = downloadFlg;
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeStringArray(new String[] { this.articleId,
                this.fileId,
                this.album,
                this.songTitle,
                this.leadArtist,
                this.original,
                String.valueOf(this.trackNumber),
                String.valueOf(this.downloadFlg) });
    }

    public static final Parcelable.Creator<TrackBean> CREATOR = new Parcelable.Creator<TrackBean>() {
        @Override
        public TrackBean createFromParcel(final Parcel in) {
            final String[] data = new String[8];
            in.readStringArray(data);

            final TrackBean trackBean = new TrackBean();
            trackBean.articleId = data[0];
            trackBean.fileId = data[1];
            trackBean.album = data[2];
            trackBean.songTitle = data[3];
            trackBean.leadArtist = data[4];
            trackBean.original = data[5];
            trackBean.trackNumber = Integer.valueOf(data[6]);
            trackBean.downloadFlg = Integer.valueOf(data[7]);
            return trackBean;
        }

        @Override
        public TrackBean[] newArray(final int size) {
            return new TrackBean[size];
        }
    };
}
