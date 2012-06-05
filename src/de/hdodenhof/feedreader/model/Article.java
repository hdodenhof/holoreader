package de.hdodenhof.feedreader.model;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.text.Html;

import android.text.Spanned;

public class Article {

    private long id;
    private long feedId;
    private Date pubDate;
    private String guid;
    private String title;
    private String summary;
    private String content;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String text) {
        this.summary = text;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSummary() {
        return this.summary;
    }

    public String getContent() {
        return this.content;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public Spanned getFormatedContent() {

        if (content != null && content.length() != 0) {
            Document doc = Jsoup.parse(content);
            doc.select("img").remove();
            return Html.fromHtml(doc.toString());
        } else {
            return Html.fromHtml("<h1>Full article not supplied</h1>");
        }

    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        return this.getTitle();
    }

}
