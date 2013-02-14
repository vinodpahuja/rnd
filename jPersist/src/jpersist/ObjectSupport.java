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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import jcommontk.object.ObjectConverter;
import jcommontk.object.ObjectFiller;
import jcommontk.object.ObjectFiller.GetHandler;
import jcommontk.object.ObjectFiller.ItemNotFoundException;
import jcommontk.utils.StringUtils;
import jpersist.annotations.ConcreteTableInheritance;
import jpersist.annotations.GlobalDelete;
import jpersist.annotations.GlobalUpdate;
import jpersist.annotations.SingleTableInheritance;
import jpersist.annotations.UpdateNullValues;
import jpersist.interfaces.ColumnMapping;
import jpersist.interfaces.GeneratedKeys;

// TODO check parent id for null value
@SuppressWarnings("unchecked")
final class ObjectSupport
  {
    private static Logger logger = Logger.getLogger(ObjectSupport.class.getName());
    
    static Result queryObject(Database db, Class cs, Object object, Set nullValuesToInclude, boolean idColumnsOnly, String externalClauses, Object[] parameters) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Querying object: class " + cs.getName() 
                       + "\nexternalClauses = " + externalClauses
                       + "\nparameters[] = " + StringUtils.toString(parameters)
                       + "IdColumnsOnly = " + idColumnsOnly);
        
        if (cs == null && object == null)
          throw new JPersistException("object is null");

        StringBuffer sqlStatement = new StringBuffer(),
                     externalClausesStrBuf = null;
        List values = new ArrayList();
        
        if (externalClauses == null || !externalClauses.startsWith("select"))
          {
            StringBuffer columnsStrBuf = new StringBuffer(), fromStrBuf = new StringBuffer(), whereStrBuf = new StringBuffer();

            if (externalClauses != null)
              externalClausesStrBuf = new StringBuffer(externalClauses);
            
            processClasses(db, cs, object, true, idColumnsOnly, false, false, nullValuesToInclude, 
                           new QueryObjectHandler(db, fromStrBuf, columnsStrBuf, whereStrBuf, externalClausesStrBuf, values));
              
            sqlStatement.append("select ").append(columnsStrBuf);

            if (externalClauses == null || !externalClauses.startsWith("from"))
              {
                sqlStatement.append(" from ").append(fromStrBuf);

                if (whereStrBuf.length() > 0 || (externalClauses == null || !externalClauses.startsWith("where"))) {
                      sqlStatement.append(" where ").append(whereStrBuf);
                  }
                else 
                	if (idColumnsOnly)
                  throw new JPersistException("useIdColumnsOnly is defined, but there are no Id field values available");
              }
          }

        if (externalClauses != null)
          {
            sqlStatement.append(" " + externalClausesStrBuf);

            if (parameters != null)
              for (int i = 0; i < parameters.length; i++)
                values.add(parameters[i]);
          }

        if (values.size() > 0)
          return db.parameterizedQuery(sqlStatement.toString(), values.toArray()).setClass(cs);
        else
          return db.executeQuery(sqlStatement.toString()).setClass(cs);
      }

    static class QueryObjectHandler implements ClassHandler
      {
        Database db;
        StringBuffer fromStrBuf, columnsStrBuf, whereStrBuf, externalClausesStrBuf;
        String identifierQuoteString;
        Set selectSet = new HashSet(), whereSet = new HashSet();
        Map lastTable = new HashMap();
        List values;
        
        public QueryObjectHandler(Database db, StringBuffer fromStrBuf, StringBuffer columnsStrBuf, StringBuffer whereStrBuf, StringBuffer externalClausesStrBuf, List values) throws JPersistException
          {
            this.db = db;
            this.fromStrBuf = fromStrBuf;
            this.columnsStrBuf = columnsStrBuf;
            this.whereStrBuf = whereStrBuf;
            this.externalClausesStrBuf = externalClausesStrBuf;
            this.values = values;
            
            this.identifierQuoteString = db.getMetaData().getIdentifierQuoteString();
          }
        
        public void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
          {
            fromStrBuf.append((fromStrBuf.length() > 0 ? ", " : "") + identifierQuoteString + table.getTableName() + identifierQuoteString);

            if (numberTables > 1)
              fromStrBuf.append(" " + tableAlias);

            if (externalClausesStrBuf != null)
              processExternalClauses(externalClausesStrBuf, table, db.getColumnMapper(), object, identifierQuoteString);

            processSelect(selectableColumns, table, numberTables, tableAlias);
            processWhere(valuesMap, table, numberTables, tableAlias);
            processJoin(objectClass, table, numberTables, tableAlias);
          }
        
        void processSelect(Set selectableColumns, MetaData.Table table, int numberTables, char tableAlias)
          {
            for (Iterator it = selectableColumns.iterator(); it.hasNext();)
              {
                MetaData.Table.Column column = (MetaData.Table.Column)it.next();
                //MetaData.Table.Key key = (MetaData.Table.Key)table.getImportedKeys().get(column.getColumnName());

                if (!selectSet.contains(column.getColumnName()))// && (key == null || !selectSet.contains(key.getForeignColumnName())))
                  {
                    selectSet.add(column.getColumnName());

                    if (columnsStrBuf.length() > 0)
                      columnsStrBuf.append(", ");

                    if (numberTables > 1)
                      columnsStrBuf.append(tableAlias + ".");

                    columnsStrBuf.append(identifierQuoteString + column.getColumnName() + identifierQuoteString);
                  }
              }
          }
        
        void processWhere(Map valuesMap, MetaData.Table table, int numberTables, char tableAlias)
          {
            for (Iterator it = valuesMap.entrySet().iterator(); it.hasNext(); )
              {
                Map.Entry entry = (Map.Entry)it.next();
                MetaData.Table.Column column = (MetaData.Table.Column)entry.getKey();
                MetaData.Table.Key key = (MetaData.Table.Key)table.getImportedKeys().get(column.getColumnName());

                if (!whereSet.contains(column.getColumnName()) && column.isSearchable() && (key == null || !whereSet.contains(key.getForeignColumnName())))
                  {
                    whereSet.add(column.getColumnName());

                    if (whereStrBuf.length() > 0)
                      whereStrBuf.append(" and ");

                    if (numberTables > 1)
                      whereStrBuf.append(tableAlias + ".");

                    Object obj = entry.getValue();

                    if (obj instanceof NullValue)
                      whereStrBuf.append(identifierQuoteString + column.getColumnName() + identifierQuoteString +  " is null");
                    else
                      {
                        values.add(obj);

                        whereStrBuf.append(identifierQuoteString + column.getColumnName() + identifierQuoteString + (hasWildCards(obj.toString()) ? " like ?" : " = ?"));
                      }
                  }
              }
          }
        
        void processJoin(Class cs, MetaData.Table table, int numberTables, char tableAlias) throws JPersistException
          {
            if (numberTables > 1)
              {
                MetaData.Table last = (MetaData.Table)lastTable.get("table");

                if (last != null)
                  {
                    Set keys = getMatchingImportedExportedKeys(last.getExportedKeys(), table.getImportedKeys());

                    if (keys.size() == 0)
                      throw new JPersistException("The inheritance relation represented by " 
                                                  + cs.getName() 
                                                  + " does not have primary/foreign key relationships defined for the underlying"
                                                  + " tables (must have a FOREIGN KEY ... REFERENCES ... clause in table creation, see alter table)");

                    for (Iterator it2 = keys.iterator(); it2.hasNext();)
                      {
                        MetaData.Table.Key key = (MetaData.Table.Key)it2.next();

                        whereStrBuf.append((whereStrBuf.length() > 0 ? " and " : "") 
                                          + (char)(tableAlias - 1) + "." + identifierQuoteString + key.getForeignColumnName() + identifierQuoteString
                                          + " = " + tableAlias + "." + identifierQuoteString + key.getLocalColumnName() + identifierQuoteString);
                      } 
                  }

                lastTable.put("table", table);
              }
          }
      }
    
    static Object loadObject(Result result, Object object, boolean loadAssociations) throws JPersistException, SQLException, IllegalAccessException, InstantiationException, InvocationTargetException
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Loading object of class " + object.getClass().getName());
        
        if (object == null)
          throw new JPersistException("data object is null");
        
        if (object instanceof PersistentObject)
          ((PersistentObject)object).makeObjectTransient();
        
        LoadClassHandler classHandler = new LoadClassHandler(result, object);
        
        processClasses(result.getDatabase(), object.getClass(), object, false, false, false, false, null, classHandler);

        if (object instanceof PersistentObject && classHandler.getHasTableInformation())
          ((PersistentObject)object).setObjectChecksum(calculateChecksum(result.getDatabase(), object));

        if (loadAssociations && (!(object instanceof PersistentObject) || !((PersistentObject)object).getIgnoreAssociations()))
          loadAssociations(result.getDatabase(), object);

        return object;
      }

    static class LoadClassHandler implements ClassHandler
      {
        Result result;
        Object object;
        boolean hasTableInformation;
        
        LoadClassHandler(Result result, Object object)
          {
            this.result = result;
            this.object = object;
          }
      
        public void processClass(Class objectClass, final Object object, final MetaData.Table table, int numberTables, final char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
          {
            if (objectClass.isAnnotationPresent(SingleTableInheritance.class) || objectClass.isAnnotationPresent(ConcreteTableInheritance.class))
              objectClass = null;

            if (table != null)
              {
                loadObjectsWithTableHelp(objectClass, object, table, tableAlias);
                hasTableInformation = true;
              }
            else
              loadObjectsWithoutTableHelp(objectClass, object);
          }
        
        void loadObjectsWithTableHelp(Class objectClass, final Object object, final MetaData.Table table, final char tableAlias) throws IllegalAccessException, InvocationTargetException, JPersistException
          {
            if (object instanceof PersistentObject)
              if (table.getPrimaryKeys().size() == 0)
                ((PersistentObject)object).setObjectPersistence(PersistentObject.MISSING_ROW_ID);

            ObjectFiller.fillObject(new GetHandler()
              {
                public Object get(String key, Class objectType) throws ItemNotFoundException
                  {
                    try 
                      {
                        MetaData.Table.Column column = table.getColumn(result.getDatabase().getColumnMapper(), key, object);

                        if (column != null)
                          {
                            String columnName = column.getColumnName();
                            Object value = null;

                            try
                              {
                                value = result.getColumnValue(objectType, columnName);
                              }
                            catch (Exception e)
                              {
                                value = result.getColumnValue(objectType, tableAlias + "." + columnName);
                              }

                            return value;
                          }
                      }
                    catch (Exception e) 
                      {
                        logger.log(Level.WARNING, e.toString(), e);
                      }

                    throw new ObjectFiller.ItemNotFoundException();
                  }
              }, object, objectClass, true, false, null, true);

            loadPrimaryKeys(table, tableAlias);
          }
        
        void loadPrimaryKeys(MetaData.Table table, char tableAlias) throws JPersistException
          {
            if (object instanceof PersistentObject)
              {
                Set ids = new HashSet(table.getPrimaryKeys().keySet());

                for (Iterator it = ids.iterator(); it.hasNext();)
                  {
                    String columnName = (String)it.next();
                    Object value = null;

                    try
                      {
                        value = result.getColumnValue(columnName);
                      }
                    catch (Exception e)
                      {
                        value = result.getColumnValue(tableAlias + "." + columnName);
                      }

                    ((PersistentObject)object).getObjectKeyValues().put(columnName, value);
                  }
              }
          }
        
        void loadObjectsWithoutTableHelp(Class objectClass, final Object object) throws IllegalAccessException, InvocationTargetException
          {
            if (logger.isLoggable(Level.FINER))
              logger.finer("table not found for class " + object.getClass().getName());

            if (object instanceof PersistentObject)
              ((PersistentObject)object).setObjectPersistence(PersistentObject.NO_TABLE_INFO);

            ObjectFiller.fillObject(new GetHandler() 
              {
                public Object get(String key, Class objectType)
                  {
                    MetaData metaData = null;
                    Object obj = null;
                    String name = null;

                    try
                      {
                        metaData = result.getDatabase().getMetaData();

                        name = key;
                        name = metaData.getStoresCase() == MetaData.STORES_UNKNOWN || metaData.getStoresCase() == MetaData.STORES_LOWERCASE 
                             ? name.toUpperCase() : name.toLowerCase();

                        obj = result.getColumnValue(objectType, name);
                      }
                    catch (Exception e)
                      {
                        try
                          {
                            name = key;
                            name = metaData.getStoresCase() == MetaData.STORES_UNKNOWN || metaData.getStoresCase() == MetaData.STORES_LOWERCASE 
                                 ? StringUtils.camelCaseToUpperCaseUnderline(name)
                                 : StringUtils.camelCaseToLowerCaseUnderline(name);

                            obj = result.getColumnValue(objectType, name);
                          }
                        catch (Exception e2)
                          {
                            logger.log(Level.WARNING, e2.getMessage(), e2);
                          }
                      }

                    return obj;
                  }
              }, object, objectClass, true, false, null, true);
          }
        
        public boolean getHasTableInformation() { return hasTableInformation; }
      }
    
    static void loadAssociations(Database db, Object object) throws JPersistException, SQLException, InstantiationException, IllegalAccessException, InvocationTargetException
      {
        Class objectClass = object.getClass();
        Method objectMethods[] = objectClass.getMethods();

        for (int i = 0; i < objectMethods.length; i++)
          {
            if (objectMethods[i].getName().equals("setDbAssociation"))
              {
                Class objectClass2 = objectMethods[i].getParameterTypes()[0], 
                      c2Type = objectClass2.isArray() ? objectClass2.getComponentType() : objectClass2, 
                      collectionType = null;
                
                if (!(object instanceof PersistentObject) || !((PersistentObject)object).classInIgnoreAssociation(c2Type))
                  {
                    if (objectMethods[i].getParameterTypes().length == 2)
                      collectionType = objectMethods[i].getParameterTypes()[1];

                    Object associationObject = c2Type.newInstance();

                    copyAssociationIds(db, object, associationObject);

                    Result result2 = db.queryObject(associationObject);

                    try
                      {
                        if (collectionType == null)
                          {
                            if (!objectClass2.isArray() && result2.hasNext())
                              {
                                Object obj = result2.next();

                                if (obj != null)
                                  objectMethods[i].invoke(object, new Object[] { obj });
                              }
                            else
                              {
                                List tmp = new ArrayList();

                                while (result2.hasNext())
                                  tmp.add(result2.next());

                                if (tmp.size() > 0)
                                  objectMethods[i].invoke(object, new Object[] { tmp.toArray((Object[])Array.newInstance(c2Type,tmp.size())) });
                              }
                          }
                        else
                          {
                            Collection collection = null;
                            
                            if (collectionType.isInterface())
                              {
                                if (collectionType.equals(List.class))
                                  collection = new ArrayList();
                                else if (collectionType.equals(Queue.class))
                                  collection = new LinkedList();
                                else if (collectionType.equals(Set.class))
                                  collection = new HashSet();
                                else if (collectionType.equals(SortedSet.class))
                                  collection = new TreeSet();
                                else throw new InstantiationException("Association collection must be a List, Queue, Set, SortedSet or a class that implements Collection.");
                              }
                            else collection = (Collection)collectionType.newInstance();

                            while (result2.hasNext())
                              collection.add(result2.next());

                            if (collection.size() > 0)
                              objectMethods[i].invoke(object, new Object[] { null, collection });
                          }
                      }
                    finally
                      {
                        result2.close();
                      }
                  }
              }
          }
      }

    static int objectTransaction(Database db, Object object, Set nullValuesToInclude, boolean isInsertUpdate, String externalClauses, Object[] parameters) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException, InstantiationException
      {
        if (object == null)
          throw new JPersistException("object is null");

        int returnValue = 0;
        boolean commit = false, rollback = false;

        if (db.getAutoCommit())
          {
            db.setAutoCommit(false);
            commit = true;
          }
        
        try
          {
            if (isInsertUpdate)
              returnValue = saveObject(db, object, nullValuesToInclude, externalClauses, parameters);
            else
              returnValue = deleteObject(db, object, nullValuesToInclude, externalClauses, parameters);
          }
        catch (Exception e)
          {
            rollback = true;
            
            if (commit)
              db.rollback();
              
            throw new JPersistException(e);
          }
        finally
          {
            if (commit)
              {
                if (!rollback)
                  db.commit();
                
                db.setAutoCommit(true);
              }
            
            if (!rollback && isInsertUpdate && object instanceof PersistentObject && ((PersistentObject)object).getReloadAfterSave())
              {
                try
                  {
                    Result result = queryObject(db, object.getClass(), object, null, true, null, (Object[])null);

                    try
                      {
                        if (result.hasNext())
                          result.next(object);
                      }
                    finally
                      {
                        result.close();
                      }
                  }
                catch (Exception e)
                  {
                    String message = "Could not reload object following save.  Can not make this object persistent.";

                    if (e.getMessage() != null && e.getMessage().startsWith("useIdColumnsOnly")
                          && !db.getMetaData().supportsGeneratedKeys())
                      {
                        message += "\nYour database reports that it does not support retrieving auto-generated keys."
                                 + "\nTherefore, you can't make objects relying on auto-generated keys persistent following an insert."
                                 + "\nIn this case, only loaded objects can be persistent.  You can either implement GeneratedKeys or set "
                                 + "PersistentObject.setReloadAfterSave(false)";
                      }

                    throw new JPersistException(message, e);
                  }
              }
          }
        
        return returnValue;
      }

    static int saveObject(Database db, Object object, Set nullValuesToInclude, String externalClauses, Object[] parameters) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException, InstantiationException
      {
        int returnValue = 0;
        boolean persistentUpdate = (object instanceof PersistentObject && ((PersistentObject)object).getObjectChecksum() != 0 && ((PersistentObject)object).getObjectPersistence() == PersistentObject.OBJECT_CAN_PERSIST),
                externalWhere = (externalClauses != null && externalClauses.toLowerCase().startsWith("where")),
                globalUpdate = object.getClass().isAnnotationPresent(GlobalUpdate.class);

        if ( persistentUpdate || globalUpdate || externalWhere)
          {
            if (globalUpdate || externalWhere || ((PersistentObject)object).objectHasChanged() || calculateChecksum(db, object) != ((PersistentObject)object).getObjectChecksum())
              returnValue = updateObject(db, object, nullValuesToInclude, externalClauses, parameters);
          }
        else 
          returnValue = insertObject(db, object);

        returnValue += saveAssociations(db, object);
        
        return returnValue;
      }

    static int saveAssociations(Database db, Object object) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException, InstantiationException
      {
        Method methods[] = object.getClass().getMethods();
        boolean isPersistentObject = object instanceof PersistentObject;
        int returnValue = 0;
        
        for (int i = 0; i < methods.length; i++)
          {
            if (methods[i].getName().equals("getDbAssociation"))
              {
                Object associationObject = methods[i].invoke(object, new Object[] { null });

                if (associationObject != null)
                  if (associationObject instanceof Collection)
                    {
                      Iterator it = ((Collection)associationObject).iterator();
                      Object obj = null;

                      while (it.hasNext() && (obj = it.next()) != null)
                        {
                          if (!(isPersistentObject && ((PersistentObject)object).classInIgnoreAssociation(obj.getClass())))
                            {
                              copyAssociationIds(db, object, obj);
                              returnValue += saveObject(db, obj, null, null, null);
                            }
                        }
                    }
                  else if (associationObject.getClass().isArray())
                    {
                      Object objectArray[] = (Object[])associationObject;

                      for (int i2 = 0; i2 < objectArray.length; i2++)
                        {
                          if (!(isPersistentObject && ((PersistentObject)object).classInIgnoreAssociation(objectArray[i2].getClass())))
                            {
                              copyAssociationIds(db, object, objectArray[i2]);
                              returnValue += saveObject(db, objectArray[i2], null, null, null);
                            }
                        }
                    }
                  else 
                    {
                      if (!(isPersistentObject && ((PersistentObject)object).classInIgnoreAssociation(associationObject.getClass())))
                        {
                          copyAssociationIds(db, object, associationObject);
                          returnValue = saveObject(db, associationObject, null, null, null);
                        }
                    }
              }
          }

        return returnValue;
      }

    static int insertObject(Database db, Object object) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException, InstantiationException
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("inserting object of class " + object.getClass().getName());
        
        if (object == null)
          throw new JPersistException("object is null");

        List returnValues = new ArrayList();

        processClasses(db, object.getClass(), object, true, false, false, false, null, new InsertClassHandler(db, returnValues));

        if (object instanceof PersistentObject)
          ((PersistentObject)object).setObjectChecksum(calculateChecksum(db, object));
        
        int returnValue = 0;
        
        for (int i = 0; i < returnValues.size(); i++)
          returnValue += ((Integer)returnValues.get(i)).intValue();
        
        return returnValue;
      }

    static class InsertClassHandler implements ClassHandler
      {
        String identifierQuoteString;
        List returnValues;
        Database db;
        
        InsertClassHandler(Database db, List returnValues) throws JPersistException
          {
            this.db = db;
            this.returnValues = returnValues;
            this.identifierQuoteString = db.getMetaData().getIdentifierQuoteString();
          }
      
        public void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
          {
            StringBuffer sqlStatement = new StringBuffer("insert into " + identifierQuoteString + table.getTableName() + identifierQuoteString + " ("),
                         columnsStrBuf = new StringBuffer(),
                         valuesStrBuf = new StringBuffer();
            List columnValues = new ArrayList(), keysRequested = null, keysReturned = null;
            
            processColumns(table, valuesMap, sqlStatement, columnsStrBuf, valuesStrBuf, columnValues);
            
            String dbUrl = db.getMetaData().getDatabaseUrl().toLowerCase(),
                   possibleGeneratedKey = table.getPossibleGeneratedKey();
            
            if (dbUrl.startsWith("jdbc:oracle") && possibleGeneratedKey != null)
              {
                keysReturned = new ArrayList();
                keysReturned.add(possibleGeneratedKey);
                keysRequested = new ArrayList(keysReturned);
              }
            
            returnValues.add(new Integer(db.parameterizedUpdate(sqlStatement.toString(), keysReturned, columnValues.toArray())));

            processGeneratedKeys(table, db.getColumnMapper(), object, dbUrl, keysRequested, keysReturned);
          }
        
        void processColumns(MetaData.Table table, Map valuesMap, StringBuffer sqlStatement, StringBuffer columnsStrBuf, StringBuffer valuesStrBuf, List columnValues) throws JPersistException
          {
            int readOnly = 0;
            
            for (Iterator it = valuesMap.entrySet().iterator(); it.hasNext();)
              {
                Map.Entry entry = (Map.Entry)it.next();
                MetaData.Table.Column column = (MetaData.Table.Column)entry.getKey();

                if (!column.isReadOnly())
                  {
                    Object obj = entry.getValue();

                    if (!(obj instanceof NullValue))
                      {
                        columnsStrBuf.append((columnsStrBuf.length() > 0 ? ", " : "") + identifierQuoteString + column.getColumnName() + identifierQuoteString);
                        columnValues.add(obj);
                      }
                  }
                else readOnly++;
              }
        
            if (columnValues.size() == 0)
              {
                if (readOnly > 0)
                  throw new JPersistException("Table " + table.getTableName() + " appears to be readonly, might need to log in to the database");
                else
                  throw new JPersistException("There must be some number of column values to insert");
              }

            sqlStatement.append(columnsStrBuf + ") values(");

            for (Iterator it = columnValues.iterator(); it.hasNext(); it.next())
              valuesStrBuf.append((valuesStrBuf.length() > 0 ? ", " : "") + "?");

            sqlStatement.append(valuesStrBuf + ")");
          }

        void processGeneratedKeys(MetaData.Table table, ColumnMapping columnMapper, Object object, String dbUrl, List keysRequested, List keysReturned) throws JPersistException, IllegalAccessException, InvocationTargetException
          {
            if (object instanceof GeneratedKeys)
              ((GeneratedKeys)object).setGeneratedKeys(db, keysRequested, keysReturned);
            else if (dbUrl.startsWith("jdbc:oracle") && keysReturned != null && keysReturned.size() > 0)
              {
                if (keysRequested.size() != keysReturned.size())
                  throw new JPersistException("Auto-generated keys returned (" + keysReturned.size() 
                                              + ") do not match the number of primary keys requested " + keysRequested
                                              + "; try implementing GeneratedKeys");

                setAutoGeneratedKeys(object, table, columnMapper, keysRequested, keysReturned);
              }
            else if (table.getGeneratedKey() != null)
              {
                boolean queryExecuted = false;
                Result result = null;

                if (dbUrl.startsWith("jdbc:mysql"))
                  {
                    result = db.executeQuery("select LAST_INSERT_ID() as id ");
                    queryExecuted = true;
                  }
                else if (dbUrl.startsWith("jdbc:derby") || dbUrl.startsWith("jdbc:db2"))
                  {
                    result = db.executeQuery("select IDENTITY_VAL_LOCAL() as id from " + table.getTableName());
                    queryExecuted = true;
                  }
                else if (dbUrl.startsWith("jdbc:hsqldb") || dbUrl.startsWith("jdbc:h2"))
                  {
                    result = db.executeQuery("select IDENTITY() as id from " + table.getTableName());
                    queryExecuted = true;
                  }
                else if (dbUrl.startsWith("jdbc:postgresql"))
                  {
                    result = db.executeQuery("select currval('" + table.getTableName() + '_' + table.getGeneratedKey() + "_seq" + "') as id");
                    queryExecuted = true;
                  }

                if (queryExecuted && result.hasNext() && result.next() != null)
                  {
                    keysReturned = new ArrayList();
                    keysReturned.add(result.getColumnValue("id"));

                    keysRequested = new ArrayList();
                    keysRequested.add(table.getGeneratedKey());

                    setAutoGeneratedKeys(object, table, db.getColumnMapper(), keysRequested, keysReturned);
                  }
              }
          }
      }
    
    static void setAutoGeneratedKeys(Object object, MetaData.Table table, ColumnMapping columnMapper, List keysRequested, List keysReturned) throws IllegalAccessException, InvocationTargetException
      {
        for (int h = 0; h < keysReturned.size(); h++)
          {
            String key = (String)keysRequested.get(h);
            Object value = keysReturned.get(h);
            
            Method method = getMatchingMethod(columnMapper, key, object, false);

            if (method != null)
              {
                Object convertedObject = ObjectConverter.convertObject(method.getParameterTypes()[0], value);

                if (convertedObject != null)
                  method.invoke(object, new Object[] { convertedObject });

                if (object instanceof PersistentObject)
                  ((PersistentObject)object).getObjectKeyValues().put(key, value);
              }
          }
      }

    static int updateObject(Database db, Object object, Set nullValuesToInclude, String externalClauses, Object[] parameters) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("updating object of class " + object.getClass().getName()
                     + "\nexternalClauses = " + externalClauses
                     + "\nparameters[] = " + StringUtils.toString(parameters));
      
        if (object == null)
          throw new JPersistException("object is null");

        List returnValues = new ArrayList();
        Map updatedKeys = new HashMap();

        processClasses(db, object.getClass(), object, true, false, false, true, nullValuesToInclude, 
                       new UpdateClassHandler(db, updatedKeys, returnValues, externalClauses, parameters));

        if (object instanceof PersistentObject)
          {
            ((PersistentObject)object).getObjectKeyValues().putAll(updatedKeys);
            ((PersistentObject)object).setObjectChecksum(calculateChecksum(db, object));
          }
          
        int returnValue = 0;
        
        for (int i = 0; i < returnValues.size(); i++)
          returnValue += ((Integer)returnValues.get(i)).intValue();
        
        return returnValue;
      }

    static class UpdateClassHandler implements ClassHandler
      {
        Database db;
        String externalClauses, identifierQuoteString;
        Map updatedKeys;
        Object[] parameters;
        List returnValues;
        
        UpdateClassHandler(Database db, Map updatedKeys, List returnValues, String externalClauses, Object[] parameters) throws JPersistException
          {
            this.db = db;
            this.externalClauses = externalClauses;
            this.parameters = parameters;
            this.updatedKeys = updatedKeys;
            this.returnValues = returnValues;
            
            this.identifierQuoteString = db.getMetaData().getIdentifierQuoteString();
          }
        
        public void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
          {
            StringBuffer sqlStatement = new StringBuffer();
            List columnValues = new ArrayList(), whereValues = new ArrayList();
            StringBuffer columnsStrBuf = new StringBuffer(), whereStrBuf = new StringBuffer();

            for (Iterator it = valuesMap.entrySet().iterator(); it.hasNext(); )
              {
                Map.Entry entry = (Map.Entry)it.next();
                MetaData.Table.Column column = (MetaData.Table.Column)entry.getKey();
                String columnName = column.getColumnName();
                Object obj = entry.getValue();

                if (!column.isReadOnly())
                  {
                    if (obj instanceof NullValue)
                      columnsStrBuf.append((columnsStrBuf.length() > 0 ? ", " : "") + identifierQuoteString + columnName + identifierQuoteString + " = null");
                    else
                      {
                        columnsStrBuf.append((columnsStrBuf.length() > 0 ? ", " : "") + identifierQuoteString + columnName + identifierQuoteString + " = ?");
                        columnValues.add(obj);
                      }
                  }

                if (object instanceof PersistentObject && ((PersistentObject)object).getObjectKeyValue(columnName) != null)
                  updatedKeys.put(columnName, obj);

                if (object instanceof PersistentObject && (obj = ((PersistentObject)object).getObjectKeyValue(columnName)) != null)
                  if (column.isSearchable())
                    {
                      whereStrBuf.append((whereStrBuf.length() > 0 ? " and " : "") + identifierQuoteString + columnName + identifierQuoteString + (hasWildCards(obj.toString()) ? " like ?" : " = ?"));
                      whereValues.add(obj);
                    }
              }

            sqlStatement.append("update " + identifierQuoteString + table.getTableName() + identifierQuoteString + " set " + columnsStrBuf);

            if (whereStrBuf.length() > 0 && (externalClauses == null || !externalClauses.startsWith("where")))
              sqlStatement.append(" where ").append(whereStrBuf);

            StringBuffer externalClausesStrBuf = null;
            
            if (externalClauses != null)
              processExternalClauses(externalClausesStrBuf = new StringBuffer(externalClauses), table, db.getColumnMapper(), object, identifierQuoteString);
            
            if (externalClauses != null)
              {
                sqlStatement.append(" " + externalClausesStrBuf);

                if (parameters != null)
                  for (int i = 0; i < parameters.length; i++)
                    whereValues.add(parameters[i]);
              }

            if (columnValues.size() > 0 && (whereValues.size() > 0 || objectClass.isAnnotationPresent(GlobalUpdate.class)))
              {
                columnValues.addAll(whereValues);
                returnValues.add(new Integer(db.parameterizedUpdate(sqlStatement.toString(), columnValues.toArray())));
              }
            else
              throw new JPersistException("Object does not have a where clause and does not extend GlobalUpdate");
          }
      }
    
    static int deleteObject(Database db, Object object, Set nullValuesToInclude, String externalClauses, Object[] parameters) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("deleting object of class " + object.getClass().getName()
                       + "\nexternalClauses = " + externalClauses
                       + "\nparameters[] = " + StringUtils.toString(parameters));
      
        if (object == null)
          throw new JPersistException("object is null");

        List returnValues = new ArrayList();

        processClasses(db, object.getClass(), object, true, false, true, false, nullValuesToInclude, 
                       new DeleteClassHandler(db, returnValues, externalClauses, parameters));

        if (object instanceof PersistentObject)
          ((PersistentObject)object).makeObjectTransient();
          
        int returnValue = 0;
        
        for (int i = 0; i < returnValues.size(); i++)
          returnValue += ((Integer)returnValues.get(i)).intValue();
        
        return returnValue;
      }

    static class DeleteClassHandler implements ClassHandler
      {
        Database db;
        String identifierQuoteString, externalClauses;
        Object[] parameters;
        List returnValues;
        
        DeleteClassHandler(Database db, List returnValues, String externalClauses, Object[] parameters) throws JPersistException
          {
            this.db = db;
            this.externalClauses = externalClauses;
            this.parameters = parameters;
            this.returnValues = returnValues;
            
            this.identifierQuoteString = db.getMetaData().getIdentifierQuoteString();
          }
      
        public void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
          {
            StringBuffer sqlStatement = new StringBuffer(), whereStrBuf = new StringBuffer();
            List values = new ArrayList();

            for (Iterator it = valuesMap.entrySet().iterator(); it.hasNext(); )
              {
                Map.Entry entry = (Map.Entry)it.next();
                MetaData.Table.Column column = (MetaData.Table.Column)entry.getKey();

                if (column.isSearchable())
                  {
                    String columnName = column.getColumnName();
                    Object obj = entry.getValue();

                    if (!(obj instanceof NullValue))
                      if (!(object instanceof PersistentObject) || ((PersistentObject)object).getObjectChecksum() == 0
                          || (((PersistentObject)object).getObjectChecksum() != 0 && (obj = ((PersistentObject)object).getObjectKeyValue(columnName)) != null))
                        {
                          whereStrBuf.append((whereStrBuf.length() > 0 ? " and " : "") + identifierQuoteString + columnName + identifierQuoteString + (hasWildCards(obj.toString()) ? " like ?" : " = ?"));
                          values.add(obj);
                        }
                  }
              }

            sqlStatement.append("delete ");

            if (externalClauses == null || !externalClauses.startsWith("from"))
              {
                sqlStatement.append(" from " + identifierQuoteString + table.getTableName() + identifierQuoteString);

                if (whereStrBuf.length() > 0 || (externalClauses == null || !externalClauses.startsWith("where")))
                  sqlStatement.append(" where ").append(whereStrBuf);
              }
            
            StringBuffer externalClausesStrBuf = null;
            
            if (externalClauses != null)
              processExternalClauses(externalClausesStrBuf = new StringBuffer(externalClauses), table, db.getColumnMapper(), object, identifierQuoteString);

            if (externalClauses != null)
              {
                sqlStatement.append(" " + externalClausesStrBuf);

                if (parameters != null)
                  for (int i = 0; i < parameters.length; i++)
                    values.add(parameters[i]);
              }

            if (values.size() > 0)
              returnValues.add(new Integer(db.parameterizedUpdate(sqlStatement.toString(), values.toArray())));
            else if (!(objectClass.isAnnotationPresent(GlobalDelete.class)))
              throw new JPersistException("Object does not have a where clause and does not extend GlobalDelete");
            else
              returnValues.add(new Integer(db.executeUpdate(sqlStatement.toString())));
          }
      }
    
    static void copyAssociationIds(Database db, Object object, Object associationObject) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        Set keys = getMatchingImportedExportedKeys(getImportedExportedKeys(db, object, true), 
                                                   getImportedExportedKeys(db, associationObject, false));

        if (keys.size() != 0)
          {
            for (Iterator it = keys.iterator(); it.hasNext();)
              {
                MetaData.Table.Key key = (MetaData.Table.Key)it.next();

                Method getMethod = getMatchingMethod(db.getColumnMapper(), key.getForeignColumnName(), object, true),
                       setMethod = getMatchingMethod(db.getColumnMapper(), key.getLocalColumnName(), associationObject, false);

                if (getMethod == null)
                  throw new JPersistException("Could not locate method for column " + key.getForeignColumnName());
                if (setMethod == null)
                  throw new JPersistException("Could not locate method for column " + key.getLocalColumnName());

                Object convertedObject = ObjectConverter.convertObject(setMethod.getParameterTypes()[0], getMethod.invoke(object, (Object[])null));

                if (convertedObject == null)
                  throw new JPersistException("Could not match primary/foreign keys association relation represented by " + object.getClass().getName());

                setMethod.invoke(associationObject, new Object[] { convertedObject });
              }
          }
        else
          {
            throw new JPersistException("The association relation represented by " 
                                        + object.getClass().getName() + " <-- " + associationObject.getClass().getName()
                                        + " does not have primary/foreign key relationships defined for the underlying"
                                        + " tables (must have a FOREIGN KEY ... REFERENCES ... clause in table creation, see alter table)");
          }
      }

    static Method getMatchingMethod(ColumnMapping columnMapper, String columnName, Object object, boolean getMethod)
      {
        Method methods[] = object.getClass().getMethods();
        String matchName = MetaData.normalizeName(columnName);

        for (int i = 0; i < methods.length; i++)
          if ((getMethod && methods[i].getName().startsWith("get")) || (!getMethod &&  methods[i].getName().startsWith("set")))
            if (MetaData.normalizeName(methods[i].getName().substring(3)).indexOf(matchName) > -1)
              return methods[i];

        String name = null;
        
        if (object instanceof ColumnMapping && (name = ((ColumnMapping)object).getTableColumnName(columnName.toLowerCase())) != null)
          matchName = name;
        else if (columnMapper != null && (name = columnMapper.getTableColumnName(columnName.toLowerCase())) != null)
          matchName = name;

        for (int i = 0; i < methods.length; i++)
          if ((getMethod && methods[i].getName().startsWith("get")) || (!getMethod && methods[i].getName().startsWith("set")))
            if (MetaData.normalizeName(methods[i].getName().substring(3)).indexOf(matchName) > -1)
              return methods[i];

        return null;
      }

    static Set getMatchingImportedExportedKeys(Map objectKeys, Map associationKeys)
      {
        Set keys = new HashSet();

        for (Iterator it = associationKeys.entrySet().iterator(); it.hasNext();)
          {
            MetaData.Table.Key foreignKey = (MetaData.Table.Key)((Map.Entry)it.next()).getValue();

            if (foreignKey != null)
              {
                MetaData.Table.Key localKey = (MetaData.Table.Key)objectKeys.get(foreignKey.getForeignColumnName());

                if (localKey != null)
                  keys.add(foreignKey);
              }
          }

        return keys;
      }

    static Map getImportedExportedKeys(Database db, Object object, boolean exportedKeys) throws JPersistException, SQLException
      {
        Map keys = new HashMap();
        Class objectClass = object.getClass();
        Package p = objectClass.getPackage();
        
        while (objectClass != null && p == null || (p != null && (!objectClass.getPackage().equals(ObjectSupport.class.getPackage()) && !objectClass.getPackage().getName().equals("java.lang"))))
          {
            String tableName = getTableName(objectClass);
            MetaData.Table table = db.getMetaData().getTable(db.getConnection(), db.getTableMapper(), db.getColumnMapper(), db.getCatalogPattern(), db.getSchemaPattern(), tableName, object);
            
            if (table == null)
              throw new JPersistException("Table " + tableName + " is not locatable");
            
            if (exportedKeys)
              keys.putAll(table.getExportedKeys());
            else
              keys.putAll(table.getImportedKeys());
            
            objectClass = objectClass.getSuperclass();
            p = objectClass.getPackage();
          }
        
        return keys;
      }
    
    static long calculateChecksum(Database db, Object object) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        ChecksumCalculator checksumCalculator = new ChecksumCalculator();

        processClasses(db, object.getClass(), object, true, false, false, false, null, checksumCalculator);
        
        return checksumCalculator.getCheckSum();
      }
    
    static class ChecksumCalculator implements ClassHandler
      {
        CRC32 checkSum = new CRC32();
        
        public void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws SQLException, IllegalAccessException, InvocationTargetException
          {
            for (Iterator it = valuesMap.entrySet().iterator(); it.hasNext();)
              checkSum.update(((Map.Entry)it.next()).getValue().toString().getBytes());
          }
        
        long getCheckSum() { return checkSum.getValue(); }
      }
    
    static interface ClassHandler
      {
        void processClass(Class objectClass, Object object, MetaData.Table table, int numberTables, char tableAlias, Map valuesMap, Set selectableColumns) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException;
      }
    
    static void processClasses(Database db, Class objectClass, Object object, boolean tableRequired, boolean IdColumnsOnly, boolean baseTableOnly, boolean isUpdate, Set nullValuesToInclude, ClassHandler ch) throws JPersistException, SQLException, IllegalAccessException, InvocationTargetException
      {
        Class headClass = objectClass, baseClass = null;
        Package p = objectClass.getPackage();
        char tableAlias = 'a';
        List classes = new ArrayList();
        boolean singleTable = objectClass.isAnnotationPresent(SingleTableInheritance.class),
                allFieldsSt = singleTable || objectClass.isAnnotationPresent(ConcreteTableInheritance.class);
        int numberOfTables = 0;
        
        while (p == null || (p != null && (!objectClass.getPackage().equals(ObjectSupport.class.getPackage()) && !objectClass.getPackage().getName().equals("java.lang") && !objectClass.getPackage().getName().equals("rnd.mywt.server.bean"))))
          {
            classes.add(objectClass);

            baseClass = objectClass;
            objectClass = objectClass.getSuperclass();
            
            p = objectClass.getPackage();
          }

        if (baseTableOnly || allFieldsSt)
          numberOfTables = 1;
        else
          numberOfTables = classes.size();
        
        for (int i = classes.size() - 1; i > -1; i--, tableAlias++)
          {
            String tableName = null;
            
            if (allFieldsSt)
              {
                objectClass = headClass;
                tableName = getTableName(singleTable ? baseClass : headClass);
              }
            else
              {
                objectClass = (Class)classes.get(i);
                tableName = getTableName(objectClass);
              }
            
            MetaData.Table table = db.getMetaData().getTable(db.getConnection(), db.getTableMapper(), db.getColumnMapper(), db.getCatalogPattern(), db.getSchemaPattern(), tableName, object);

            if (table == null && tableRequired)
              throw new JPersistException("Table " + tableName + " is not locatable");

            Map valuesMap = new HashMap();
            Set selectableColumns = new HashSet();

            if (table != null)
              getValuesMap(valuesMap, selectableColumns, table, db.getColumnMapper(), object, objectClass, nullValuesToInclude, IdColumnsOnly, isUpdate, allFieldsSt);
            
            ch.processClass(objectClass, object, table, numberOfTables, tableAlias, valuesMap, selectableColumns);
            
            if (baseTableOnly || allFieldsSt)
              break;
          }
      }
    
    static void getValuesMap(Map valuesMap, Set selectableColumns, MetaData.Table table, ColumnMapping columnMapper, Object object, Class objectClass, Set nullValuesToInclude, boolean IdColumnsOnly, boolean isUpdate, boolean allFieldsSti) throws IllegalAccessException, InvocationTargetException, JPersistException
      {
        Method methods[] = objectClass.getMethods();
        
        for (int i = 0; i < methods.length; i++)
          {
            if (methods[i].getName().startsWith("get") && (allFieldsSti ||  methods[i].getDeclaringClass().equals(objectClass))
                    && !methods[i].getName().equals("getDbAssociation") 
                    && methods[i].getParameterTypes().length == 0)
              {
                MetaData.Table.Column column = null;
                String methodName = methods[i].getName().substring(3);
                column = table.getColumn(columnMapper, methodName, object);

                if (column != null)
                  {
                    selectableColumns.add(column);
                    
                    if (object != null)
                      {
                        Object value = methods[i].invoke(object, (Object[])null);

                        if (IdColumnsOnly == false || table.getPrimaryKeys().size() == 0 || column.isPrimaryKey())
                          {
                            if (value != null)
                              valuesMap.put(column, value);
                            else
                              {
                                boolean includeNull = isUpdate && object != null && objectClass.isAnnotationPresent(UpdateNullValues.class);

                                if (!includeNull && nullValuesToInclude != null)
                                  includeNull = nullValuesToInclude.contains(methodName) 
                                             || nullValuesToInclude.contains(column.getClassName())
                                             || nullValuesToInclude.contains(Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1));

                                if (includeNull)
                                  valuesMap.put(column, new NullValue(column.getDataType()));
                              }
                          }
                      }
                  }
              }
          }
      }

    static void processExternalClauses(StringBuffer externalClausesStrBuf, MetaData.Table table, ColumnMapping columnMapper, Object object, String identifierQuoteString) throws JPersistException
      {
        int pos = 0;

        while ((pos = externalClausesStrBuf.indexOf(":")) > -1)
          {
            int pos2 = 0;

            for (pos2 = pos + 1; pos2 < externalClausesStrBuf.length() && "~`!@#$%^&*()-=+\\|]}[{'\";:/?.>,< ".indexOf(externalClausesStrBuf.charAt(pos2)) == -1; pos2++);

            String searchStr = externalClausesStrBuf.substring(pos+1,pos2);
            MetaData.Table.Column column = table.getColumn(columnMapper, searchStr, object);
            
            if (column != null)
              externalClausesStrBuf.replace(pos, pos2, identifierQuoteString + column.getColumnName() + identifierQuoteString);
          }
      }
        
    static boolean valueIsZeroOrFalse(Object object)
      {
        if (object instanceof Integer)
          return ((Integer)object).intValue() == 0;
        
        if (object instanceof Short)
          return ((Short)object).shortValue() == 0;
        
        if (object instanceof Long)
          return ((Long)object).longValue() == 0;
        
        if (object instanceof Float)
          return ((Float)object).floatValue() == 0.0;

        if (object instanceof Double)
          return ((Double)object).doubleValue() == 0.0;
        
        if (object instanceof Boolean)
          return ((Boolean)object).booleanValue() == false;
        
        return false;
      }
    
    static String getTableName(Class objectClass)
      {
        String tableName = objectClass.getName().replace('$','.');
        
        if (tableName.lastIndexOf('.') != -1)
          tableName = tableName.substring(tableName.lastIndexOf('.') + 1);
        
        if (tableName.indexOf(';') != -1)
          tableName = tableName.substring(0,tableName.indexOf(';'));
        
        return tableName;
      }

    static boolean hasWildCards(String value)
      {
        if (value.indexOf('%') != -1 || value.indexOf('_') != -1)
          return  true;

        return false;
      }
    
    static class NullValue
      {
        int sqlType;

        NullValue(int sqlType)
          {
            this.sqlType = sqlType;
          }
        
        public int getSqlType() { return sqlType; }
      }
  }
