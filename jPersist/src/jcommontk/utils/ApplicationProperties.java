/**
 * Copyright (C) 2006 - present Software Sensation Inc.  
 * All Rights Reserved.
 *
 * This file is part of jCommonTk.
 *
 * jCommonTk is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the accompanying license 
 * for more details.
 *
 * You should have received a copy of the license along with jCommonTk; if not, 
 * go to http://www.softwaresensation.com and download the latest version.
 */

package jcommontk.utils;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Properties;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains properties from the application.properties file along with system properties.  
 * These properties are passed to the server and can be obtained within the 
 * install&amp;update script from $script.getClientInfo().
 */
public class ApplicationProperties
  {
    private static Logger logger = Logger.getLogger(ApplicationProperties.class.getName());
    
    static Properties props;
    static URL url;
    
    private ApplicationProperties() { }
    
    public static synchronized Properties getProperties ()
      {
        if (props == null)
          props = loadProperties();
        
        return props; 
      }
    
    public static void setPropertiesFile(URL url)
      {
        ApplicationProperties.url = url;
      }
    
    public static Properties loadProperties () 
      {
        try
          {
            if (url == null)
              url = ClassLoader.getSystemResource("application.properties");

            if (url == null)
              throw new FileNotFoundException("application.properties");
            
            props = new Properties();
            props.load(url.openStream());

            Enumeration e = System.getProperties().propertyNames();
            String key;

            while (e.hasMoreElements())
              {
                key = (String)e.nextElement();
                props.setProperty(key, System.getProperty(key));
              }
            
            return props; 
          }
        catch (Exception e)
          {
            logger.log(Level.SEVERE, e.toString(), e);
          }
        
        return null;
      }
  }

