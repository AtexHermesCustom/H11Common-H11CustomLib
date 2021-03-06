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
import com.unisys.media.cr.adapter.ncm.common.data.NCMEditionIdentificator;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMEditionBuildProperties;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMEditionValueClient;
import com.unisys.media.ncm.cfg.common.data.values.LevelValue;
import com.unisys.media.ncm.cfg.common.data.values.EditionValue;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;

public class Edition {
	
    private static final String loggerName = Edition.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
	
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder docBuilder = null;
    private NCMDataSource ds = null;   	
    private NCMEditionBuildProperties buildProps = null;
    
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		// default date format
    private String convertFormat = null;   	// default = no converter    
    
    public Edition (NCMDataSource ds) throws ParserConfigurationException {
        this.ds = ds;
        this.docBuilder = docBuilderFactory.newDocumentBuilder();
    }    
    
    public void setDateFormat(DateFormat dateFormat) {
    	this.dateFormat = dateFormat;
    }
    
    public void setConvertFormat(String convertFormat) {
    	this.convertFormat = convertFormat;
    }
    
    public void setBuildProperties(NCMEditionBuildProperties buildProps) {
    	this.buildProps = buildProps;
    }
    
    public NCMEditionBuildProperties getBuildProperties() {
    	if (buildProps == null) {
    		// default settings    		
	        buildProps = new NCMEditionBuildProperties();
	        buildProps.setDateFormat(dateFormat);
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
    
    public Document getDocument (String strLevel, String strEdtName, int pubDate) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
        return getDocument(levelV.getId(), (short) editionId, pubDate);
    }
        
    public Document getDocument (byte[] level, short edtId, int pubDate) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8*1024*1024);
        write(level, edtId, pubDate, out);
        byte[] bytes = out.toByteArray();
        out.close();
        return docBuilder.parse(new ByteArrayInputStream(bytes));
    }
        
    public void write (String strLevel, String strEdtName, int pubDate, OutputStream out) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException {
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
        LevelValue levelV = cfgVC.findLevelByName(strLevel);
        int editionId = cfgVC.getEditionByName(levelV.getId(), strEdtName).getEditionId();
        write(levelV.getId(), (short) editionId, pubDate, out);
    }
        
    public void write (byte[] level, short edtId, int pubDate, OutputStream out)
            throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {
    	logger.entering(loggerName, "write: level=" + level + ", edId=" + edtId + ", pubdate=" + pubDate);
        NCMEditionIdentificator pk = new NCMEditionIdentificator(level, pubDate, (short) 0, edtId);
        write(pk, out);
        logger.exiting(loggerName, "write");
    }
    
    public void write (NCMEditionIdentificator pk, OutputStream out)
    		throws UnsupportedEncodingException, IOException, XMLSerializeWriterException {    
    	logger.entering(loggerName, "write: edition pk=" + pk.getIdenticator());
        NCMEditionValueClient edtVC = getEditionValueClient(pk);
        XMLSerializeWriter w = new XMLSerializeWriter(out);
        w.writeObject(edtVC, getBuildProperties());
        w.close();
        logger.exiting(loggerName, "write");    	
    }
    
    public NCMEditionValueClient getEditionValueClient(NCMEditionIdentificator pk) {
    	return (NCMEditionValueClient) ds.getNode(pk, getBuildProperties());
    }
}
