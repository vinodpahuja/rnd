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

package jcommontk.object;

/**
 * Utility for converting type to type.
 */

@SuppressWarnings("unchecked")
public class ObjectConverter
  {
    /**
     * Converts value to an instance of class parameterClass.
     *
     * @param parameterClass class to convert to
     * @param value value to convert
     * @return a new instance of parameterClass with the given value
     */
  
    public static <T> T convertObject(Class<T> parameterClass, Object value)
      {
        Object newValue = value;

        if (value != null)
          {
            if (!parameterClass.isAssignableFrom(value.getClass()))
              if (parameterClass.isAssignableFrom(String.class))
                newValue = value.toString();
              else if (parameterClass.isAssignableFrom(int.class) || parameterClass.isAssignableFrom(Integer.class))
                newValue = value instanceof Number ? new Integer(((Number)value).intValue()) : new Integer(value.toString());
              else if (parameterClass.isAssignableFrom(short.class) || parameterClass.isAssignableFrom(Short.class))
                newValue = value instanceof Number ? new Short(((Number)value).shortValue()) : new Short(value.toString());
              else if (parameterClass.isAssignableFrom(long.class) || parameterClass.isAssignableFrom(Long.class))
                newValue = value instanceof Number ? new Long(((Number)value).longValue()) : new Long(value.toString());
              else if (parameterClass.isAssignableFrom(float.class) || parameterClass.isAssignableFrom(Float.class))
                newValue = value instanceof Number ? new Float(((Number)value).floatValue()) : new Float(value.toString());
              else if (parameterClass.isAssignableFrom(double.class) || parameterClass.isAssignableFrom(Double.class))
                newValue = value instanceof Number ? new Double(((Number)value).doubleValue()) : new Double(value.toString());
              else if (parameterClass.isAssignableFrom(boolean.class) || parameterClass.isAssignableFrom(Boolean.class))
                {
                  String str = value.toString().toLowerCase();

                  newValue = str.equals("on") || str.equals("true") || str.equals("yes") || str.equals("1") ? Boolean.TRUE : Boolean.FALSE;
                }
              else if (parameterClass.isAssignableFrom(byte.class) || parameterClass.isAssignableFrom(Byte.class))
                newValue = new Byte(value.toString());
              else if ((parameterClass.isAssignableFrom(char.class) || parameterClass.isAssignableFrom(Character.class)) && value.toString().length() > 0)
                newValue = new Character(value.toString().charAt(0));
          }
        else if (parameterClass.isPrimitive())
          {
            if (parameterClass.isAssignableFrom(int.class))
              newValue = new Integer(0);
            else if (parameterClass.isAssignableFrom(short.class))
              newValue = new Short((short)0);
            else if (parameterClass.isAssignableFrom(long.class))
              newValue = new Long(0);
            else if (parameterClass.isAssignableFrom(float.class))
              newValue = new Float(0);
            else if (parameterClass.isAssignableFrom(double.class))
              newValue = new Double(0);
            else if (parameterClass.isAssignableFrom(boolean.class))
              newValue = Boolean.FALSE;
            else if (parameterClass.isAssignableFrom(byte.class))
              newValue = new Byte((byte)0);
            else if (parameterClass.isAssignableFrom(char.class))
              newValue = new Character((char)0);
          }
        
        return (T)newValue;
      }
  }
