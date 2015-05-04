package com.atex.h11.custom.common;

import com.unisys.media.extension.common.constants.ApplicationConstants;
import com.unisys.media.extension.common.security.UPSUser;
import com.unisys.media.cr.model.data.datasource.IDataSourceClient;
import com.unisys.media.cr.model.data.datasource.DataSourceManager;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.Properties;
import javax.naming.Context;

public class DataSource {
    
    private static final String loggerName = DataSource.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);	
	
    private static final String JNDI_PROPS = "jndi.properties";
    private static final String JNP_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String JNP_PKGS = "org.jnp.interfaces";

    //private static final String DATASOURCE = "ejb/103/cr/ncm/default/DataSourceManager";
    
	private static final String DEFAULT_HOST = "atexhost";
	private static final String DEFAULT_NAMINGPORT = "1099";
	
    /**
     * Default constructor.
     */
    public DataSource() {}
    
    public static synchronized IDataSourceClient newInstance (String sessionId)
        throws FileNotFoundException, IOException {
        return newInstance("SESSIONID" + sessionId, sessionId);
    }
    
    public static synchronized NCMDataSource newInstance (String user, String passwd)
        throws FileNotFoundException, IOException {
        
    	logger.entering(loggerName, "newInstance: user=" + user);
        DataSourceManager dsmgr = null;

        try {
            // Get a DataSourceManager instance while running in app server.
            // dsmgr = DataSourceManager.getInstance();
            dsmgr = DataSourceManager.getDefaultContextInstance();
            logger.finer("used default context instance");
        } catch (Exception e) {
            // Either read the naming properties from a file or set them based 
            // on environment variables.
            Properties jndiProps = new Properties();
            String jndiPropsName = System.getProperty(JNDI_PROPS);
            if (jndiPropsName != null) {
            	// load from jndi props file
                jndiProps.load(new FileInputStream(jndiPropsName));
            } else {
                try {
                    // try to load them from a file in the working directory
                    jndiProps.load(new FileInputStream(JNDI_PROPS));
                } catch (FileNotFoundException fnfe) {
                    // prepare the props based on defaults and the environment
                    jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, JNP_FACTORY);
                    jndiProps.put(Context.URL_PKG_PREFIXES, JNP_PKGS);
                    String host = null;
                    if ((host = System.getenv("J2EE_IP")) == null)
                        host = DEFAULT_HOST;			// set to default host
                    String port = null;
                    if ((port = System.getenv("J2EE_NAMINGPORT")) == null)
                        port = DEFAULT_NAMINGPORT;      // set to default port
                    String providerUrlStr = "jnp://" + host + ":" + port;
                    jndiProps.put(Context.PROVIDER_URL, providerUrlStr);
                }
            }

            // Get a DataSourceManager instance.
            dsmgr = DataSourceManager.getInstance(jndiProps);
            logger.finer("used jndi props");
            Enumeration<?> i = jndiProps.propertyNames();
            while (i.hasMoreElements()) {
            	String key = (String) i.nextElement();
            	logger.finer(key + "=" + jndiProps.getProperty(key));
            }            
        }
        
        // Login
		UPSUser upsUser = 
			UPSUser.instanceUPSUserForNamedUser(user, passwd, ApplicationConstants.APP_MEDIA_API_ID);
		NCMDataSource ds = 
			(NCMDataSource) dsmgr.getDataSource(NCMDataSource.DS_PK, upsUser, ApplicationConstants.APP_MEDIA_API_ID);

		logger.exiting(loggerName, "newInstance");
		return ds;
    }
}