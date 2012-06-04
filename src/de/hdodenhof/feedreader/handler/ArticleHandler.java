package de.hdodenhof.feedreader.handler;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.hdodenhof.feedreader.model.Article;

public class ArticleHandler extends DefaultHandler {
    private Article a = null;

    private ArrayList<Article> articleList;
    private StringBuffer mSb;

    private boolean isGuid = false;
    private boolean isArticle = false;
    private boolean isTitle = false;
    private boolean isContent = false;
    private boolean isSummary = false;

    public ArticleHandler(ArrayList<Article> articles) {
        super();
        this.articleList = articles;
    }
    
    public ArrayList<Article> getArticles(){
        return this.articleList;
    }

    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

        mSb = new StringBuffer();

        if (localName.equalsIgnoreCase("TITLE") && isArticle == true) {
            isTitle = true;
        }

        else if ((localName.equalsIgnoreCase("DESCRIPTION") || localName.equalsIgnoreCase("SUMMARY")) && isArticle == true) {
            isSummary = true;
        }

        else if ((localName.equalsIgnoreCase("ENCODED") || localName.equalsIgnoreCase("CONTENT")) && isArticle == true) {
            isContent = true;
        }

        else if ((localName.equalsIgnoreCase("GUID") || localName.equalsIgnoreCase("ID")) && isArticle == true) {
            isGuid = true;
        }        
        
        else if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
            a = new Article();
            isArticle = true;
        }

    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (isArticle) {
            if (localName.equalsIgnoreCase("ITEM") || localName.equalsIgnoreCase("ENTRY")) {
                articleList.add(a);
                a = null;
                isArticle = false;
            } 
            
            else if (isTitle) {
                a.setTitle(mSb.toString().trim());
                isTitle = false;
            } 
            
            else if (isSummary) {
                a.setSummary(mSb.toString().trim());
                isSummary = false;
            } 

            else if (isGuid) {
                a.setGuid(mSb.toString().trim());
                isGuid = false;
            }             
            
            else if (isContent) {
                a.setContent(mSb.toString().trim());
                isContent = false;
            }

        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {

        if (isArticle && (isTitle || isContent || isSummary || isGuid)) {
            mSb.append(new String(ch, start, length));
        }

    }
}
