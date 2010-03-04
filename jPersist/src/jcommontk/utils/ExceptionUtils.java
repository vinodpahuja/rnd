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

import java.lang.reflect.InvocationTargetException;

public class ExceptionUtils
  {
    public static String normalizeMessageChain(Exception e)
      {
        return normalizeMessageChain(e, false);
      }
    
    public static String normalizeMessageChain(Exception e, boolean useHtmlBreak)
      {
        String message = "";
        Throwable t = e;

        do
          {
            if (message.length() > 0)
              {
                message += useHtmlBreak ? "<br>" : "\n";
                message += "Caused by: " + (useHtmlBreak ? "<br>" : "\n");
                message += "     " + t;
              }
            else message += t;
          }
        while ((t = t.getCause()) != null);

        if (useHtmlBreak)
          message = message.replaceAll("\n","<br>");
        
        return message;
      }

    public static Throwable getRootCause(Throwable t_in)
      {
        Throwable t_out = t_in;

        while (t_out.getCause() != null)
          t_out = t_out.getCause();

        return t_out;
      }
    
    public static String getRootMessage(Throwable t)
      {
        return getRootCause(t).toString();
      }

    public static String getRootLocalizedMessage(Throwable t)
      {
        return getRootCause(t).toString();
      }

    public static Throwable getNormalizedCause(Throwable t)
      {
        if (t.getCause() != null)
          return t.getCause();

        return t;
      }
    
    public static String getNormalizedMessage(Throwable t)
      {
        if (t instanceof IllegalAccessException)
          return "IllegalAccessException: Can't access member due to visibility (non-public)";
        else if (t instanceof InvocationTargetException)
          return getNormalizedCause(t).getMessage();
        else if (t instanceof InstantiationException)
          return "InstantiationException: Can't create new instance because of visibility (not public), or there's no suitable\n" +
                  "constructor (might need an empty constructor), or it's an interface, or it's an abstract class";
        
        return t.getMessage();
      }
    
    public static class JCommonException extends Exception
      {
        private static final long serialVersionUID = 100L;
        
        public JCommonException(Throwable cause) 
          {
            super(getNormalizedMessage(cause), cause);

            if (cause instanceof RuntimeException)
              throw (RuntimeException)cause;
          }

        public JCommonException(String message) { super(message); }
        
        public JCommonException(String message, Throwable cause) 
          {
            super(getNormalizedMessage(cause), cause); 

            if (cause instanceof RuntimeException)
              throw (RuntimeException)cause;
          }
      }
  }
