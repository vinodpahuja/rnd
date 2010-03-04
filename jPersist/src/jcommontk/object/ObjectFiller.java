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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import jcommontk.utils.StringUtils;

/**
 * Object filler will fill an objects public members with values from a map.
 */

@SuppressWarnings("unchecked")
public class ObjectFiller
  {
  /**
   * Fill the object with values from the given getHandler.
   * 
   * @param getHandler the GetHandler providing values for the object being filled
   * @param object the object being filled
   *
   * @return the object passed in
   */
  
    public static <T> T fillObject(GetHandler getHandler, T object) throws IllegalAccessException, InvocationTargetException
      {
        return fillObject(getHandler, object, null, true, false, null, false);
      }
    
  /**
   * Fill the object with values from the given getHandler.
   * 
   * @param getHandler the GetHandler providing values for the object being filled
   * @param object the object being filled
   * @param ignoreValueNames ignore the value names in the set
   * @param setNulls set or ignore null values
   *
   * @return the object passed in
   */
  
    public static <T> T fillObject(GetHandler getHandler, T object, Set<String> ignoreValueNames, boolean setNulls) throws IllegalAccessException, InvocationTargetException
      {
        return fillObject(getHandler, object, null, true, false, ignoreValueNames, setNulls);
      }
    
  /**
   * Fill the object with values from the given getHandler.
   * 
   * @param getHandler the GetHandler providing values for the object being filled
   * @param object the object being filled
   * @param fillPublicSetMethods fill public set methods of the object
   * @param fillPublicFields fill public fields of the object
   * @param ignoreValueNames ignore the value names in the set
   * @param setNulls set or ignore null values
   *
   * @return the object passed in
   */
  
    public static <T> T fillObject(GetHandler getHandler, T object, boolean fillPublicSetMethods, boolean fillPublicFields, Set<String> ignoreValueNames, boolean setNulls) throws IllegalAccessException, InvocationTargetException
      {
        return fillObject(getHandler, object, null, fillPublicSetMethods, fillPublicFields, ignoreValueNames, setNulls);
      }

  /**
   * Fill the object with values from the given getHandler.
   * 
   * @param getHandler the GetHandler providing values for the object being filled
   * @param object the object being filled
   * @param c the declaring class that methods and fields must exist in or null for no restriction
   * @param fillPublicSetMethods fill public set methods of the object
   * @param fillPublicFields fill public fields of the object
   * @param ignoreValueNames ignore the value names in the set
   * @param setNulls set or ignore null values
   *
   * @return the object passed in
   */
  
    public static <T> T fillObject(GetHandler getHandler, T object, Class<T> c, boolean fillPublicSetMethods, boolean fillPublicFields, Set<String> ignoreValueNames, boolean setNulls) throws IllegalAccessException, InvocationTargetException
      {
        Set memberSet = new HashSet();
        
        if (fillPublicSetMethods)
          {
            Method[] methods = c != null ? c.getMethods() : object.getClass().getMethods();
            
            for (int i = 0; i < methods.length; i++)
              {
                Method method = methods[i];
                String methodName = method.getName();

                if (c == null || methods[i].getDeclaringClass().equals(c))
                  if (methodName.startsWith("set") && !methodName.equalsIgnoreCase("set"))
                    {
                      String valueName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);

                      if (!memberSet.contains(valueName))
                        {
                          memberSet.add(valueName);

                          if (method.getParameterTypes().length > 0 && (ignoreValueNames == null || !ignoreValueNames.contains(valueName)))
                            {
                              try
                                {
                                  Object value = getHandler.get(valueName, method.getParameterTypes()[0]);

                                  if (value == null)
                                    value = getHandler.get(StringUtils.camelCaseToLowerCaseUnderline(valueName), method.getParameterTypes()[0]);

                                  if (value == null)
                                    value = getHandler.get(valueName.toLowerCase(), method.getParameterTypes()[0]);

                                  if (setNulls || value != null)
                                    if (method.getParameterTypes() != null && method.getParameterTypes().length == 1)
                                      method.invoke(object, new Object[] { ObjectConverter.convertObject(method.getParameterTypes()[0], value) });
                                }
                              catch (ItemNotFoundException e) { }
                            }
                        }
                    }
              }
          }
        
        if (fillPublicFields)
          {
            Field[] fields = c != null ? c.getFields() : object.getClass().getFields();
            
            for (int i = 0; i < fields.length; i++)
              {
                String fieldName = fields[i].getName();
                
                if (c == null || fields[i].getDeclaringClass().equals(c))
                  if (!memberSet.contains(fieldName))
                    {
                      memberSet.add(fieldName);

                      if (ignoreValueNames == null || !ignoreValueNames.contains(fieldName))
                        {
                          try
                            {
                              Object value = getHandler.get(fieldName, fields[i].getType());

                              if (value == null)
                                value = getHandler.get(fieldName.toLowerCase(), fields[i].getType());

                              if (value == null)
                                value = getHandler.get(StringUtils.camelCaseToLowerCaseUnderline(fieldName), fields[i].getType());

                              if (setNulls || value != null)
                                fields[i].set(object, new Object[] { ObjectConverter.convertObject(fields[i].getType(), value) });
                            }
                          catch (ItemNotFoundException e) { }
                        }
                    }
              }
          }
        
        return object;
      }

    /**
     * Implement this interface for ObjectFiller to gain access to your data.
     * If your object has a field that doesn't match up to data, 
     * throw ItemNotFoundException instead of returning null so ObjectFiller 
     * doesn't set the field to null.
     */
    public interface GetHandler
      {
        public <T> T get(String key, Class<T> objectType) throws ItemNotFoundException;
      }
    
    public static class ItemNotFoundException extends Exception
      {
        private static final long serialVersionUID = 100L;
      }
  }

