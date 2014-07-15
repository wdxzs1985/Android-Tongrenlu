package info.tongrenlu.domain;

public class ArticleBean {

    private String articleId;

    private String title;

    private String description;

    public String getArticleId() {
        return this.articleId;
    }

    public void setArticleId(final String articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.articleId == null) ? 0 : this.articleId.hashCode());
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
        final ArticleBean other = (ArticleBean) obj;
        if (this.articleId == null) {
            if (other.articleId != null) {
                return false;
            }
        } else if (!this.articleId.equals(other.articleId)) {
            return false;
        }
        return true;
    }

}
