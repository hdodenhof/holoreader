package de.hdodenhof.feedreader.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class FeedHandler extends DefaultHandler implements GenericHandler {

        @SuppressWarnings("unused")
        private static final String TAG = FeedHandler.class.getSimpleName();          
        
        private String mName;
        private StringBuffer mSb;

        private boolean mIsFound = false;
        private boolean mIsTitle = false;

        public FeedHandler() {
                super();
        }

        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {

                if (localName.equalsIgnoreCase("TITLE") && !mIsFound) {
                        mSb = new StringBuffer();
                        mIsTitle = true;
                }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

                if (!mIsFound) {
                        if (localName.equalsIgnoreCase("TITLE")) {
                                mName = mSb.toString().trim();
                                mIsTitle = false;
                                mIsFound = true;
                        }
                }

        }

        public void characters(char[] ch, int start, int length) throws SAXException {

                if (mIsTitle) {
                        mSb.append(new String(ch, start, length));
                }

        }

        public Object getResult() {
                return this.mName;
        }
}
