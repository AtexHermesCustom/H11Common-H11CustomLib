package com.atex.h11.custom.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.atex.media.converter.interfaces.IConverter;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMLogicalPagePK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMLogicalPageBuildProperties;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMLogicalPageValueClient;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;
import com.unisys.media.ncm.cfg.common.data.values.LevelValue;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;

public class LogPage {

    private static final String loggerName = PhysPage.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);		
	
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder docBuilder = null;
    private NCMDataSource ds = null;    	
    private NCMLogicalPageBuildProperties buildProps = null;
    
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		// default date format
    private String convertFormat = null;   	// default = no converter    
	
    public LogPage (NCMDataSource ds) throws ParserConfigurationException {
        this.ds = ds;
        this.docBuilder = docBuilderFactory.newDocumentBuilder();
    }      
    
    public void setDateFormat(DateFormat dateFormat) {
    	this.dateFormat = dateFormat;
    }
    
    public void setConvertFormat(String convertFormat) {
    	this.convertFormat = convertFormat;
    }
    
    public void setBuildProperties(NCMLogicalPageBuildProperties buildProps) {
    	this.buildProps = buildProps;
    }    
    
    private NCMLogicalPageBuildProperties getBuildProperties() {
    	if (buildProps == null) {
    		// default settings
			buildProps = new NCMLogicalPageBuildProperties();
			buildProps.setIncludeLayoutInPage(true);
			buildProps.setIncludeObjContent(true);
			buildProps.setIncludePageContent(true);
			buildProps.setIncludeLayContent(true); // new
			buildProps.setIncludeIPTC(true);
			buildProps.setDateFormat(dateFormat);
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
        
    public Document getDocument (String strLevel, String strEdtName, int pubDate, String strLogPageName) 
		    throws UnsupportedEncodingException, IOException, 
		           XMLSerializeWriterException, SAXException {
		UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
		LevelValue levelV = cfgVC.findLevelByName(strLevel);
		int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
		return getDocument(levelV.getId(), (short) editionId, pubDate, strLogPageName);
    }

    public Document getDocument (byte[] level, short edtId, int pubDate, String strLogPageName) 
		    throws UnsupportedEncodingException, IOException, 
		           XMLSerializeWriterException, SAXException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(8*1024*1024);
		write(level, edtId, pubDate, strLogPageName, out);
		byte[] bytes = out.toByteArray();
		out.close();
		return docBuilder.parse(new ByteArrayInputStream(bytes));
	}

	public void write (String strLevel, String strEdtName, int pubDate, String strLogPageName, OutputStream out) 
		    throws UnsupportedEncodingException, IOException, 
		           XMLSerializeWriterException {
		UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
		LevelValue levelV = cfgVC.findLevelByName(strLevel);
		int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
		write(levelV.getId(), (short) editionId, pubDate, strLogPageName, out);
	}

	public void write (byte[] level, short edtId, int pubDate, String strLogPageName, OutputStream out)
		    throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {
		logger.entering(loggerName, "write: level=" + level + ", edId=" + edtId + ", pubdate=" + pubDate + ", page=" + strLogPageName);    	
		NCMLogicalPagePK pk = new NCMLogicalPagePK(strLogPageName, pubDate, edtId,
				level, NCMObjectPK.LAST_VERSION, NCMObjectPK.ACTIVE);		
		write(pk, out);
		logger.exiting(loggerName, "write");
	}

	public void write (NCMLogicalPagePK pk, OutputStream out)
		    throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {    
		logger.entering(loggerName, "write: physpage pk=" + pk.getIdenticator(false));    	      	
		NCMLogicalPageValueClient lpVC = (NCMLogicalPageValueClient) ds.getNode(pk, getBuildProperties());
		XMLSerializeWriter w = new XMLSerializeWriter(out);
		w.writeObject(lpVC, getBuildProperties());
		w.close();
		logger.exiting(loggerName, "write");
	}    
    
}
