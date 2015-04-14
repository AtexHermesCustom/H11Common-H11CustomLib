package com.atex.h11.custom.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.atex.media.converter.interfaces.IConverter;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;
import com.unisys.media.cr.adapter.ncm.common.data.NCMNewspaperIdentificator;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMNewspaperBuildProperties;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMNewspaperValueClient;
import com.unisys.media.ncm.cfg.common.data.values.LevelValue;
import com.unisys.media.ncm.cfg.common.data.values.LevelTreeValue;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;

public class Newspaper {
    
    private static final String loggerName = Newspaper.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);	
	
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder docBuilder = null;
    private NCMDataSource ds = null;	
    private NCMNewspaperBuildProperties buildProps = null;
    
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		// default date format
    private String convertFormat = null;   	// default = no converter    
	
    public Newspaper (NCMDataSource ds) throws ParserConfigurationException {
        this.ds = ds;
        this.docBuilder = docBuilderFactory.newDocumentBuilder();
    }
    
    public void setDateFormat(DateFormat dateFormat) {
    	this.dateFormat = dateFormat;
    }
    
    public void setConvertFormat(String convertFormat) {
    	this.convertFormat = convertFormat;
    }
    
    public void setBuildProperties(NCMNewspaperBuildProperties buildProps) {
    	this.buildProps = buildProps;
    }
    
    private NCMNewspaperBuildProperties getBuildProperties() {
    	if (buildProps == null) {
    		// default settings
	        buildProps = new NCMNewspaperBuildProperties();
	        buildProps.setDateFormat(dateFormat);
	        buildProps.setIncludeEditions(true);
	        buildProps.setIncludePhysPages(true);
	        buildProps.setIncludeLogPages(true);
	        buildProps.setIncludeObjects(true);
	        buildProps.setIncludeIPTC(true);
	        buildProps.setIncludeObjContent(true);
	        buildProps.setIncludeMetadataGroups(new Vector<String>());
	        buildProps.setXhtmlNestedAsXml(true);
	        buildProps.setNeutralNestedAsXml(true);
	        if (convertFormat != null && (convertFormat.equalsIgnoreCase(IConverter.FMT_NEUTRAL)
	                || convertFormat.equalsIgnoreCase(IConverter.FMT_XHTML)
	                || convertFormat.equalsIgnoreCase(IConverter.FMT_ICML)
	                || convertFormat.equalsIgnoreCase(IConverter.FMT_FLATTEXT)
	                || convertFormat.equalsIgnoreCase(IConverter.FMT_NEWSROOM)
	                || convertFormat.equalsIgnoreCase(IConverter.FMT_INCOPY))) {
	            // InCopy, NewsRoom, FlatText, Icml, Xhtml, Neutral
	        	buildProps.setIncludeConvertTo(convertFormat);
	        }       
    	}
        return buildProps;
    }
    
    public String[] getNewspapers () {
        String[] strNewspapers = null;
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelTreeValue rootLTV = cfgVC.getAllLevels();
        //byte[] levelId = rootLTV.getId();
        String strLevelName = rootLTV.getName();
        System.out.println(strLevelName);
        List<LevelTreeValue> childLevels = rootLTV.getChildLevelsList();
        Vector<String> npV = new Vector<String>();
        for (LevelTreeValue ltv : childLevels) {
            if (ltv == null) continue;
            npV.add(ltv.getName());
        }
        if (!npV.isEmpty()) {
            strNewspapers = new String[npV.size()];
            npV.toArray(strNewspapers);
        }
        return strNewspapers;
    }
    
    public Document getDocument (String strLevel, int pubDate) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        return getDocument(levelV.getId(), pubDate);
    }
    
    public Document getDocument (byte[] level, int pubDate) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8*1024*1024);
        write(level, pubDate, out);
        byte[] bytes = out.toByteArray();
        out.close();
        return docBuilder.parse(new ByteArrayInputStream(bytes));
    }
    
    public void write (String strLevel, int pubDate, OutputStream out) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        write(levelV.getId(), pubDate, out);
    }
    
    public void write (byte[] level, int pubDate, OutputStream out)
            throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {
    	logger.entering(loggerName, "write: level=" + level + ", pubdate=" + pubDate);    	
        NCMNewspaperIdentificator pk = new NCMNewspaperIdentificator(level, pubDate);
        write(pk, out);
        logger.exiting(loggerName, "write");
    }
    
    public void write (NCMNewspaperIdentificator pk, OutputStream out)
    		throws UnsupportedEncodingException, IOException, XMLSerializeWriterException { 
    	logger.entering(loggerName, "write: newspaper pk=" + pk.getIdenticator());        	
    	NCMNewspaperValueClient npVC = (NCMNewspaperValueClient) ds.getNode(pk, getBuildProperties());
        XMLSerializeWriter w = new XMLSerializeWriter(out);
        w.writeObject(npVC, getBuildProperties());
        w.close();
        logger.exiting(loggerName, "write");
    }

}