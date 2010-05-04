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

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jcommontk.utils.LoggerUtils;
import jcommontk.utils.StringUtils;
import jcommontk.utils.XMLParser;
import jcommontk.utils.XMLParserException;
import jpersist.interfaces.ColumnMapping;
import jpersist.interfaces.TableMapping;
import jpersist.utils.ResultSetUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The DatabaseManager provides pooling and management for database handlers and 
 * connection instances.  It also provides access to XML defined databases, 
 * logging, sql statements, and object-oriented database access.  An application 
 * can have any number of DatabaseManagers, but only one is typically needed.
 */

@SuppressWarnings("unchecked")
public final class DatabaseManager
  {
    private static final int CONNECTION_SOURCE_IS_UNDEFINED = 0;
    private static final int CONNECTION_SOURCE_IS_JNDI = 1;
    private static final int CONNECTION_SOURCE_IS_DATA_SOURCE = 2;
    private static final int CONNECTION_SOURCE_IS_DRIVER_MANAGER = 3;
    
    private static Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    
    private static Map dbDefinitionsMap = new HashMap();
    
    private String databaseName, databaseDriver, databaseUrl, databaseUsername, databasePassword, catalogPattern, schemaPattern;
    private int maxPoolSize, databasesAllocated, connectionSourceType = CONNECTION_SOURCE_IS_UNDEFINED;
    private List connectionsList = Collections.synchronizedList(new ArrayList()),
                 databaseFreePool = Collections.synchronizedList(new ArrayList());
    private TableMapping tableMapper;
    private ColumnMapping columnMapper;
    private DataSource dataSource;
    private Map sqlStatements;
    //private License license = new License(500);
    private boolean isClosed;

    static
      {
        Logger logger = Logger.getLogger("jpersist");
        
        logger.setUseParentHandlers(false);
        logger.addHandler(new LoggerUtils.OutputStreamHandler(System.out, true, true));
        logger.setLevel(Level.FINER);
        
        try
          {
            URL url = ClassLoader.getSystemResource("databases.xml");
            boolean validate = false;
            
            if (url == null)
              {
                url = ClassLoader.getSystemResource("databases_v.xml");
                validate = true;
              }
            
            if (url != null)
              new LoadXMLDefinition(url, validate);
          }
        catch (Exception e)
          {
            logger.log(Level.SEVERE, e.toString(), e);
          }
      }

    static class LoadXMLDefinition extends XMLParser
      {
        LoadXMLDefinition(URL fileUrl, boolean validate) throws IOException, XMLParserException
          {
            super(fileUrl, validate);
          }
        
        public void processXML(Element e)
          {
            NodeList nl = e.getElementsByTagName("database");

            for (int i = 0; i < nl.getLength(); i++)
              {
                Element e2 = (Element)nl.item(i);

                String name = StringUtils.emptyToDefault(e2.getAttribute("name"),null),
                       useJndi = StringUtils.emptyToDefault(e2.getAttribute("useJndi"),null),
                       poolSize = StringUtils.emptyToDefault(e2.getAttribute("poolSize"),"10"),
                       driver = StringUtils.emptyToDefault(e2.getAttribute("driver"),null),
                       url = StringUtils.emptyToDefault(e2.getAttribute("url"),null),
                       catalogPattern = StringUtils.emptyToDefault(e2.getAttribute("catalogPattern"),null),
                       schemaPattern = StringUtils.emptyToDefault(e2.getAttribute("schemaPattern"),null),
                       user = StringUtils.emptyToDefault(e2.getAttribute("username"),null),
                       password = StringUtils.emptyToDefault(e2.getAttribute("password"),null);

                DefinedDatabase dd = new DefinedDatabase(name, driver, url, catalogPattern, schemaPattern, user, password, new Integer(poolSize).intValue(), useJndi == null ? false : new Boolean(useJndi).booleanValue());
                dbDefinitionsMap.put(name, dd);
                logger.config(dd.toString());
              }
          }
      }

    static class DefinedDatabase
      {
        String name, driver, url, catalogPattern, schemaPattern, user, password;
        int poolSize;
        boolean useJndi;
        
        DefinedDatabase(String name, String driver, String url, String catalogPattern, String schemaPattern, String user, String password, int poolSize, boolean useJndi)
          {
            this.name = name;
            this.driver = driver;
            this.url = url;
            this.catalogPattern = catalogPattern;
            this.schemaPattern = schemaPattern;
            this.user = user;
            this.password = password;
            this.poolSize = poolSize;
            this.useJndi = useJndi;
          }
        
        String getName() { return name; }
        String getDriver() { return driver; }
        String getUrl() { return url; }
        String getCatalogPattern() { return catalogPattern; }
        String getSchemaPattern() { return schemaPattern; }
        String getUsername() { return user; }
        String getPassword() { return password; }
        int getPoolSize() { return poolSize; }
        boolean useJndi() { return useJndi; }
        
        public String toString()
          {
            if (useJndi)
              return "Name: " + name + ", poolsize = " + poolSize + ", useJndi = true, url = " + url + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern + ", user = " + user + ", password = " + password;
            else
              return "Name: " + name + ", poolsize = " + poolSize + ", driver = " + driver + ", url = " + url + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern + ", user = " + user + ", password = " + password;
          }
      }

    /**
     * Returns a DatabaseManager instance as defined in the databases.xml file.  The format
     * of the databases file, which is found via ClassLoader.getSystemResource(), is:
     * 
     * <pre>
     *     &lt;databases&gt;
     *         &lt;database name="" useJndi="" url="" [poolSize=""] [catalogPattern=""] [schemaPattern=""] [user=""] [password=""] /&gt;
     *         &lt;database name="" driver="" url="" [poolSize=""] [catalogPattern=""] [schemaPattern=""] [user=""] [password=""] /&gt;
     *     &lt;/databases&gt;
     * </pre>
     * 
     * 
     * @param dbName is the name defined in the database element
     * @return an instance os DatabaseManager, or null if not defined
     * @throws JPersistException
     */
    
    public static DatabaseManager getXmlDefinedDatabaseManager(String dbName) throws JPersistException
      {
        DefinedDatabase definedDatabase = (DefinedDatabase)dbDefinitionsMap.get(dbName);
        
        if (logger.isLoggable(Level.FINER))
          if (definedDatabase != null)
            logger.finer("Defined database found: " + definedDatabase);
          else
            logger.finer("Defined database was not found: name = " + dbName);
        
        if (definedDatabase != null)
          {
            if (definedDatabase.useJndi())
              return new DatabaseManager(dbName, definedDatabase.getPoolSize(), definedDatabase.getUrl(), definedDatabase.getCatalogPattern(), definedDatabase.getSchemaPattern());
            else
              {
                if (definedDatabase.getUsername() == null)
                  return new DatabaseManager(dbName, definedDatabase.getPoolSize(), definedDatabase.getDriver(), definedDatabase.getUrl(), definedDatabase.getCatalogPattern(), definedDatabase.getSchemaPattern());
                else
                  return new DatabaseManager(dbName, definedDatabase.getPoolSize(), definedDatabase.getDriver(), definedDatabase.getUrl(), definedDatabase.getCatalogPattern(), definedDatabase.getSchemaPattern(), definedDatabase.getUsername(), definedDatabase.getPassword());
              }
          }
        
        throw new JPersistException("Defined Database '" + dbName + "' not found");
      }

    /**
     * Create a DatabaseManager instance using JNDI.
     * 
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param jndiUri the JNDI URI
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public static DatabaseManager getJndiDefinedDatabaseManager(String databaseName, int poolSize, String jndiUri, String catalogPattern, String schemaPattern)
      {
        return new DatabaseManager(databaseName, poolSize, jndiUri, catalogPattern, schemaPattern);
      }
    
    /**
     * Create a DatabaseManager instance using JNDI.
     * 
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param jndiUri the JNDI URI
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     * @param username the username to use for signon
     * @param password the password to use for signon
     */
    
    public static DatabaseManager getJndiDefinedDatabaseManager(String databaseName, int poolSize, String jndiUri, String catalogPattern, String schemaPattern, String username, String password)
      {
        return new DatabaseManager(databaseName, poolSize, jndiUri, catalogPattern, schemaPattern, username, password);
      }
    
    /**
     * Create a DatabaseManager instance using a supplied database driver.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param driver the database driver class name
     * @param url the driver oriented database url
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public static DatabaseManager getUrlDefinedDatabaseManager(String databaseName, int poolSize, String driver, String url, String catalogPattern, String schemaPattern)
      {
        return new DatabaseManager(databaseName, poolSize, driver, url, catalogPattern, schemaPattern);
      }
    
    /**
     * Create a DatabaseManager instance using a supplied database driver.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param driver the database driver class name
     * @param url the driver oriented database url
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     * @param username the username to use for signon
     * @param password the password to use for signon
     */
    
    public static DatabaseManager getUrlDefinedDatabaseManager(String databaseName, int poolSize, String driver, String url, String catalogPattern, String schemaPattern, String username, String password)
      {
        return new DatabaseManager(databaseName, poolSize, driver, url, catalogPattern, schemaPattern, username, password);
      }
    
    /**
     * Create a DatabaseManager instance using a supplied DataSource.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param dataSource the data source that supplies connections
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public static DatabaseManager getDataSourceDatabaseManager(String databaseName, int poolSize, DataSource dataSource, String catalogPattern, String schemaPattern)
      {
        return new DatabaseManager(databaseName, poolSize, dataSource, catalogPattern, schemaPattern);
      }
    
    /**
     * Create a DatabaseManager instance using a supplied DataSource.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param dataSource the data source that supplies connections
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public DatabaseManager(String databaseName, int poolSize, DataSource dataSource, String catalogPattern, String schemaPattern)
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Creating new DatabaseManager: name = " + databaseName + ", poolSize = " + poolSize + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern);
      
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.dataSource = dataSource;
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
        this.connectionSourceType = CONNECTION_SOURCE_IS_DATA_SOURCE;
      }
    
    /**
     * Create a DatabaseManager instance using JNDI.
     * 
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param jndiUri the JNDI URI
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public DatabaseManager(String databaseName, int poolSize, String jndiUri, String catalogPattern, String schemaPattern)
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Creating new DatabaseManager: name = " + databaseName + ", poolSize = " + poolSize + ", jndiUrl = " + jndiUri + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern);
      
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.databaseUrl = jndiUri;
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
        this.connectionSourceType = CONNECTION_SOURCE_IS_JNDI;
      }
    
    /**
     * Create a DatabaseManager instance using JNDI.
     * 
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param jndiUri the JNDI URI
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     * @param username the username to use for signon
     * @param password the password to use for signon
     */
    
    public DatabaseManager(String databaseName, int poolSize, String jndiUri, String catalogPattern, String schemaPattern, String username, String password)
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Creating new DatabaseManager: name = " + databaseName + ", poolSize = " + poolSize + ", jndiUrl = " + jndiUri
                                                               + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern
                                                               + ", username = " + username + ", password = " + password);
      
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.databaseUrl = jndiUri;
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
        this.databaseUsername = username;
        this.databasePassword = password;
        this.connectionSourceType = CONNECTION_SOURCE_IS_JNDI;
      }
    
    /**
     * Create a DatabaseManager instance using a supplied database driver.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param driver the database driver class name
     * @param url the driver oriented database url
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     */
    
    public DatabaseManager(String databaseName, int poolSize, String driver, String url, String catalogPattern, String schemaPattern)
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Creating new DatabaseManager: name = " + databaseName + ", poolSize = " + poolSize
                                                               + ", driver = " + driver + ", url = " + url
                                                               + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern);
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.databaseDriver = driver;
        this.databaseUrl = url;
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
        this.connectionSourceType = CONNECTION_SOURCE_IS_DRIVER_MANAGER;
      }
    
    /**
     * Create a DatabaseManager instance using a supplied database driver.
     *
     * @param databaseName the name to associate with the DatabaseManager instance
     * @param poolSize the number of instances to manage
     * @param driver the database driver class name
     * @param url the driver oriented database url
     * @param catalogPattern the catalogPattern (can contain SQL wildcards)
     * @param schemaPattern the schemaPattern (can contain SQL wildcards)
     * @param username the username to use for signon
     * @param password the password to use for signon
     */
    
    public DatabaseManager(String databaseName, int poolSize, String driver, String url, String catalogPattern, String schemaPattern, String username, String password)
      {
        if (logger.isLoggable(Level.FINER))
          logger.finer("Creating new DatabaseManager: name = " + databaseName + ", poolSize = " + poolSize + ", driver = " + driver + ", url = " + url 
                                                               + ", catalogPattern = " + catalogPattern + ", schemaPattern = " + schemaPattern
                                                               + ", username = " + username + ", password = " + password);
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.databaseDriver = driver;
        this.databaseUrl = url;
        this.catalogPattern = catalogPattern;
        this.schemaPattern = schemaPattern;
        this.databaseUsername = username;
        this.databasePassword = password;
        this.connectionSourceType = CONNECTION_SOURCE_IS_DRIVER_MANAGER;
      }

    /**
     * Returns the database name.
     * @return the database name
     */
    
    public String getDatabaseName() { return databaseName; }
    
    /**
     * Closes all resources associated with the DatabaseManager.  If the DatabaseManager 
     * is managing pooled connections via JNDI, then this method does nothing.  However, 
     * if the connections are allocated by the manager (non-JNDI), they will be closed.
     */
    public void close() throws JPersistException
      {
        if (!isClosed)
          try
            {
              if (getDatabase().getConnection().getMetaData().getURL().indexOf("hsql") != -1)
                getDatabase().executeUpdate("shutdown;");

              if (connectionSourceType == CONNECTION_SOURCE_IS_DRIVER_MANAGER)
                synchronized(connectionsList)
                  {
                    for (Iterator it = connectionsList.iterator(); it.hasNext();)
                      ((Connection)it.next()).close();
                  }
              
              isClosed = true;
            }
          catch (Exception e)
            {
              throw new JPersistException(e);
            }
      }

    /**
     * Closes the database manager (simply calls close()), can be used in EL. For example:
     * 
     * <pre>
     *     ${dbm.close}
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
     * Closes the database manager (simply calls close()), via a JavaBeans setter.  For example:
     * <pre>
     *     <jsp:useBean id="dbm" scope="request" class="jpersist.DatabaseManager" />
     *     <jsp:setProperty name="dbm" property="closed" value="true"/>
     * </pre>
     * 
     * @throws JPersistException
     */
    
    public void setClosed(boolean true_only) throws JPersistException
      {
        close();
      }
            
    /**
     * Returns true if the database manager is closed, false otherwise.
     *
     * @return true or false
     */
    
    public boolean isClosed() { return isClosed; }

    /**
     * Sets the license key.  The license key is either encoded with a timeout option or it is permanent.
     * 
     * @param licenseKey a generated license key
     * 
     * @throws jwebtk.licensing.LicenseException
     */
    public void setLicense(String licenseKey)// throws LicenseException
      {
/*
        if (licenseKey != null)
          {
            license = new License(licenseKey, "j.*p.*");

            if (license.getCheckSum() != 2323253737L && license.getCheckSum() != 3756392378L)
              throw new LicenseException("Invalid License - Invalid Checksum");
          }

        logger.warning("License: " + license);
*/
      }
      
    /**
     * Sets the logging level to the given level.
     *
     * @param level an instance of java.util.logging.Level
     */
    
    public static void setLogLevel(Level level)
      {
        logger.info("Setting log level to " + level.getName());
        Logger.getLogger("jpersist").setLevel(level);
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
            Database db = getDatabase();

            try
              {
                return db.getMetaData(); 
              }
            finally
              {
                db.close();
              }
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
     * constructors.
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
     * Objects implementing TableMapping and/or ColumnMapping override the global mappers.
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
     * Returns an instance of the defined database.
     *
     * @return an instance of Database
     * @throws JPersistException
     */
    
    public Database getDatabase() throws JPersistException
      {
        try
          {
            Database db = null;

            if (logger.isLoggable(Level.FINER))
              logger.finer("Retrieving database");
            
            if (databaseFreePool.size() > 0)
              {
                db = (Database)databaseFreePool.remove(0);

                if (logger.isLoggable(Level.FINE))
                  logger.fine("Database allocated from free pool");
              }
            else if (databaseFreePool.size() < maxPoolSize)
              {
                db = new Database(this); //, license);
                db.setDatabaseName(databaseName);

                if (connectionSourceType == CONNECTION_SOURCE_IS_DRIVER_MANAGER)
                  {
                    Class.forName(databaseDriver);
                    
                    Connection connection = null;

                    if (databaseUsername != null)
                      connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
                    else
                      connection = DriverManager.getConnection(databaseUrl);
                    
                    db.setConnection(connection);
                    connectionsList.add(connection);
                  }

                if (logger.isLoggable(Level.FINE))
                  logger.fine("Database added to pool; size is now = " + databaseFreePool.size() + ", max size = " + maxPoolSize);
              }
            else throw new JPersistException("Database pool is empty");

            if (connectionSourceType != CONNECTION_SOURCE_IS_DRIVER_MANAGER)
              {
                try
                  {
                    if (connectionSourceType == CONNECTION_SOURCE_IS_JNDI)
                      {
                        if (databaseUsername != null)
                          db.setConnection(((DataSource)new InitialContext().lookup("java:comp/env/" + databaseUrl)).getConnection(databaseUsername,databasePassword));
                        else
                          db.setConnection(((DataSource)new InitialContext().lookup("java:comp/env/" + databaseUrl)).getConnection());
                      }
                    else if (connectionSourceType == CONNECTION_SOURCE_IS_DATA_SOURCE)
                      db.setConnection(dataSource.getConnection());
                    else throw new JPersistException("connectionSourceType = " + connectionSourceType);
                  }
                catch (NamingException ex)
                  {
                    throw new JPersistException("Error for database = " + databaseUrl, ex);
                  }
              }

            if (logger.isLoggable(Level.FINE))
              logger.fine("Databases allocated = " + ++databasesAllocated);

            db.initDatabase();
            db.setMetaDataLimits(catalogPattern, schemaPattern);
            db.setGlobalMappers(tableMapper, columnMapper);

            return db;
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    void releaseDatabase(Database db)
      {
        if (connectionSourceType != CONNECTION_SOURCE_IS_DRIVER_MANAGER)
          try { db.getConnection().close(); } 
          catch (Exception e) 
            {
              logger.log(Level.SEVERE, e.toString(), e);
            }

        databaseFreePool.add(db);

        if (logger.isLoggable(Level.FINE))
          {
            logger.fine("Database added back to free pool; size is now = " + databaseFreePool.size());
            logger.fine("Databases allocated = " + --databasesAllocated);
          }
      }
    
    /**
     * Loads SQL statements based on a supplied query.  The SQL statements are 
     * loaded into a map based on column 1 (id/key column) and column 2 
     * (SQL statement/value column) of your SQL query.
     * 
     * @param sql the SQL statement that loads the statements (e.g. "select sql_id, sql_statement from sql_statements")
     *
     * @throws JPersistException
     */
    
    public void loadSQLStatements(String sql) throws JPersistException
      {
        Database db = getDatabase();

        try
          {
            sqlStatements = ResultSetUtils.loadMap(db.executeQuery(sql).getResultSet(), new HashMap());
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Set the map that sql statements are retrieved from (used in place of loadSQLStatements()).
     *
     * @param sqlStatements the SQL statements map that sql statements are retrieved from
     *
     * @throws JPersistException
     */
    
    public void setSQLStatements(Map sqlStatements) throws JPersistException
      {
        this.sqlStatements = sqlStatements;
      }
    
    /**
     * Returns the SQL statement associated with the SQL Id (key column from loadSQLStatements()).
     * 
     * @param key the sql id
     *
     * @return the SQL statement associated with the key (SQL Id)
     *
     * @throws JPersistException
     */
    
    public String getSQLStatement(String key) throws JPersistException
      {
        if (sqlStatements == null)
          throw new JPersistException("SQL Statements have not been initialized");

        return (String)sqlStatements.get(key);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object);
     * 
     *     if (result.hasNext())
     *       return result.next(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */
    
    public <T> T loadObject(T object) throws JPersistException
      {
        return loadObject(object, true, null, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object);
     * 
     *     if (result.hasNext())
     *       return result.next(object, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param loadAssociations true to load associations
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */
    
    public <T> T loadObject(T object, boolean loadAssociations) throws JPersistException
      {
        return loadObject(object, loadAssociations, null, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, nullValuesToInclude);
     * 
     *     if (result.hasNext())
     *       return result.next(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */
    
    public <T> T loadObject(T object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return loadObject(object, true, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, nullValuesToInclude);
     * 
     *     if (result.hasNext())
     *       return result.next(object, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */
    
    public <T> T loadObject(T object, boolean loadAssociations, Set<String> nullValuesToInclude) throws JPersistException
      {
        return loadObject(object, loadAssociations, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(T object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObject(object, true, null, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next(object, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(T object, boolean loadAssociations, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObject(object, loadAssociations, null, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(T object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObject(object, true, nullValuesToInclude, externalClauses, externalClausesParameters);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next(object, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(T object, boolean loadAssociations, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            Result<T> result = db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
            
            if (result.hasNext())
              return result.next(object, loadAssociations);
          }
        finally
          {
            db.close();
          }
        
        return null;
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(cs);
     * 
     *     if (result.hasNext())
     *       return result.next();
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param cs the class to query and create an object instance from
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(Class<T> cs) throws JPersistException
      {
        return loadObject(cs, true, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(cs);
     * 
     *     if (result.hasNext())
     *       return result.next(loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param cs the class to query and create an object instance from
     * @param loadAssociations true to load associations
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(Class<T> cs, boolean loadAssociations) throws JPersistException
      {
        return loadObject(cs, loadAssociations, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(cs, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next();
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param cs the class to query and create an object instance from
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(Class<T> cs, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObject(cs, true, externalClauses, externalClausesParameters);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     Result result = db.queryObject(cs, externalClauses, externalClausesParameters);
     * 
     *     if (result.hasNext())
     *       return result.next(loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param cs the class to query and create an object instance from
     * @param loadAssociations true to load associations
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return returns the object passed in
     *
     * @throws JPersistException
     */

    public <T> T loadObject(Class<T> cs, boolean loadAssociations, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            Result<T> result = db.queryObject(cs, externalClauses, externalClausesParameters);
            
            if (result.hasNext())
              return result.next(loadAssociations);
          }
        finally
          {
            db.close();
          }
        
        return null;
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object).loadObjects(collection, object.getClass());
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object) throws JPersistException
      {
        return loadObjects(collection, object, true, null, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object).loadObjects(collection, object.getClass(), loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param loadAssociations true to load associations
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, boolean loadAssociations) throws JPersistException
      {
        return loadObjects(collection, object, loadAssociations, null, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, nullValuesToInclude).loadObjects(collection, object.getClass());
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return loadObjects(collection, object, true, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, nullValuesToInclude).loadObjects(collection, object.getClass(), loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, boolean loadAssociations, Set<String> nullValuesToInclude) throws JPersistException
      {
        return loadObjects(collection, object, loadAssociations, nullValuesToInclude, null, (Object[])null);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, externalClauses, externalClausesParameters).loadObjects(collection, object.getClass());
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObjects(collection, object, true, null, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, externalClauses, externalClausesParameters).loadObjects(collection, object.getClass(), loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, boolean loadAssociations, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObjects(collection, object, loadAssociations, null, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters).loadObjects(collection, object.getClass());
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObjects(collection, object, true, nullValuesToInclude, externalClauses, externalClausesParameters);
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads the 
     * object (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters).loadObjects(collection, object.getClass(), loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param object the object to load
     * @param loadAssociations true to load associations
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, T object, boolean loadAssociations, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();

        try
          {
            return db.queryObject(object, nullValuesToInclude, externalClauses, externalClausesParameters).loadObjects(collection, (Class<T>)object.getClass(), loadAssociations);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Builds a select query from a class that matches up to a table, and then loads an  
     * instance (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(cs).loadObjects(cs, collection);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param cs the class to load
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, Class<T> cs) throws JPersistException
      {
        return loadObjects(collection, cs, true, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads an  
     * instance (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(cs).loadObjects(cs, collection, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param cs the class to load
     * @param loadAssociations true to load associations
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */
    
    public <T> Collection<T> loadObjects(Collection<T> collection, Class<T> cs, boolean loadAssociations) throws JPersistException
      {
        return loadObjects(collection, cs, loadAssociations, null, (Object[])null);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads an  
     * instance (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(cs, externalClauses, externalClausesParameters).loadObjects(cs, collection);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param cs the class to load
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */

    public <T> Collection<T> loadObjects(Collection<T> collection, Class<T> cs, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return loadObjects(collection, cs, true, externalClauses, externalClausesParameters);
      }

    /**
     * Builds a select query from a class that matches up to a table, and then loads an  
     * instance (using set methods that match columns in a table matched to the class name) 
     * with the result.
     * 
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     *   
     * try
     *   {
     *     return db.queryObject(cs, externalClauses, externalClausesParameters).loadObjects(cs, collection, loadAssociations);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     *   
     * return collection;
     * </pre>
     * 
     * @param collection an instance of Collection
     * @param cs the class to load
     * @param loadAssociations true to load associations
     * @param externalClauses external clauses, which can begin with a where clause or any clause after the where clause.
     * @param externalClausesParameters the parameters to use with external clauses, can be null (1.5+ can use varargs)
     *
     * @return the Collection that was passed in
     *
     * @throws JPersistException
     */

    public <T> Collection<T> loadObjects(Collection<T> collection, Class<T> cs, boolean loadAssociations, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();

        try
          {
            return db.queryObject(cs, externalClauses, externalClausesParameters).loadObjects(collection, cs, loadAssociations);
          }
        finally
          {
            db.close();
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
        Database db = getDatabase();

        try
          {
            db.loadAssociations(object);
          }
        finally
          {
            db.close();
          }
      }

    /**
     * Builds either an update or an insert depending on whether the object is persistent and was previously loaded, or not, respectively.
     *
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.saveObject(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.saveObject(object, nullValuesToInclude);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the update
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.saveObject(object, externalClauses, externalClausesParameters);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * 
     * @param object the object to load
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.saveObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the update
     * @param externalClauses external clauses beginning with a where or after
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return the number of rows updated
     *
     * @throws JPersistException
     */
    
    public int saveObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return db.saveObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Builds a delete statement from the object.  
     *
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.deleteObject(object);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.deleteObject(object, nullValuesToInclude);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.deleteObject(object, externalClauses, externalClausesParameters);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param externalClauses external clauses
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return the number of rows updated
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
     * <p>This method should not be used within a transaction manager.  Use TransactionManager.getDatabase() instead.
     *
     * <p>This is a one line convienence method for:
     * <pre>
     * Database db = getDatabase();
     * 
     * try
     *   {
     *     return db.deleteObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
     *   }
     * finally
     *   {
     *     db.close();
     *   }
     * </pre>
     * 
     * @param object the object to load
     * @param nullValuesToInclude is a Set of set methods without the 'set', or table column names, to include in the where clause if null
     * @param externalClauses external clauses
     * @param externalClausesParameters the parameters to use with external clauses, can be null
     *
     * @return the number of rows updated
     *
     * @throws JPersistException
     */
    
    public int deleteObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return db.deleteObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Executes a simple query.
     * 
     * @param c a Collection
     * @param sql the SQL statement
     *
     * @return the Collection passed in
     *
     * @throws JPersistException
     */
    
    public Collection<Object[]> executeQuery(Collection<Object[]> c, String sql) throws JPersistException
      {
        return executeQuery(c, false, sql);
      }
    
    /**
     * Executes a simple query.
     * 
     * @param c a Collection
     * @param singleObject use a single object instead of an array for a single column result
     * @param sql the SQL statement
     * @return the Collection passed in
     *
     * @throws JPersistException
     */
    
    public Collection<Object[]> executeQuery(Collection<Object[]> c, boolean singleObject, String sql) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return ResultSetUtils.loadCollection(db.executeQuery(sql).getResultSet(), c, singleObject);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Executes a parameterized query.  A paramterized query allows the use of '?' in 
     * SQL statements.
     * 
     * @param c a Collection
     * @param sql the SQL statement
     * @param parameters objects used to set the parameters to the query
     *
     * @return the Collection passed in
     *
     * @throws JPersistException
     */
    
    public Collection<Object[]> parameterizedQuery(Collection<Object[]> c, String sql, Object... parameters) throws JPersistException
      {
        return parameterizedQuery(c, false, sql);
      }
    
    /**
     * Executes a parameterized query.  A paramterized query allows the use of '?' in 
     * SQL statements.
     * 
     * @param c a Collection
     * @param singleObject use a single object instead of an array for a single column result
     * @param sql the SQL statement
     * @param parameters objects used to set the parameters to the query
     *
     * @return the Collection passed in
     *
     * @throws JPersistException
     */
    
    public Collection<Object[]> parameterizedQuery(Collection<Object[]> c, boolean singleObject, String sql, Object... parameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return ResultSetUtils.loadCollection(db.parameterizedQuery(sql, parameters).getResultSet(), c, singleObject);
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
        finally
          {
            db.close();
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
        Database db = getDatabase();
        
        try
          {
            return db.executeUpdate(sql);
          }
        finally
          {
            db.close();
          }
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
        Database db = getDatabase();
        
        try
          {
            return db.executeUpdate(sql, keys);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Executes a parameterized update.  A paramterized update allows the use of '?' in 
     * SQL statements.
     * 
     * @param sql the SQL statement
     * @param parameters objects used to set the parameters to the update
     *
     * @throws JPersistException
     */
    
    public int parameterizedUpdate(String sql, Object... parameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return db.parameterizedUpdate(sql, parameters);
          }
        finally
          {
            db.close();
          }
      }
    
    /**
     * Executes a parameterized update.  A paramterized update allows the use of '?' in 
     * SQL statements.
     * 
     * @param sql the SQL statement
     * @param keys is a List.  If keys is non-null, then generated keys will be returned in 
     *             the keys List.  List can also define the key columns required 
     *             (depending on the database).
     * @param parameters objects used to set the parameters to the update
     *
     * @throws JPersistException
     */
    
    public int parameterizedUpdate(String sql, List keys, Object... parameters) throws JPersistException
      {
        Database db = getDatabase();
        
        try
          {
            return db.parameterizedUpdate(sql, keys, parameters);
          }
        finally
          {
            db.close();
          }
      }
  }
