package com.atex.h11.custom.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import com.unisys.media.cr.adapter.ncm.common.data.NCMPhysPageIdentificator;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMPhysPageBuildProperties;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMPhysPageValueClient;
import com.unisys.media.ncm.cfg.common.data.values.LevelValue;
import com.unisys.media.ncm.cfg.common.data.values.EditionValue;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;

public class PhysPage {
    
    private static final String loggerName = PhysPage.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);		
	
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder docBuilder = null;
    private NCMDataSource ds = null;    
    private NCMPhysPageBuildProperties buildProps = null;
    
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		// default date format
    private String convertFormat = null;   	// default = no converter    
    
    public PhysPage (NCMDataSource ds) throws ParserConfigurationException {
        this.ds = ds;
        this.docBuilder = docBuilderFactory.newDocumentBuilder();
    }    
    
    public void setDateFormat(DateFormat dateFormat) {
    	this.dateFormat = dateFormat;
    }
    
    public void setConvertFormat(String convertFormat) {
    	this.convertFormat = convertFormat;
    }
    
    public void setBuildProperties(NCMPhysPageBuildProperties buildProps) {
    	this.buildProps = buildProps;
    }
    
    private NCMPhysPageBuildProperties getBuildProperties() {
    	if (buildProps == null) {
    		// default settings
	    	buildProps = new NCMPhysPageBuildProperties();
	    	buildProps.setIncludeObjContent(true);
	    	buildProps.setIncludeLayContent(true);
	    	buildProps.setIncludeLayObjContent(true);
	    	buildProps.setDateFormat(dateFormat);
	    	buildProps.setIncludeIPTC(true);
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
    
    public String[] getEditions (String strLevel) {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        return getEditions(levelV.getId());
    }
    
    public String[] getEditions (byte[] level) {
        String[] strEditions = null;
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        EditionValue[] edtVs = cfgVC.getEditionsByLevelId(level, false);
        if (edtVs != null) {
            strEditions = new String[edtVs.length];
            for (int i = 0; i < edtVs.length; i++) {
                if (edtVs[i] == null || edtVs[i].getMasterEditionId() > 0) continue;
                strEditions[i] = edtVs[i].getName();
            }
        }
        return strEditions;
    }
        
    public Document getDocument (String strLevel, String strEdtName, int pubDate, short seqNum) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
        return getDocument(levelV.getId(), (short) editionId, pubDate, seqNum);
    }
        
    public Document getDocument (byte[] level, short edtId, int pubDate, short seqNum) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8*1024*1024);
        write(level, edtId, pubDate, seqNum, out);
        byte[] bytes = out.toByteArray();
        out.close();
        return docBuilder.parse(new ByteArrayInputStream(bytes));
    }
        
    public void write (String strLevel, String strEdtName, int pubDate, short seqNum, OutputStream out) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
        write(levelV.getId(), (short) editionId, pubDate, seqNum, out);
    }
        
    public void write (byte[] level, short edtId, int pubDate, short seqNum, OutputStream out)
            throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {
    	logger.entering(loggerName, "write: level=" + level + ", edId=" + edtId + ", pubdate=" + pubDate + ", page=" + seqNum);    	
    	NCMPhysPageIdentificator pk = new NCMPhysPageIdentificator(level, pubDate, (short) 0, edtId, seqNum);
    	write(pk, out);
    	logger.exiting(loggerName, "write");
    }

    public void write (NCMPhysPageIdentificator pk, OutputStream out)
		    throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {    
    	logger.entering(loggerName, "write: physpage pk=" + pk.getIdenticator());    	      	
        NCMPhysPageValueClient ppVC = (NCMPhysPageValueClient) ds.getNode(pk, getBuildProperties());
        XMLSerializeWriter w = new XMLSerializeWriter(out);
        w.writeObject(ppVC, getBuildProperties());
        w.close();
        logger.exiting(loggerName, "write");
    }
}
