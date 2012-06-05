package de.hdodenhof.feedreader.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FeedHandler extends DefaultHandler implements GenericHandler {

    private String name;
    private StringBuffer mSb;

    private boolean isFound = false;
    private boolean isTitle = false;

    public FeedHandler() {
        super();
    }

    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

        if (localName.equalsIgnoreCase("TITLE") && !isFound) {
            mSb = new StringBuffer();
            isTitle = true;
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

        if (!isFound) {
            if (localName.equalsIgnoreCase("TITLE")) {
                name = mSb.toString().trim();
                isTitle = false;
                isFound = true;
            } 
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {

        if (isTitle) {
            mSb.append(new String(ch, start, length));
        }

    }

    public Object getResult() {
        return this.name;
    }
}
