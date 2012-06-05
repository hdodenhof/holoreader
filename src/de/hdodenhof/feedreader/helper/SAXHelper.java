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
    GenericHandler handler;
    SAXParserFactory factory;
    SAXParser saxParser;
    XMLReader xmlReader;
    InputSource inputSource;
    
    public SAXHelper(GenericHandler handler) throws ParserConfigurationException, SAXException{
        this.handler = handler;
        setup();    
    }

    public SAXHelper(GenericHandler handler, String url) throws ParserConfigurationException, SAXException, IOException{
        this.handler = handler;
        setup();
        setUrl(url);
    }  
    
    private void setup() throws ParserConfigurationException, SAXException{
        this.factory = SAXParserFactory.newInstance();
        this.saxParser = factory.newSAXParser();
        this.xmlReader = saxParser.getXMLReader();
        setHandler();         
    }
    
    public void setUrl(String url) throws IOException{
        URL rssUrl = new URL(url);
        inputSource = new InputSource(rssUrl.openStream());        
    }
    
    public void setHandler() {
        this.xmlReader.setContentHandler((DefaultHandler) handler);   
    }    
    
    public Object parse() throws IOException, SAXException{
        xmlReader.parse(inputSource);
        return getHandlerResult();
    }
    
    public Object getHandlerResult(){
        return handler.getResult();
    }

}
