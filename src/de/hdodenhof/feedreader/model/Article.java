package de.hdodenhof.feedreader.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;

import android.text.Spanned;

public class Article implements Parcelable {
    private String title;
    private String text;
    private String full;

    public Article() { ; };
    

    public Article(Parcel in) {
        readFromParcel(in);
    }
     
    
    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle(){
        return this.title;
    }
    
    public String getText() {
        return this.text;
    }
    
    public String getFull() {
        return this.full;
    }

    public Spanned getFormatedFull(){
        
        if (full != null && full.length() != 0){
            Document doc = Jsoup.parse(full);
            doc.select("img").remove();  
            return Html.fromHtml(doc.toString());
        } else {
            return Html.fromHtml("<h1>Full article not supplied</h1>");
        }
        
    }
    
    public void setFull(String full) {
        this.full = full;
    }

    public String getSummary(){
        // TODO: Break after full word
        if (this.text.length() > 150){
            return this.text.substring(0, 150);

        } else {
            return this.text;

        }
    }

    public String toString() {
        return this.getTitle() + ": " + this.getSummary() + "\r\n";
    }

    public int describeContents() {
        return 0;
    }
 
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(text);
        dest.writeString(full);
    }
 
    private void readFromParcel(Parcel in) {
 
        title = in.readString();
        text = in.readString();
        full = in.readString();

    }
 
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator CREATOR =
        new Parcelable.Creator() {
            public Article createFromParcel(Parcel in) {
                return new Article(in);
            }
 
            public Article[] newArray(int size) {
                return new Article[size];
            }
        };
}
