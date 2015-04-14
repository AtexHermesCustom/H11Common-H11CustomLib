package com.atex.h11.custom.common;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
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
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;

public class HermesObject {
	
    private static final String loggerName = HermesObject.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);		

    protected DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    protected DocumentBuilder docBuilder = null;
    protected NCMDataSource ds = null;
    protected NCMObjectBuildProperties buildProps = null;
   
    protected DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		// default date format
    protected String convertFormat = null;   	// default = no converter
    
    public HermesObject (NCMDataSource ds) throws ParserConfigurationException {
        this.ds = ds;
        this.docBuilder = docBuilderFactory.newDocumentBuilder();
    }
    
    public void setDateFormat(DateFormat dateFormat) {
    	this.dateFormat = dateFormat;
    }
    
    public void setConvertFormat(String convertFormat) {
    	this.convertFormat = convertFormat;
    }
    
    public void setBuildProperties(NCMObjectBuildProperties buildProps) {
    	this.buildProps = buildProps;
    }
    
    public NCMObjectBuildProperties getBuildProperties() {
    	if (buildProps == null) {
    		// default settings
            buildProps = new NCMObjectBuildProperties();
            buildProps.setDateFormat(dateFormat);
            buildProps.setIncludeObjContent(true);
            buildProps.setIncludeLay(true);
            buildProps.setIncludeLayContent(true);
            buildProps.setIncludeLayObjContent(true);
            buildProps.setIncludeAttachments(true);
            buildProps.setIncludeCaption(true);
            buildProps.setIncludeCreditBox(true);
            buildProps.setIncludeIPTC(true);
            buildProps.setIncludeTextPreview(true);
            buildProps.setIncludeLinkedObject(true);
            buildProps.setIncludeVariants(true);
            buildProps.setIncludeSpChild(true);
            buildProps.setXhtmlNestedAsXml(true);
            buildProps.setNeutralNestedAsXml(true);
            buildProps.setIncludeMetadataChild(true);
            buildProps.setIncludeMetadataGroups(new Vector<String>());
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
    
    public void export (NCMObjectPK objPK, File file)
            throws UnsupportedEncodingException, IOException,
                   XMLSerializeWriterException {
        write(objPK, new FileOutputStream(file));
    }
    
    public Document getDocument (NCMObjectPK objPK) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(128*1024);
        write(objPK, out);
        byte[] bytes = out.toByteArray();
        out.close();
        return docBuilder.parse(new ByteArrayInputStream(bytes));
    }
        
    public void write (NCMObjectPK objPK, OutputStream out) 
            throws UnsupportedEncodingException, IOException, 
                   XMLSerializeWriterException { 
    	logger.entering(loggerName, "write: object pk=" + objPK.getObjId());    	    	
        NCMObjectValueClient objVC = (NCMObjectValueClient) ds.getNode(objPK, getBuildProperties());
        XMLSerializeWriter w = new XMLSerializeWriter(out);
        w.writeObject(objVC, getBuildProperties());
        w.close();
        logger.exiting(loggerName, "write");
    }

}
