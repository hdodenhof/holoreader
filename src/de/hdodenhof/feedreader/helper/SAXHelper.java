package de.hdodenhof.feedreader.helper;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.hdodenhof.feedreader.handler.GenericHandler;

public class SAXHelper {
        GenericHandler mHandler;
        SAXParserFactory mSAXParserFactory;
        SAXParser mSAXParser;
        XMLReader mXMLReader;
        InputSource mInputSource;

        public SAXHelper(String url, GenericHandler handler) throws ParserConfigurationException, SAXException, IOException {
                this.mHandler = handler;
                this.mSAXParserFactory = SAXParserFactory.newInstance();
                this.mSAXParser = mSAXParserFactory.newSAXParser();
                this.mXMLReader = mSAXParser.getXMLReader();
                this.mXMLReader.setContentHandler((DefaultHandler) handler);
                this.mInputSource = new InputSource((new URL(url)).openStream());
        }

        public Object parse() throws IOException, SAXException {
                mXMLReader.parse(mInputSource);
                return getHandlerResult();
        }

        public Object getHandlerResult() {
                return mHandler.getResult();
        }

}
