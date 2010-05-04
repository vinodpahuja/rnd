/**
 * Copyright (C) 2006 - present Software Sensation Inc.  
 * All Rights Reserved.
 *
 * This file is part of jPersist.
 *
 * jPersist is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the accompanying license 
 * for more details.
 *
 * You should have received a copy of the license along with jPersist; if not, 
 * go to http://www.softwaresensation.com and download the latest version.
 */

package jpersist;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcommontk.object.ObjectConverter;
import jcommontk.utils.StringUtils;
import jpersist.interfaces.ResultObject;

@SuppressWarnings("unchecked")
class ResultProxy implements InvocationHandler
  {
    private static Logger logger = Logger.getLogger(ResultProxy.class.getName());
    
    Class objectClass;
    Result result;
    Object[] handlers;

    ResultProxy(Result result, Class objectClass, Object[] handlers) throws JPersistException, SQLException
      {
        this.result = result;
        this.objectClass = objectClass;
        this.handlers = handlers;
      }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
        String name = method.getName();
        Object returnValue = null;

        try
          {
            if (method.getDeclaringClass().equals(ResultObject.class) || method.getDeclaringClass().equals(ListIterator.class))
              {
                if (name.equals("next"))
                  {
                    result.next();

                    returnValue = proxy;
                  }
                else if (name.equals("previous"))
                  {
                    result.previous();

                    returnValue = proxy;
                  }
                else
                  returnValue = result.getClass().getDeclaredMethod(method.getName(),method.getParameterTypes()).invoke(result,args);
              }
            else
              {
                Object object;

                if (handlers != null && handlers.length > 0 && (object = checkHandlers(method,args)) != null)
                  return object;
                else if (name.startsWith("get"))
                  return getDBValue(method, name.substring(3));
                else if (name.startsWith("set"))
                  return setDBValue(name.substring(3), args[0]);
              }
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        
        return returnValue;
      }

    Object checkHandlers(Method m, Object[] args) throws IllegalAccessException, InvocationTargetException
      {
        Class classes[] = null;
        Object object = null;
        
        if (args != null && args.length > 0)
          {
            classes = new Class[args.length];

            for (int i = 0; i < classes.length; i++)
              classes[i] = args[i].getClass();
          }
        
        for (int i = 0; i < handlers.length; i++)
          {
            try
              {
                Method method = handlers[i].getClass().getMethod(m.getName(),classes);

                if (method != null)
                  object = method.invoke(handlers[i],args);
              }
            catch (NoSuchMethodException e)
              {
                if (logger.isLoggable(Level.FINEST))
                  logger.log(Level.FINEST,e.getMessage(),e);
              }
          }
        
        return object;
      }
    
    Object getDBValue(Method method, String name) throws JPersistException, SQLException
      {
        Object obj;
        String newName = null;

        try
          {
            obj = result.getColumnValue(method.getReturnType(), getColumnName(name));
          }
        catch (Exception e)
          {
            MetaData metaData = result.getDatabase().getMetaData();
            
            try
              {
                newName = metaData.getStoresCase() == MetaData.STORES_UNKNOWN || metaData.getStoresCase() == MetaData.STORES_LOWERCASE 
                     ? name.toUpperCase() : name.toLowerCase();
              
                obj = result.getColumnValue(method.getReturnType(), newName);
              }
            catch (Exception e2)
              {
                newName = metaData.getStoresCase() == MetaData.STORES_UNKNOWN || metaData.getStoresCase() == MetaData.STORES_LOWERCASE 
                     ? StringUtils.camelCaseToUpperCaseUnderline(name)
                     : StringUtils.camelCaseToLowerCaseUnderline(name);
                obj = result.getColumnValue(method.getReturnType(), newName);
              }
          }

        return ObjectConverter.convertObject(method.getReturnType(), obj);
      }

    Object setDBValue(String name, Object value) throws JPersistException, SQLException
      {
        result.setColumnValue(getColumnName(name), value);
        
        return null;
      }
    
    String getColumnName(String name)
      {
        try
          {
            Database db = result.getDatabase();
            MetaData.Table table = db.getMetaData().getTable(db.getConnection(), db.getTableMapper(), db.getColumnMapper(), db.getCatalogPattern(), db.getSchemaPattern(), ObjectSupport.getTableName(objectClass), null);
          
            if (table != null)
              {
                MetaData.Table.Column column = table.getColumn(db.getColumnMapper(), name, null);

                if (column != null)
                  return column.getColumnName();
              }
            
          }
        catch (RuntimeException e)
          {
            throw e;
          }
        catch (Exception e)
          {
            logger.log(Level.SEVERE,e.getMessage(),e);
          }
        
        return objectClass.getName();
      }
  }

