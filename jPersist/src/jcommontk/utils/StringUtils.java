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

public class StringUtils
  {
    public static boolean isEmptyOrNull(String str) { return str == null || str.length() == 0; }
    
    public static String emptyToNull(String val)
      {
        return emptyToDefault(val, null);
      }
    
    public static String nullToEmpty(String val)
      {
        return emptyToDefault(val, "");
      }
    
    public static String emptyToDefault(String val, String defaultValue)
      {
        if (val == null || val.length() == 0)
          return defaultValue;
        
        return val;
      }
    
    public static String lowerCaseUnderlineToCamelCase(String name)
      {
        StringBuffer newName = new StringBuffer();
        boolean nextIsUpper = false;
        
        name = name.toLowerCase();
        
        for (int i = 0; i < name.length(); i++)
          {
            if ((name.charAt(i)) == '_')
              nextIsUpper = true;
            else
              {
                newName.append(nextIsUpper ? Character.toUpperCase(name.charAt(i)) : name.charAt(i));
                
                nextIsUpper = false;
              }
          }
        
        return newName.toString();
      }
    
    public static String camelCaseToLowerCaseUnderline(String name)
      {
        StringBuffer newName = new StringBuffer();

        for (int i = 0; i < name.length(); i++)
          if (Character.isUpperCase(name.charAt(i)))
            {
              if (i == 0)
                newName.append(Character.toLowerCase(name.charAt(i)));
              else
                newName.append("_").append(Character.toLowerCase(name.charAt(i)));
            }
          else
            newName.append(name.charAt(i));

        return newName.toString();
      }
    
    public static String upperCaseUnderlineToCamelCase(String name)
      {
        return lowerCaseUnderlineToCamelCase(name);
      }
    
    public static String camelCaseToUpperCaseUnderline(String name)
      {
        StringBuffer newName = new StringBuffer();

        for (int i = 0; i < name.length(); i++)
          if (Character.isUpperCase(name.charAt(i)))
            {
              if (i == 0)
                newName.append(Character.toUpperCase(name.charAt(i)));
              else
                newName.append("_").append(Character.toUpperCase(name.charAt(i)));
            }
          else
            newName.append(Character.toUpperCase(name.charAt(i)));

        return newName.toString();
      }
    
    public static String toString(Object[] objects)
      {
        String str = "";
        
        if (objects != null)
          for (int i = 0; i < objects.length; i++)
            str += "[" + objects[i] + "]" + (i < objects.length - 1 ? ", " : "");
        
        return str;
      }
  }

