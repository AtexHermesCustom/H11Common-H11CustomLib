package com.atex.h11.custom.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriter;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.types.NCMObjectNodeType;

public class StoryPackage extends HermesObject {
	
    private static final String loggerName = StoryPackage.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);		
  
    public StoryPackage (NCMDataSource ds) throws ParserConfigurationException {
    	super(ds);
    }
    
    @Override
    public void write (NCMObjectPK objPK, OutputStream out) 
    		throws UnsupportedEncodingException, IOException, XMLSerializeWriterException { 
    	logger.entering(loggerName, "write: object pk=" + objPK.getObjId());    	    	
    	NCMObjectValueClient objVC = (NCMObjectValueClient) ds.getNode(objPK, getBuildProperties());
        if (objVC.getType() != NCMObjectNodeType.OBJ_STORY_PACKAGE)
            throw new IllegalArgumentException("Provided object is not a story package!");    	
    	XMLSerializeWriter w = new XMLSerializeWriter(out);
    	w.writeObject(objVC, getBuildProperties());
    	w.close();
    	logger.exiting(loggerName, "write");
    }    
}
