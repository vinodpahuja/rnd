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
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcommontk.utils.StringUtils;
import jpersist.ObjectSupport.NullValue;
import jpersist.interfaces.AsciiStream;
import jpersist.interfaces.BinaryStream;
import jpersist.interfaces.CharacterStream;
import jpersist.interfaces.ColumnMapping;
import jpersist.interfaces.TableMapping;
//import jwebtk.licensing.License;
//import jwebtk.licensing.LicenseException;

/**
 * The center of JPersist, the Database class provides a seemless integration 
 * of the JDBC classes Connection, Statement, and ResultSet,  while also 
 * providing object oriented database access.
 */

@SuppressWarnings("unchecked")
public final class Database
  {
    private static Logger logger = Logger.getLogger(Database.class.getName());
    
    private String databaseName, catalogPattern, schemaPattern, querySql, updateSql;
    private TableMapping tableMapper;
    private ColumnMapping columnMapper;
    private List resultsList, queryQueueList, batchUpdateCounts;
    private DatabaseManager databaseManager;
    private Statement queryStatement, updateStatement;
    private Connection connection;
    private boolean isClosed, isBatch;
    private int fetchSize, maxRows, resultSetType, resultSetConcurrency;

    /* Non-public access ************************************************************/

    Database(DatabaseManager databaseManager) //, License license) throws LicenseException
      {
        this.databaseManager = databaseManager;

        //if (license == null || !license.isLicenseValid())
        //  throw new LicenseException("Invalid License");
      }

    DatabaseManager getDatabaseManager() { return databaseManager; }
    
    void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    void initDatabase()
      {
        querySql = updateSql = null;
        queryStatement = updateStatement = null;
        resultsList = new ArrayList();
        queryQueueList = new ArrayList();
        isClosed = false;
        fetchSize = maxRows = resultSetType = resultSetConcurrency = 0;
      }

    void setConnection(Connection connection)
      {
        this.connection = connection; 
      }

    void closeResult(Result result)
      {
        try
          {
            ResultSet resultSet = result.getResultSet();
            Statement statement = resultSet.getStatement();

            if (statement == queryStatement)
              resultSet.close();
            else
              statement.close();
          }
        catch (Exception e) { } // don't care.  JDBC spec says close can be called multiple times, but Resin was complaining.
      }
    
    void closeQueryStatement()
      {
        if (queryStatement != null)
          {
            try
              {
                queryStatement.close();
              }
            catch (Exception e) { } // don't care.  JDBC spec says close can be called multiple times, but Resin was complaining.

            queryStatement = null;
          }
      }
    
    void closeUpdateStatement()
      {
        if (updateStatement != null)
          {
            try
              {
                updateStatement.close();
              }
            catch (Exception e) { } // don't care.  JDBC spec says close can be called multiple times, but Resin was complaining.

            updateStatement = null;
          }
      }
    
    String getCatalogPattern() { return catalogPattern; }

    String getSchemaPattern() { return schemaPattern; }
    
    TableMapping getTableMapper() { return tableMapper; }

    ColumnMapping getColumnMapper() { return columnMapper; }
    
    /* End Non-public access ************************************************************/
    
    /**
     * Create a Database instance.
     *
     * @param databaseName the name of this database
     * @param connection a java.sql.Connection instance
     * @deprecated
     */
    @Deprecated
    public Database(String databaseName, Connection connection) //, String licenseKey) throws LicenseException
      {
        this.databaseName = databaseName;
        this.connection = connection;
/*
        License license = new License(licenseKey, "j.*p.*");
        
        if (license.getCheckSum() != 2323253737L && license.getCheckSum() != 3756392378L)
          throw new LicenseException("Invalid License - Invalid Checksum");

        logger.warning("License: " + license);
*/
        initDatabase();
      }
/*
    public Database(String databaseName, Connection connection)
      {
        this.databaseName = databaseName;
        this.connection = connection;

        initDatabase();
      }
*/
    /**
     * Returns the name of the current dtabase handler.
     *
     * @return a string representing the database handler name
     */
    
    public String getDatabaseName() { return databaseName; }

    /**
     * Returns the database connection.
     * 
     * @return the JDBC connection instance
     *
     * @throws JPersistException
     */
    
    public Connection getConnection() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        return connection;
      }

    /**
     * Returns the database meta data associated with the current database.  
     * The MetaData class can be used to access information about tables in 
     * the database.  It can also be used to add table and column mapping.
     * 
     * @return an instance of MetaData
     *
     * @throws JPersistException
     */
    
    public MetaData getMetaData() throws JPersistException 
      {
        try
          {
            return MetaData.getMetaData(connection); 
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Sets limits on the meta data table information returned.  Defining 
     * catalogPattern and schemaPattern can help reduce the amount of time
     * spent loading table information.  With some databases, it is absolutely
     * needed.  This can also be set with databases.xml and/or DatabaseManager
     * constructors.  Meta data limits will be reset (using the values in 
     * DatabaseManager) with each call to DatabaseManager.getDatabase().
     * 
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public void setMetaDataLimits(String catalogPattern, String schemaPattern)
      {
        logger.fine("Limiting meta data with catalog = " + catalogPattern + ", schema = " + schemaPattern);
        
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
      }

    /**
     * This method can be used to add global table and column mappers.
     * Objects implementaing TableMapping and/or ColumnMapping override the global mappers.
     *
     * @param tableMapper an instance of TableMapping
     * @param columnMapper an instance of ColumnMapping
     */
    public void setGlobalMappers(TableMapping tableMapper, ColumnMapping columnMapper) 
      {
        this.tableMapper = tableMapper;
        this.columnMapper = columnMapper;
      }

    /**
     * Closes the Database (returns pooled connections to the pool).
     * 
     * @throws JPersistException
     */
    
    public void close() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        logger.finer("closing database");

        try
          {
            endBatch();

            for (Iterator it = resultsList.iterator(); it.hasNext();)
              ((Result)it.next()).close();

            closeQueryStatement();
            closeUpdateStatement();
          }
        finally
          {
            if (databaseManager != null)
              databaseManager.releaseDatabase(this);
          }
        
        isClosed = true;
      }

    /**
     * Closes the Database (simply calls close()), can be used in EL. For example:
     * 
     * <pre>
     *     ${db.close}
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
     * Closes the Database (simply calls close()), via a JavaBeans setter.  For example:
     * <pre>
     *     <jsp:useBean id="db" scope="request" class="jpersist.Database" />
     *     <jsp:setProperty name="db" property="closed" value="true"/>
     * </pre>
     * 
     * @throws JPersistException
     */
    
    public void setClosed(boolean true_only) throws JPersistException
      {
        close();
      }
            
    /**
     * Returns true if the database is closed, false otherwise.
     *
     * @return true or false
     */
    
    public boolean isClosed() { return isClosed; }
    
    /**
     * See same in java.sql.Statement
     *
     * @see java.sql.Statement
     */
    
    public void setFetchSize(int fetchSize) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Setting fetch size to " + fetchSize);
        
        this.fetchSize = fetchSize; 
      }
    
    /**
     * See same in java.sql.Statement
     *
     * @see java.sql.Statement
     */
    
    public void setMaxRows(int maximumResultRows) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Setting maximum result rows to " + maximumResultRows);
        
        this.maxRows = maximumResultRows; 
      }
    
    /**
     * Defaults to ResultSet.TYPE_SCROLL_INSENSITIVE.
     * 
     * @see java.sql.Connection
     * @see java.sql.ResultSet
     *
     * @throws JPersistException
     */

    public void setResultSetType(int resultSetType) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Setting result set type to " + resultSetType);
        
        this.resultSetType = resultSetType;
      }

    /**
     * Defaults to ResultSet.CONCUR_READ_ONLY.
     * 
     * @see java.sql.Connection
     * @see java.sql.ResultSet
     *
     * @throws JPersistException
     */
    
    public void setResultSetConcurrency(int resultSetConcurrency) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("Setting result set concurrency to " + resultSetConcurrency);
        
        this.resultSetConcurrency = resultSetConcurrency;
      }

    /**
     * Creates a simple statement that is configured more for queries.
     * 
     * @return a JDBC Statement instance
     *
     * @throws JPersistException
     */
    
    public Statement getStatementForQuery() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating statement for querying: resultSetType = " + resultSetType + ", resultSetConcurrency = " + resultSetConcurrency);

            Statement statement = connection.createStatement(resultSetType > 0 ? resultSetType : ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                             resultSetConcurrency > 0 ? resultSetConcurrency : ResultSet.CONCUR_READ_ONLY);

            if (fetchSize != 0)
              statement.setFetchSize(fetchSize);

            if (maxRows != 0)
              statement.setMaxRows(maxRows);
          
            return statement;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Creates a simple statement that is configured more for updates.
     * 
     * @return a JDBC Statement instance
     *
     * @throws JPersistException
     */
    
    public Statement getStatementForUpdate() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating statement for updating");

            if (isBatch && updateStatement != null)
              if (!(updateStatement instanceof PreparedStatement))
                return updateStatement;
              else
                {
                  executeBatch();
                  closeUpdateStatement();
                }

            return updateStatement = getConnection().createStatement();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Creates a prepared statement that is configured more for queries.
     * 
     * @param sql the sql statement to be used (eg. "select * from tbl where id = ?")
     *
     * @return a JDBC PreparedStatement instance
     *
     * @throws JPersistException
     */
    
    public PreparedStatement getPreparedStatementForQuery(String sql) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (sql == null || sql.length() == 0)
          throw new JPersistException(JPersistException.SQL_STATEMENT_NULL);

        try
          {
            if (queryStatement != null && queryStatement instanceof PreparedStatement && sql.equals(querySql))
              return (PreparedStatement)queryStatement;

            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating prepared statement for querying:\n" + "sql = " + sql + "\n" 
                          + "resultSetType = " + resultSetType + ", resultSetConcurrency = " + resultSetConcurrency);

            queryStatement = connection.prepareStatement(sql, resultSetType > 0 ? resultSetType : ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                         resultSetConcurrency > 0 ? resultSetConcurrency : ResultSet.CONCUR_READ_ONLY);

            if (fetchSize != 0)
              queryStatement.setFetchSize(fetchSize);

            if (maxRows != 0)
              queryStatement.setMaxRows(maxRows);
          
            querySql = sql;

            return (PreparedStatement)queryStatement;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Creates a prepared statement that is configured more for updates.
     * 
     * @param sql the sql statement to be used (eg. "select * from tbl where id = ?")
     * @param keys either an empty list to return generated keys in or a list of keys to return.
     *
     * @return a JDBC PreparedStatement instance
     *
     * @throws JPersistException
     */
    
    public PreparedStatement getPreparedStatementForUpdate(String sql, List keys) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (sql == null || sql.length() == 0)
          throw new JPersistException(JPersistException.SQL_STATEMENT_NULL);

        try
          {
            if (updateStatement != null)
              if (updateStatement instanceof PreparedStatement && !(updateStatement instanceof CallableStatement) && sql.equals(updateSql))
                return (PreparedStatement)updateStatement;
              else
                {
                  if (isBatch)
                    executeBatch();

                  closeUpdateStatement();
                }
            
            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating prepared statement for updating");

            if (keys != null && keys.size() > 0)
              updateStatement = getConnection().prepareStatement(sql, (String[])keys.toArray(new String[keys.size()]));
            else if (keys != null)
              updateStatement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            else
              updateStatement = getConnection().prepareStatement(sql);

            updateSql = sql;

            return (PreparedStatement)updateStatement;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Creates a callable statement that can be used to interact with PL/SQL, and can also continue to be 
     * used with the database handler (i.e. query results can be loaded into objects and so forth).
     * 
     * @param sql the sql statement to be used (eg. "{call getName(?, ?)}")
     *
     * @return a JDBC CallableStatement instance
     *
     * @throws JPersistException
     */
    
    public CallableStatement getCallableStatementForQuery(String sql) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (sql == null || sql.length() == 0)
          throw new JPersistException(JPersistException.SQL_STATEMENT_NULL);

        try
          {
            if (queryStatement != null && queryStatement instanceof CallableStatement && sql.equals(querySql))
              return (CallableStatement)queryStatement;
            
            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating callable statement for querying:\n" + "sql = " + sql + "\n"
                          + "resultSetType = " + resultSetType + ", resultSetConcurrency = " + resultSetConcurrency);

            queryStatement = connection.prepareCall(sql, resultSetType > 0 ? resultSetType : ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                    resultSetConcurrency > 0 ? resultSetConcurrency : ResultSet.CONCUR_READ_ONLY);

            if (fetchSize != 0)
              queryStatement.setFetchSize(fetchSize);

            if (maxRows != 0)
              queryStatement.setMaxRows(maxRows);
        
            querySql = sql;

            return (CallableStatement)queryStatement;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Creates a callable statement that can be used to interact with PL/SQL, and can also continue to be 
     * used with the database handler (i.e. query results can be loaded into objects and so forth).
     * 
     * @param sql the sql statement to be used (eg. "{call getName(?, ?)}")
     *
     * @return a JDBC CallableStatement instance
     *
     * @throws JPersistException
     */
    
    public CallableStatement getCallableStatementForUpdate(String sql, List keys) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (sql == null || sql.length() == 0)
          throw new JPersistException(JPersistException.SQL_STATEMENT_NULL);

        try
          {
            if (updateStatement != null)
              if (updateStatement instanceof CallableStatement && sql.equals(updateSql))
                return (CallableStatement)updateStatement;
              else
                {
                  if (isBatch)
                    executeBatch();

                  closeUpdateStatement();
                }
            
            if (logger.isLoggable(Level.FINER))
              logger.finer("Creating callable statement for updating");

            updateStatement = connection.prepareCall(sql);

            updateSql = sql;

            return (CallableStatement)updateStatement;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Builds a select query from the object and executes it.  Any methods matching table columns will be returned, 
     * and any values set will be used to build the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public <T> Result<T> queryObject(T object) throws JPersistException
      {
        return queryObject(object, null, null, (Object[])null);
      }
    
    /**
     * Builds a select query from the object and executes it.  Any methods matching table columns will be returned, 
     * and any values set will be used to build the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public <T> Result<T> queryObject(T object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return queryObject(object, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a select query from the object and executes it.  Any methods matching table columns will be returned.
     * externalClauses can begin with a where clause or anything after the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public <T> Result<T> queryObject(T object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return queryObject(object, null, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from the object and executes it.  Any methods matching table columns will be returned.
     * externalClauses can begin with a where clause or anything after the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public <T> Result<T> queryObject(T object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            Result<T> result = ObjectSupport.queryObject(this, object.getClass(), object, nullValuesToInclude, false, externalClauses, externalClausesParameters);
            
            resultsList.add(result);
            
            return result;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Builds a select query from a class that matches up to a table.  Any methods matching table 
     * columns will be used to build the column list.  externalClauses can begin with a where 
     * clause or anything after the where clause.
     * 
     * @param cs any class that matches up to a table
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public <T> Result<T> queryObject(Class<T> cs) throws JPersistException
      {
        return queryObject(cs, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table.  Any methods matching table 
     * columns will be used to build the column list.  externalClauses can begin with a where 
     * clause or anything after the where clause.
     * 
     * @param cs any class that matches up to a table
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    public <T> Result<T> queryObject(Class<T> cs, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            Result<T> result = ObjectSupport.queryObject(this, cs, null, null, false, externalClauses, externalClausesParameters);
            
            resultsList.add(result);
            
            return result;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
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
            ObjectSupport.loadAssociations(this, object);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Builds either an update or an insert depending on whether the object is persistent and was previously loaded, or not, respectively.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     *
     * @return number of rows updated
     *
     * @throws JPersistException
     */
    
    public int saveObject(Object object) throws JPersistException
      {
        return saveObject(object, null, null, (Object[])null);
      }
    
    /**
     * Builds either an update or an insert depending on whether the object is persistent and was previously loaded, or not, respectively.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the update
     *
     * @return number of rows updated
     *
     * @throws JPersistException
     */
    
    public int saveObject(Object object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return saveObject(object, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds either an update or an insert depending on whether the object is persistent and was previously loaded, or not, respectively.
     * externalClauses can begin with a where clause or anything after the where clause.  
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return number of rows updated
     *
     * @throws JPersistException
     */
    
    public int saveObject(Object object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return saveObject(object, null, externalClauses, externalClausesParameters);
      }

    /**
     * Builds either an update or an insert depending on whether the object is persistent and was previously loaded, or not, respectively.
     * externalClauses can begin with a where clause or anything after the where clause.  
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the update
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return number of rows updated
     *
     * @throws JPersistException
     */
    
    public int saveObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return ObjectSupport.objectTransaction(this, object, nullValuesToInclude, true, externalClauses, externalClausesParameters);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Builds a delete statement from the object.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     *
     * @return number of rows deleted
     *
     * @throws JPersistException
     */
    
    public int deleteObject(Object object) throws JPersistException
      {
        return deleteObject(object, null, null, (Object[])null);
      }
    
    /**
     * Builds a delete statement from the object.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return number of rows deleted
     *
     * @throws JPersistException
     */
    
    public int deleteObject(Object object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return deleteObject(object, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a delete statement from the object.  externalClauses can begin with a where clause or anything after 
     * the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return number of rows deleted
     *
     * @throws JPersistException
     */
    
    public int deleteObject(Object object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return deleteObject(object, null, externalClauses, externalClausesParameters);
      }

    /**
     * Builds a delete statement from the object.  externalClauses can begin with a where clause or anything after 
     * the where clause.
     * 
     * @param object any Object (POJO, PersistentObject, etc.)
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return number of rows deleted
     *
     * @throws JPersistException
     */
    
    public int deleteObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return ObjectSupport.objectTransaction(this, object, nullValuesToInclude, false, externalClauses, externalClausesParameters);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Executes a query from a previously created (and setup) prepared or callable statement.
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public Result executeQuery() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (queryStatement == null)
          throw new JPersistException("Prepared or callable statement is null");
        
        try
          {
            Result result = new Result(this, ((PreparedStatement)queryStatement).executeQuery());
            
            resultsList.add(result);
            
            return result;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Executes a simple query.
     * 
     * @param sql the SQL statement
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public Result executeQuery(String sql) throws JPersistException
      {
        logger.fine(sql);

        if (sql == null || sql.length() == 0)
          throw new JPersistException(JPersistException.SQL_STATEMENT_NULL);

        try
          {
            Statement statement = getStatementForQuery();
            
            Result result = new Result(this, statement.executeQuery(sql));
            
            resultsList.add(result);
            
            return result;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Executes a parameterized query.  A paramterized query allows the use of '?' in 
     * SQL statements.
     * 
     * @param sql the SQL statement
     * @param parameters an array of objects used to set the parameters to the query
     *
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public Result parameterizedQuery(String sql, Object... parameters) throws JPersistException
      {
        if (logger.isLoggable(Level.FINE))
          logger.fine("sql = " + sql + "\nparameters[] = " + StringUtils.toString(parameters));

        try
          {
            PreparedStatement preparedStatement = getPreparedStatementForQuery(sql);

            setPreparedStatementObjects(preparedStatement, parameters);

            preparedStatement.execute();
            
            Result result = new Result(this, preparedStatement.getResultSet());
            
            resultsList.add(result);
            
            return result;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Executes an update from a previously created (and setup) prepared or callable statement.
     *
     * @return the number of rows updated
     *
     * @throws JPersistException
     */
    
    public int executeUpdate() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (queryStatement == null)
          throw new JPersistException("Prepared or callable statement is null");
        
        try
          {
            int rval = 0;
            
            if (isBatch)
              ((PreparedStatement)updateStatement).addBatch();
            else
              rval = ((PreparedStatement)updateStatement).executeUpdate();
            
            return rval;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * Executes a simple update.
     * 
     * @param sql the SQL statement
     *
     * @throws JPersistException
     */
    
    public int executeUpdate(String sql) throws JPersistException
      {
        return executeUpdate(sql, (List)null);
      }
    
    /**
     * Executes a simple update.
     * 
     * @param sql the SQL statement
     * @param keys is a List.  If keys is non-null, then generated keys will be returned in 
     *             the keys List.  List can also define the key columns required 
     *             (depending on the database).
     *
     * @throws JPersistException
     */
    
    public int executeUpdate(String sql, List keys) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("sql = " + sql + "\nkeys = " + keys);

        try
          {
            int rval = 0;
            Statement statement = getStatementForUpdate();

            if (isBatch)
              statement.addBatch(sql);
            else
              {
                if (keys != null && keys.size() > 0)
                  statement.executeUpdate(sql, (String[])keys.toArray(new String[keys.size()]));
                else if (keys != null)
                  statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                else
                  statement.executeUpdate(sql);

                if (keys != null)
                  {
                    ResultSet resultKeys = statement.getGeneratedKeys();

                    keys.clear();

                    while (resultKeys.next())
                      keys.add(new Long(resultKeys.getLong(1)));
                  }

                if (logger.isLoggable(Level.FINER))
                  logger.finer("keys = " + keys + ", rows updated = " + rval);
              }

            return rval;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Executes a parameterized update.  A paramterized update allows the use of '?' in 
     * SQL statements.
     * 
     * @param sql the SQL statement
     * @param parameters an array of objects used to set the parameters to the update
     *
     * @throws JPersistException
     */
    
    public int parameterizedUpdate(String sql, Object... parameters) throws JPersistException
      {
        return parameterizedUpdate(sql, null, parameters);
      }
    
    /**
     * Executes a parameterized update.  A paramterized update allows the use of '?' in 
     * SQL statements.
     * 
     * @param sql the SQL statement
     * @param keys is a List.  If keys is non-null, then generated keys will be returned in 
     *             the keys List.  List can also define the key columns required 
     *             (depending on the database).
     * @param parameters an array of objects used to set the parameters to the update
     *
     * @throws JPersistException
     */
    
    public int parameterizedUpdate(String sql, List keys, Object... parameters) throws JPersistException
      {
        try
          {
            if (logger.isLoggable(Level.FINE))
              logger.fine("sql = " + sql + "\nparameters[] = " + StringUtils.toString(parameters) + ", keys = " + keys);
            
            if (isClosed)
              throw new JPersistException(JPersistException.DATABASE_CLOSED);

            int rval = 0;
            PreparedStatement preparedStatement = getPreparedStatementForUpdate(sql, keys);

            setPreparedStatementObjects(preparedStatement, parameters);

            if (isBatch)
              preparedStatement.addBatch();
            else
              {
                rval = preparedStatement.executeUpdate();

                if (keys != null)
                  {
                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

                    keys.clear();

                    while (generatedKeys.next())
                      keys.add(generatedKeys.getObject(1));
                  }

                if (logger.isLoggable(Level.FINER))
                  logger.finer("keys = " + keys + ", rows updated = " + rval);
              }

            return rval;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Adds a query to the query queue.  The query queue is very 
     * useful for building up several queries to be executed at a later 
     * time (page rendering for example).  Each call to nextQuery() executes 
     * the queries in the query queue in the order they where added.
     * 
     * @param sql simple query sql
     *
     * @throws JPersistException
     */
    
    public void queueQuery(String sql) throws JPersistException
      {
        logger.finer(sql);
        
        queryQueueList.add(sql);
      }
    
    /**
     * Adds a parameterized query to the query queue.  The query queue is very 
     * useful for building up several queries to be executed at a later 
     * time (page rendering for example).  Each call to nextQuery() executes 
     * the queries in the query queue in the order they where added.
     * 
     * @param sql parameterized query sql
     * @param parameters an array of objects used to set the parameters to the query
     *
     * @throws JPersistException
     */
    
    public void queueParameterizedQuery(String sql, Object... parameters) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINER))
          logger.finer("sql = " + sql + "\nparameters[] = " + StringUtils.toString(parameters));
        
        Object[] obj = new Object[parameters.length + 1];
        
        obj[0] = sql;
        
        for (int i = 0; i < parameters.length; i++)
          obj[i + 1] = parameters[i];
      }

    /**
     * Simply calls nextQuery().  Usefull in environments where it is easier to call get methods.
     * 
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public Result getNextQuery() throws JPersistException
      {
        return nextQuery();
      }

    /**
     * If a query exists in the query queue it is executed and a Result instance is 
     * returned, otherwise null is returned.  The query queue is very 
     * useful for building up several queries to be executed at a later 
     * time (page rendering for example).  Each call to nextQuery() executes 
     * the queries in the query queue in the order they where added.
     * 
     * @return a jpersist.Result instance 
     *
     * @throws JPersistException
     */
    
    public Result nextQuery() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (queryQueueList.size() > 0)
          {
            Object obj = queryQueueList.get(0);

            queryQueueList.remove(0);
            
            if (obj instanceof String)
              return executeQuery((String)obj);
            else
              {
                String sql = (String)((Object[])obj)[0];
                Object[] objs = new Object[((Object[])obj).length - 1];

                for (int i = 0; i < objs.length; i++)
                  objs[i] = ((Object[])obj)[i+1];

                return parameterizedQuery(sql, objs);
              }
          }
        
        return null;
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void setAutoCommit(boolean autoCommit) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Setting auto commit to " + autoCommit);
        
        try
          {
            getConnection().setAutoCommit(autoCommit);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Starts a transaction by setting auto commit to false.  endTransaction() should allways be called to reset the auto commit mode.
     */
    
    public void beginTransaction() throws JPersistException
      {
        if (logger.isLoggable(Level.FINE))
          logger.fine("Starting transaction");
        
        setAutoCommit(false);
      }
    
    /**
     * Ends a transaction by setting auto commit to true (also commits the transaction).
     * Should allways be called, even after commit or rollback.
     */
    
    public void endTransaction() throws JPersistException
      {
        if (logger.isLoggable(Level.FINE))
          logger.fine("End transaction");
        
        setAutoCommit(true);
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public boolean getAutoCommit() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getConnection().getAutoCommit();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void commit() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Commiting transaction");

        try
          {
            getConnection().commit();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void rollback() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Rolling back transaction");
        
        try
          {
            getConnection().rollback();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void rollback(Savepoint savepoint) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Rolling back transaction");
        
        try
          {
            getConnection().rollback(savepoint);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public Savepoint setSavepoint() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Adding savepoint");
        
        try
          {
            return getConnection().setSavepoint();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public Savepoint setSavepoint(String name) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Adding savepoint named = " + name);
        
        try
          {
            return getConnection().setSavepoint(name);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void releaseSavepoint(Savepoint savepoint) throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        if (logger.isLoggable(Level.FINE))
          logger.fine("Releasing savepoint");
        
        try
          {
            if (isClosed)
              throw new JPersistException(JPersistException.DATABASE_CLOSED);

            getConnection().releaseSavepoint(savepoint);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Start adding sql statements to batch update processing.  If the statement 
     * type (Statement, PreparedStatement or CallableStatement) changes or a PreparedStatements SQL 
     * changes the current batch is executed and a new batch is started.
     */
    public void beginBatch() throws JPersistException
      {
        if (getMetaData().supportsBatchUpdates())
          {
            isBatch = true;
            
            try
              {
                closeUpdateStatement();
              }
            catch (Exception e)
              {
                throw new JPersistException(e);
              }
            
            batchUpdateCounts = new ArrayList();
          }
      }

    /**
     * Executes the statements that have been added to batch processing.
     * 
     * @throws jpersist.JPersistException
     */
    public void executeBatch() throws JPersistException
      {
        if (isBatch && updateStatement != null)
          try
            {
              for (Integer count : updateStatement.executeBatch())
                batchUpdateCounts.add(count);
            }
          catch (Exception e)
            {
              throw new JPersistException(e);
            }
      }

    /**
     * Returns the update counts for any previous batch updates (between beginBatch() and endBatch()).
     * 
     * @return an array of Integer values representing the update counts
     */
    public Integer[] getBatchUpdateCounts() 
      {
        Integer[] updateCounts = null;
        
        if (batchUpdateCounts != null)
          updateCounts = (Integer[]) batchUpdateCounts.toArray(new Integer[batchUpdateCounts.size()]); 
        
        return updateCounts;
      }

    /**
     * Clears the batch update counts.
     */
    public void clearBatchUpdateCounts() 
      {
        if (batchUpdateCounts != null)
          batchUpdateCounts.clear(); 
      }

    /**
     * Ends batch update processing.
     */
    public void endBatch() throws JPersistException
      {
        isBatch = false;
        
        try
          {
            closeUpdateStatement();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        
        clearBatchUpdateCounts();
        batchUpdateCounts = null;
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public SQLWarning getWarnings() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            return getConnection().getWarnings();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    /**
     * See same in java.sql.Connection
     *
     * @see java.sql.Connection
     */
    
    public void clearWarnings() throws JPersistException
      {
        if (isClosed)
          throw new JPersistException(JPersistException.DATABASE_CLOSED);

        try
          {
            getConnection().clearWarnings();
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    /**
     * Sets the prepared statement (using appropriate set methods) with objects from array.
     *
     * @param preparedStatement an instance of java.sql.PreparedStatement
     * @param objects an array of objects
     */
    
    public static void setPreparedStatementObjects(PreparedStatement preparedStatement, Object... objects) throws SQLException, JPersistException
      {
        for (int i = 0; i < objects.length; i++)
          {
            if (objects[i] instanceof NullValue)
              preparedStatement.setNull(i+1, ((NullValue)objects[i]).getSqlType());
            else if (objects[i] instanceof Array)
              preparedStatement.setArray(i+1, (Array)objects[i]);
            else if (objects[i] instanceof BigDecimal)
              preparedStatement.setBigDecimal(i+1, (BigDecimal)objects[i]);
            else if (objects[i] instanceof Blob)
              preparedStatement.setBlob(i+1, (Blob)objects[i]);
            else if (objects[i] instanceof Clob)
              preparedStatement.setClob(i+1, (Clob)objects[i]);
            else if (objects[i] instanceof Date)
              preparedStatement.setDate(i+1, (Date)objects[i]);
            else if (objects[i] instanceof InputStream)
              {
                throw new JPersistException("Must use jpersist.interfaces.AsciiStream or jpersist.interfaces.BinaryStream instead of InputStream");
              }
            else if (objects[i] instanceof AsciiStream)
              preparedStatement.setAsciiStream(i+1, ((AsciiStream)objects[i]).getInputStream(), ((AsciiStream)objects[i]).getLength());
            else if (objects[i] instanceof BinaryStream)
              preparedStatement.setBinaryStream(i+1, ((BinaryStream)objects[i]).getInputStream(), ((BinaryStream)objects[i]).getLength());
            else if (objects[i] instanceof Reader)
              {
                throw new JPersistException("Must use jpersist.interfaces.ReaderHandler instead of Reader");
              }
            else if (objects[i] instanceof CharacterStream)
              preparedStatement.setCharacterStream(i+1, ((CharacterStream)objects[i]).getReader(), ((CharacterStream)objects[i]).getLength());
            else if (objects[i] instanceof Ref)
              preparedStatement.setRef(i+1, (Ref)objects[i]);
            else if (objects[i] instanceof String)
              preparedStatement.setString(i+1, (String)objects[i]);
            else if (objects[i] instanceof Time)
              preparedStatement.setTime(i+1, (Time)objects[i]);
            else if (objects[i] instanceof Timestamp)
              preparedStatement.setTimestamp(i+1, (Timestamp)objects[i]);
            else if (objects[i] instanceof URL)
              preparedStatement.setURL(i+1, (URL)objects[i]);
            else
              preparedStatement.setObject(i+1, objects[i]);
          }
      }
  }

