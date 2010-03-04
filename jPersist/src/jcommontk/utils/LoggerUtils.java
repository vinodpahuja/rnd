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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LoggerUtils
  {
    public static class OutputStreamHandler extends Handler
      {
        OutputStream out;
        
        public OutputStreamHandler(OutputStream out, boolean showSourceInfo, boolean indent)
          {
             this.out = out;
             setFormatter(new SimpleFormatter(showSourceInfo, indent));
          }
         
        public void flush() { try { out.flush(); } catch(Exception e) { e.printStackTrace(); } }
        
        public void close() { }
        
        public void publish(LogRecord record)
          {
            if (isLoggable(record))
              try
                {
                  out.write(getFormatter().format(record).getBytes());
                }
              catch (Exception e)
                {
                  e.printStackTrace(System.err);
                }
          }
      }
    
    public static class SimpleFormatter extends Formatter
      {
        Date d = new Date();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
        boolean showSourceInfo, indent;
        
        public SimpleFormatter(boolean showSourceInfo, boolean indent)
          {
            this.showSourceInfo = showSourceInfo;
            this.indent = indent;
          }
        
      	public String format(LogRecord record)
          {
            d.setTime(record.getMillis());

            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            
            out.print(df.format(d) + " " + record.getLevel() + " - ");
            
            if (showSourceInfo)
              out.println(record.getSourceClassName() + "::" + record.getSourceMethodName());
            
            if (indent)
              {
                StringTokenizer strtok = new StringTokenizer(record.getMessage(), "\n");

                while (strtok.hasMoreTokens())
                  out.println("      " + strtok.nextToken());
              }
            else out.println(record.getMessage());
            
            Object[] params = record.getParameters();
            
            if (params != null)
              out.println("Parameters: " + StringUtils.toString(params));
            
            Throwable t = record.getThrown();

            if (t != null)
              t.printStackTrace(out);
            
            return sw.toString();
          }
      }
  }
