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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jpersist.interfaces.AsciiStream;
import jpersist.interfaces.AsciiStreamAdapter;
import jpersist.interfaces.BinaryStream;
import jpersist.interfaces.BinaryStreamAdapter;
import jpersist.interfaces.CharacterStream;
import jpersist.interfaces.CharacterStreamAdapter;
import jpersist.interfaces.ResultObject;

/**
 * The result class is created and returned by all query methods in jpersist.Database 
 * and jpersist.DatabaseManager, and basically wraps a ResultSet with ResultSet
 * functionality, along with ListIterator and object loading functionality.
 */
@SuppressWarnings("unchecked")
public class Result<T> implements ResultObject<T>
  {
    private static Logger logger = Logger.getLogger(Result.class.getName());
    private boolean isClosed, resultSetIsEmpty;
    private ResultSet resultSet;
    private Database db;
    private Class<T> cs;
    
    /**
     * Create an instance of Result by passing in a jpersist.Database instance 
     * and a ResultSet instance returned from a query (statement.getResultSet()).
     *
     * @param db 
     * @param resultSet
     */
    public Result(Database db, ResultSet resultSet) throws JPersistException
      {
        this.db = db;
        this.resultSet = resultSet;
        
        try
          {
            boolean beforeFirst = resultSet.isBeforeFirst(), afterLast = resultSet.isAfterLast();
            resultSetIsEmpty = !(beforeFirst || afterLast) || (beforeFirst && afterLast);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Create an instance of Result by passing in a jpersist.Database instance 
     * and a ResultSet instance returned from a query (statement.getResultSet()).
     *
     * @param db 
     * @param resultSet
     *
     * Setting the class allows next() and previous() to return loaded 
     * (with data from the current row) objects of the class.
     */
    public Result(Database db, ResultSet resultSet, Class<T> cs) throws JPersistException
      {
        this.db = db;
        this.resultSet = resultSet;
        this.cs = cs;
        
        try
          {
            boolean beforeFirst = resultSet.isBeforeFirst(), afterLast = resultSet.isAfterLast();
            resultSetIsEmpty = !(beforeFirst || afterLast) || (beforeFirst && afterLast);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * If class (see setClass()) is defined, iteration will return instances of 
     * class loaded with the current row of data.
     *
     * @return returns this
     */
    
    public Iterator<T> iterator() { return this; }

    /**
     * Setting the class allows next() and previous() to return loaded 
     * (with data from the current row) objects of the class.
     *
     * @param cs the class to create instances for
     *
     * @return returns this
     */
    public Result setClass(Class<T> cs)
      {
        this.cs = cs;
        
        return this;
      }
    
    /**
     * Closes all resources associated with a JDBC Statement and ResultSet.
     */
    public void close() throws JPersistException
      {
        if (!isClosed)
          {
            db.closeResult(this);
            isClosed = true;
          }
      }
    
    /**
     * Closes the result (simply calls close()), can be used in EL. For example:
     * 
     * <pre>
     *     ${result.close}
     * </pre>
     *
     * @throws JPersistException
     */
    
    public String getClose() throws JPersistException
      {
        close();
        
        return null;
      }
            
    /**
     * Closes the result (simply calls close()), via a JavaBeans setter.  For example:
     * <pre>
     *     <jsp:useBean id="result" scope="request" class="jpersist.Result" />
     *     <jsp:setProperty name="result" property="closed" value="true"/>
     * </pre>
     * 
     * @throws JPersistException
     */
    
    public void setClosed(boolean true_only) throws JPersistException
      {
        close();
      }
            
    /**
     * Returns true if the result is closed, false otherwise.
     *
     * @return true or false
     */
    
    public boolean isClosed() { return isClosed; }
    
    /**
     * Return the current ResultSet, if there is a current ResultSet instance.
     * 
     * @return a JDBC ResultSet instance or null if there isn't one.
     *
     * @throws JPersistException
     */
    
    public ResultSet getResultSet() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        return resultSet;
      }
    
    /**
     * Return the current ResultSet, if there is a current ResultSet instance.
     * 
     * @return a JDBC ResultSet instance or null if there isn't one.
     *
     * @throws JPersistException
     */
    
    public Database getDatabase() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        return db;
      }
    
    /**
     * Returns the current statement or null if no statement is active.
     * 
     * @return a JDBC statement
     *
     * @throws JPersistException
     */
    
    public Statement getStatement() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return resultSet.getStatement();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * See same in java.sql.Statement
     *
     * @see java.sql.Statement
     */
    
    public boolean getMoreResults() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        logger.finer("Attempting to get more results");

        try
          {
            Statement statement = getStatement();
            
            if (statement != null && statement.getMoreResults())
              {
                resultSet = statement.getResultSet();
                
                boolean beforeFirst = resultSet.isBeforeFirst(), afterLast = resultSet.isAfterLast();
                resultSetIsEmpty = !(beforeFirst || afterLast) || (beforeFirst && afterLast);
                
                return true;
              }
            
            resultSet = null;
            resultSetIsEmpty = false;

            return false;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Statement
     *
     * @see java.sql.Statement
     */
    
    public boolean getMoreResults(int doWhatWithCurrent) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        logger.finer("Attempting to get more results");

        try
          {
            Statement statement = getStatement();
            
            if (statement != null && statement.getMoreResults(doWhatWithCurrent))
              {
                resultSet = statement.getResultSet();
                
                boolean beforeFirst = resultSet.isBeforeFirst(), afterLast = resultSet.isAfterLast();
                resultSetIsEmpty = !(beforeFirst || afterLast) || (beforeFirst && afterLast);
                
                return true;
              }
            
            resultSet = null;
            resultSetIsEmpty = false;

            return false;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Load an instance of the class (using set methods that match columns in a table matched to the class name) 
     * with the results of the current ResultSet row.
     * 
     * @param cs any class with an empty constructor
     *
     * @return a new instance of cs 
     *
     * @throws JPersistException
     */
    
    public <C> C loadObject(Class<C> cs) throws JPersistException
      {
        return loadObject(cs, true);
      }
    
    /**
     * Load an instance of the class (using set methods that match columns in a table matched to the class name) 
     * with the results of the current ResultSet row.
     * 
     * @param cs any class with an empty constructor
     * @param loadAssociations true to load associations
     *
     * @return a new instance of cs 
     *
     * @throws JPersistException
     */
    
    public <C> C loadObject(Class<C> cs, boolean loadAssociations) throws JPersistException
      {
        try
          {
            return loadObject(cs.newInstance(), loadAssociations);
          }
        catch (Exception ex)
          {
            throw new JPersistException(ex);
          }
      }
    
    /**
     * Load the object (using set methods that match columns in a table matched to the class name) 
     * with the results of the current ResultSet row.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     *
     * @return the object passed in
     *
     * @throws JPersistException
     */
    
    public <C> C loadObject(C object) throws JPersistException
      {
        return loadObject(object, true);
      }
    
    /**
     * Load the object (using set methods that match columns in a table matched to the class name) 
     * with the results of the current ResultSet row.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param loadAssociations true to load associations
     *
     * @return the object passed in
     *
     * @throws JPersistException
     */
    
    public <C> C loadObject(C object, boolean loadAssociations) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return (C)ObjectSupport.loadObject(this, object, loadAssociations);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Loads objects (using set methods that match columns in a table matched to the class name) 
     * into a collection with the results of the current ResultSet.
     * 
     * @param collection an instance of Collection
     * @param cs any class with an empty constructor
     *
     * @return the collection passed in
     *
     * @throws JPersistException
     */
    
    public <C> Collection<C> loadObjects(Collection<C> collection, Class<C> cs) throws JPersistException
      {
        return loadObjects(collection, cs, true);
      }
    
    /**
     * Loads objects (using set methods that match columns in a table matched to the class name) 
     * into a collection with the results of the current ResultSet.
     * 
     * @param collection an instance of Collection
     * @param cs any class with an empty constructor
     * @param loadAssociations true to load associations
     *
     * @return the collection passed in
     *
     * @throws JPersistException
     */
    
    public <C> Collection<C> loadObjects(Collection<C> collection, Class<C> cs, boolean loadAssociations) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            while (hasNext())
              collection.add((C)next(cs.newInstance(), loadAssociations));
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        
        return collection;
      }

    /**
     * Loads an objects associations.
     * 
     * @param object the object whose associations are to be loaded
     * 
     * @throws jpersist.JPersistException
     */
    public void loadAssociations(Object object) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            ObjectSupport.loadAssociations(db, object);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void moveToCurrentRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().moveToCurrentRow();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void moveToInsertRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            if (getResultSet() != null)
              getResultSet().moveToInsertRow();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void insertRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().insertRow();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean rowInserted() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().rowInserted();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void deleteRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().deleteRow(); 
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean rowDeleted() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().rowDeleted();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void updateRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().updateRow();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean rowUpdated() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().rowUpdated();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void setFetchDirection(int direction) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Setting fetch direction = " + direction);
        
        try
          {
            getResultSet().setFetchDirection(direction);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean first() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().first();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean isFirst() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().isFirst();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void beforeFirst() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().beforeFirst();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean isBeforeFirst() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().isBeforeFirst();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean last() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().last();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean isLast() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().isLast();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void afterLast() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().afterLast();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public boolean isAfterLast() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getResultSet().isAfterLast();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ResultSet
     *
     * @see java.sql.ResultSet
     */
    
    public void refreshRow() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getResultSet().refreshRow();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.ListIterator
     *
     * @see java.util.ListIterator
     */
    
    public boolean hasNext()
      {
        try
          {
            ResultSet resultSet = getResultSet();
            
            return !resultSetIsEmpty && (resultSet.isBeforeFirst() || !resultSet.isLast());
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }

    /**
     * Returns the next record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If object is null and class is not 
     * defined, then this (Result) is returned.  If there is no next 
     * record, then null is returned.
     *
     * This method performs as defined in ListIterator with the following 
     * exception: if type is TYPE_FORWARD_ONLY, then null is returned.  This 
     * allows the next method to perform similar to ResultSet.next(), with 
     * next() != null instead of next() != false, which is the case with 
     * ResultSet.next().
     *
     * @return the loaded object
     *
     * @see java.util.ListIterator
     */
    
    public T next()
      {
        return next(true);
      }
    
    /**
     * Returns the next record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If object is null and class is not 
     * defined, then this (Result) is returned.  If there is no next 
     * record, then null is returned.
     *
     * This method performs as defined in ListIterator with the following 
     * exception: if type is TYPE_FORWARD_ONLY, then null is returned.  This 
     * allows the next method to perform similar to ResultSet.next(), with 
     * next() != null instead of next() != false, which is the case with 
     * ResultSet.next().
     *
     * @param loadAssociations load the objects associations
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
    
    public T next(boolean loadAssociations)
      {
        try
          {
            return next(cs != null ? cs.newInstance() : null, loadAssociations);
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }
    
    /**
     * Returns the next record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If object is null and class is not 
     * defined, then this (Result) is returned.  If there is no next 
     * record, then null is returned.
     *
     * This method performs as defined in ListIterator with the following 
     * exception: if type is TYPE_FORWARD_ONLY, then null is returned.  This 
     * allows the next method to perform similar to ResultSet.next(), with 
     * next() != null instead of next() != false, which is the case with 
     * ResultSet.next().
     *
     * @param object the object to load
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
    
    public <C> C next(C object)
      {
        return next(object, true);
      }
    
    /**
     * Returns the next record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If object is null and class is not 
     * defined, then this (Result) is returned.  If there is no next 
     * record, then null is returned.
     *
     * This method performs as defined in ListIterator with the following 
     * exception: if type is TYPE_FORWARD_ONLY, then null is returned.  This 
     * allows the next method to perform similar to ResultSet.next(), with 
     * next() != null instead of next() != false, which is the case with 
     * ResultSet.next().
     *
     * @param object the object to load
     * @param loadAssociations load the objects associations
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
    
    public <C> C next(C object, boolean loadAssociations)
      {
        try
          {
            ResultSet resultSet = getResultSet();
            
            if (resultSet.next())
              {
                if (object == null)
                  return (C)this;
                else
                  return loadObject(object, loadAssociations);
              }
            
            if (resultSet.getType() == ResultSet.TYPE_FORWARD_ONLY)
              return null;
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
        
        throw new NoSuchElementException();
      }

    /**
     * See same in java.sql.ListIterator
     *
     * @see java.util.ListIterator
     */
    
    public boolean hasPrevious() 
      {
        try
          {
            ResultSet resultSet = getResultSet();
            
            return !resultSetIsEmpty && (resultSet.isAfterLast() || !resultSet.isFirst());
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }
    
    /**
     * Returns the previous record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If the object is null and class is 
     * not defined, then this (Result) is returned.  If there is no previous 
     * record, then null is returned.
     * 
     * @return the loaded object
     *
     * @see java.util.ListIterator
     */
    public T previous() 
      {
        return previous(true);
      }
    
    /**
     * Returns the previous record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If the object is null and class is 
     * not defined, then this (Result) is returned.  If there is no previous 
     * record, then null is returned.
     *
     * @param loadAssociations load the objects associations
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
     
    public T previous(boolean loadAssociations) 
      {
        try
          {
            return previous(cs != null ? cs.newInstance() : null, loadAssociations);
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }
    
    /**
     * Returns the previous record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If the object is null and class is 
     * not defined, then this (Result) is returned.  If there is no previous 
     * record, then null is returned.
     *
     * @param object the object to load
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
     
    public <C> C previous(C object) 
      {
        return previous(object, true);
      }
    
    /**
     * Returns the previous record.  If object is non-null, it is loaded with 
     * matching data.  If the object is null, then if class is defined, a new 
     * instance will be loaded and returned.  If the object is null and class is 
     * not defined, then this (Result) is returned.  If there is no previous 
     * record, then null is returned.
     *
     * @param object the object to load
     * @param loadAssociations load the objects associations
     * 
     * @return the loaded object
     * 
     * @see java.util.ListIterator
     */
     
    public <C> C previous(C object, boolean loadAssociations) 
      {
        try
          {
            if (getResultSet().previous())
              {
                if (object == null)
                  return (C)this;
                else
                  return loadObject(object, loadAssociations);
              }
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
        
        throw new NoSuchElementException();
      }
    
    /**
     * See same in java.sql.ListIterator
     *
     * @see java.util.ListIterator
     */
    
    public int nextIndex() 
      {
        try
          {
            if (hasNext())
              return getResultSet().getRow() + 1;
          }
        catch (Exception ex)
          {
            throw new RuntimeException(ex);
          }
        
        return -1;
      }
    
    /**
     * See same in java.sql.ListIterator
     *
     * @see java.util.ListIterator
     */
    
    public int previousIndex() 
      {
        try
          {
            if (hasPrevious())
              return getResultSet().getRow() - 1;
          }
        catch (Exception ex)
          {
            throw new RuntimeException(ex);
          }
        
        return -1;
      }
    
    /**
     * See same in java.sql.ListIterator
     *
     * @see java.util.ListIterator
     */
    
    public void remove() 
      {
        try
          {
            deleteRow();
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }
    
    /**
     * See same in java.sql.ListIterator.  Not supported, and no need to.
     *
     * @throws UnsupportedOperationException
     */
    
    public void add(Object o)  { throw new UnsupportedOperationException(); }
    
    /**
     * See same in java.sql.ListIterator.  Not supported, and no need to.
     *
     * @throws UnsupportedOperationException
     */
    
    public void set(Object o) { throw new UnsupportedOperationException(); }

    /**
     * Returns the object defined by named column from the current row in the result set.
     * 
     * @param columnName the column name
     *
     * @return the object
     *
     * @throws JPersistException
     */
    
    public <C> C getColumnValue(String columnName) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return (C)getResultSet().getObject(columnName); 
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Returns the object (after converting it to returnType) defined by named column from the current row in the result set.
     *
     * @param returnType converts the return type to returnType 
     * @param columnName the column name
     *
     * @return the object
     *
     * @throws JPersistException
     */
    
    public <C> C getColumnValue(Class<C> returnType, String columnName) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            Object value = null;

            if (returnType != null)
              if (returnType == Array.class)
                value = getResultSet().getArray(columnName);
              else if (returnType == BigDecimal.class)
                value = getResultSet().getBigDecimal(columnName);
              else if (returnType == Blob.class)
                value = getResultSet().getBlob(columnName);
              else if (returnType == Clob.class)
                value = getResultSet().getClob(columnName);
              else if (returnType == Date.class)
                value = getResultSet().getDate(columnName);
              else if (returnType == InputStream.class)
                {
                  throw new JPersistException("Must use jpersist.interfaces.AsciiStream or jpersist.interfaces.BinaryStream instead of InputStream");
                }          
              else if (returnType.isAssignableFrom(AsciiStream.class))
                value = new AsciiStreamAdapter(-1, getResultSet().getAsciiStream(columnName));
              else if (returnType.isAssignableFrom(BinaryStream.class))
                value = new BinaryStreamAdapter(-1, getResultSet().getBinaryStream(columnName));
              else if (returnType == Reader.class)
                {
                  throw new JPersistException("Must use jpersist.interfaces.ReaderHandler instead of Reader");
                }
              else if (returnType.isAssignableFrom(CharacterStream.class))
                value = new CharacterStreamAdapter(-1, getResultSet().getCharacterStream(columnName));
              else if (returnType == Ref.class)
                value = getResultSet().getRef(columnName);
              else if (returnType == String.class)
                value = getResultSet().getString(columnName);
              else if (returnType == Time.class)
                value = getResultSet().getTime(columnName);
              else if (returnType == Timestamp.class)
                value = getResultSet().getTimestamp(columnName);
              else if (returnType == URL.class)
                value = getResultSet().getURL(columnName);

            if (value == null)
              value = getResultSet().getObject(columnName);

            return (C)value;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Returns the object defined by column index from the current row in the result set.
     * 
     * @param columnIndex the column index
     *
     * @return the object
     *
     * @throws JPersistException
     */
    
    public <C> C getColumnValue(int columnIndex) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return (C)getResultSet().getObject(columnIndex); 
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Returns the object (after converting it to returnType) defined by named column from the current row in the result set.
     *
     * @param returnType converts the return type to returnType 
     * @param columnIndex the column index
     *
     * @return the object
     *
     * @throws JPersistException
     */
    
    public <C> C getColumnValue(Class<C> returnType, int columnIndex) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            Object value = null;

            if (returnType != null)
              if (returnType == Array.class)
                value = getResultSet().getArray(columnIndex);
              else if (returnType == BigDecimal.class)
                value = getResultSet().getBigDecimal(columnIndex);
              else if (returnType == Blob.class)
                value = getResultSet().getBlob(columnIndex);
              else if (returnType == Clob.class)
                value = getResultSet().getClob(columnIndex);
              else if (returnType == Date.class)
                value = getResultSet().getDate(columnIndex);
              else if (returnType == InputStream.class)
                {
                  throw new JPersistException("Must use jpersist.interfaces.AsciiStream or jpersist.interfaces.BinaryStream instead of InputStream");
                }          
              else if (returnType.isAssignableFrom(AsciiStream.class))
                value = new AsciiStreamAdapter(-1, getResultSet().getAsciiStream(columnIndex));
              else if (returnType.isAssignableFrom(BinaryStream.class))
                value = new BinaryStreamAdapter(-1, getResultSet().getBinaryStream(columnIndex));
              else if (returnType == Reader.class)
                {
                  throw new JPersistException("Must use jpersist.interfaces.ReaderHandler instead of Reader");
                }
              else if (returnType.isAssignableFrom(CharacterStream.class))
                value = new CharacterStreamAdapter(-1, getResultSet().getCharacterStream(columnIndex));
              else if (returnType == Ref.class)
                value = getResultSet().getRef(columnIndex);
              else if (returnType == String.class)
                value = getResultSet().getString(columnIndex);
              else if (returnType == Time.class)
                value = getResultSet().getTime(columnIndex);
              else if (returnType == Timestamp.class)
                value = getResultSet().getTimestamp(columnIndex);
              else if (returnType == URL.class)
                value = getResultSet().getURL(columnIndex);

            if (value == null)
              value = getResultSet().getObject(columnIndex);

            return (C)value;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Sets the object value defined by column name in the current row in the result set.
     * 
     * @param columnName the column name
     * @param object the object being stored in the database
     *
     * @throws JPersistException
     */
    
    public void setColumnValue(String columnName, Object object) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            if (object instanceof Array)
              resultSet.updateArray(columnName, (Array)object);
            else if (object instanceof BigDecimal)
              resultSet.updateBigDecimal(columnName, (BigDecimal)object);
            else if (object instanceof Blob)
              resultSet.updateBlob(columnName, (Blob)object);
            else if (object instanceof Clob)
              resultSet.updateClob(columnName, (Clob)object);
            else if (object instanceof Date)
              resultSet.updateDate(columnName, (Date)object);
            else if (object instanceof InputStream)
              {
                throw new JPersistException("Must use jpersist.interfaces.AsciiStream or jpersist.interfaces.BinaryStream instead of InputStream");
              }
            else if (object instanceof AsciiStream)
              resultSet.updateAsciiStream(columnName, ((AsciiStream)object).getInputStream(), ((AsciiStream)object).getLength());
            else if (object instanceof BinaryStream)
              resultSet.updateBinaryStream(columnName, ((BinaryStream)object).getInputStream(), ((BinaryStream)object).getLength());
            else if (object instanceof Reader)
              {
                throw new JPersistException("Must use jpersist.interfaces.ReaderHandler instead of Reader");
              }
            else if (object instanceof CharacterStream)
              resultSet.updateCharacterStream(columnName, ((CharacterStream)object).getReader(), ((CharacterStream)object).getLength());
            else if (object instanceof Ref)
              resultSet.updateRef(columnName, (Ref)object);
            else if (object instanceof String)
              resultSet.updateString(columnName, (String)object);
            else if (object instanceof Time)
              resultSet.updateTime(columnName, (Time)object);
            else if (object instanceof Timestamp)
              resultSet.updateTimestamp(columnName, (Timestamp)object);
            //else if (object instanceof URL)
            //  resultSet.updateURL(columnName, (URL)object);
            else
              resultSet.updateObject(columnName, object);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
   * Sets the object value defined by column index in the current row in the result set.
   * 
   * @param columnIndex the column number
   * @param object the object being stored in the database
   * @throws JPersistException
   */
    
    public void setColumnValue(int columnIndex, Object object) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            if (object instanceof Array)
              resultSet.updateArray(columnIndex, (Array)object);
            else if (object instanceof BigDecimal)
              resultSet.updateBigDecimal(columnIndex, (BigDecimal)object);
            else if (object instanceof Blob)
              resultSet.updateBlob(columnIndex, (Blob)object);
            else if (object instanceof Clob)
              resultSet.updateClob(columnIndex, (Clob)object);
            else if (object instanceof Date)
              resultSet.updateDate(columnIndex, (Date)object);
            else if (object instanceof InputStream)
              {
                throw new JPersistException("Must use jpersist.interfaces.AsciiStream or jpersist.interfaces.BinaryStream instead of InputStream");
              }
            else if (object instanceof AsciiStream)
              resultSet.updateAsciiStream(columnIndex, ((AsciiStream)object).getInputStream(), ((AsciiStream)object).getLength());
            else if (object instanceof BinaryStream)
              resultSet.updateBinaryStream(columnIndex, ((BinaryStream)object).getInputStream(), ((BinaryStream)object).getLength());
            else if (object instanceof Reader)
              {
                throw new JPersistException("Must use jpersist.interfaces.ReaderHandler instead of Reader");
              }
            else if (object instanceof CharacterStream)
              resultSet.updateCharacterStream(columnIndex, ((CharacterStream)object).getReader(), ((CharacterStream)object).getLength());
            else if (object instanceof Ref)
              resultSet.updateRef(columnIndex, (Ref)object);
            else if (object instanceof String)
              resultSet.updateString(columnIndex, (String)object);
            else if (object instanceof Time)
              resultSet.updateTime(columnIndex, (Time)object);
            else if (object instanceof Timestamp)
              resultSet.updateTimestamp(columnIndex, (Timestamp)object);
            //else if (object instanceof URL)
            //  resultSet.updateURL(columnIndex, (URL)object);
            else
              resultSet.updateObject(columnIndex, object);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Casts the result to an interface.  Any get methods in the interface 
     * that can be mapped to the current resultSet will automatically return the 
     * coresponding value from the current row in the resultSet.
     *
     * <p>Handlers are objects that provide backing to one or more interface methods.
     * jPersist will check the array of objects for an object that can service a 
     * given method and call that method on the object.  If no object is found 
     * to handle the method it's assumed to be a call to get a column of data 
     * from the database.
     *
     * @param interfaceClass the interface to cast the result to
     * @param handlers objects that provide backing to one or more interface methods
     *
     * @return an instance of the interface
     *
     * @throws JPersistException
     */

    public Object castToInterface(Class interfaceClass, Object... handlers) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Casting Result to " + interfaceClass.getName());
        
        try
          {
            Object object = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, new ResultProxy(this, interfaceClass, handlers));

            if (object == null)
              throw new JPersistException("Dynamic Proxy Returned null");

            return object;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
  }
