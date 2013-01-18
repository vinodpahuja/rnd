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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcommontk.inflector.SimpleInflector;
import jcommontk.utils.StringUtils;
import jpersist.interfaces.ColumnMapping;
import jpersist.interfaces.TableMapping;

/**
 * This class provides database level metadata.
 */

@SuppressWarnings("unchecked")
public final class MetaData
  {
    public static final int STORES_UNKNOWN = 0;
    public static final int STORES_UPPERCASE = 1;
    public static final int STORES_LOWERCASE = 2;
    public static final int STORES_MIXEDCASE = 3;
    
    private static Logger logger = Logger.getLogger(MetaData.class.getName());
    private static ConcurrentHashMap metaDataMap = new ConcurrentHashMap();
    
    private String tableTypes[] = new String[] { "TABLE", "VIEW" }, identifierQuoteString = "", 
                   searchStringEscape = "", databaseUrl;
    private ConcurrentHashMap tables = new ConcurrentHashMap(), tableCache = new ConcurrentHashMap();
    private Map tableNameMapping = Collections.synchronizedMap(new HashMap());
    private Set stripTablePrefixes, stripTableSuffixes, stripColumnPrefixes, stripColumnSuffixes;
    private boolean supportsGeneratedKeys, supportsSavepoints, supportsBatchUpdates, strictClassTableMatching = false, strictMethodColumnMatching = true;
    private int storesCase = 0;

    static MetaData getMetaData(Connection connection) throws SQLException, JPersistException
      {
        String databaseUrl = connection.getMetaData().getURL();
        MetaData metaData = (MetaData)metaDataMap.get(databaseUrl);

        if (metaData != null)
          return metaData;
        
        return loadMetaData(connection);
      }
    
    static MetaData loadMetaData(Connection connection) throws SQLException, JPersistException
      {
        String databaseUrl = connection.getMetaData().getURL();
        MetaData metaData = (MetaData)metaDataMap.get(databaseUrl);

        if (metaData == null)
          {
            metaData = new MetaData();
            DatabaseMetaData dbMetaData = connection.getMetaData();

            try
              {
                logger.finer("database product name = " + dbMetaData.getDatabaseProductName());
                logger.finer("database product version = " + dbMetaData.getDatabaseProductVersion());
                logger.finer("database version = " + dbMetaData.getDatabaseMajorVersion() + "." + dbMetaData.getDatabaseMinorVersion());
                logger.finer("JDBC driver version = " + dbMetaData.getDriverMajorVersion() + "." +  + dbMetaData.getDriverMinorVersion());
                logger.finer("user name = " + dbMetaData.getUserName());
                logger.finer("supports transactions = " + dbMetaData.supportsTransactions());
                logger.finer("supports multiple transactions = " + dbMetaData.supportsMultipleTransactions());
                logger.finer("supports transaction isolation level TRANSACTION_READ_COMMITTED = " + dbMetaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED));
                logger.finer("supports transaction isolation level TRANSACTION_READ_UNCOMMITTED = " + dbMetaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED));
                logger.finer("supports transaction isolation level TRANSACTION_REPEATABLE_READ = " + dbMetaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ));
                logger.finer("supports transaction isolation level TRANSACTION_SERIALIZABLE = " + dbMetaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE));
                logger.finer("supports result set TYPE_FORWARD_ONLY = " + dbMetaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
                logger.finer("supports result set TYPE_SCROLL_INSENSITIVE = " + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
                logger.finer("supports result set TYPE_SCROLL_SENSITIVE = " + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));
                logger.finer("supports result set holdability CLOSE_CURSORS_AT_COMMIT = " + dbMetaData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
                logger.finer("supports result set holdability HOLD_CURSORS_OVER_COMMIT = " + dbMetaData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT));
                logger.finer("stores lower case identifiers = " + dbMetaData.storesLowerCaseIdentifiers());
                logger.finer("stores lower case quoted identifiers = " + dbMetaData.storesLowerCaseQuotedIdentifiers());
                logger.finer("stores upper case identifiers = " + dbMetaData.storesUpperCaseIdentifiers());
                logger.finer("stores upper case quoted identifiers = " + dbMetaData.storesUpperCaseQuotedIdentifiers());
                logger.finer("stores mixed case identifiers = " + dbMetaData.storesMixedCaseIdentifiers());
                logger.finer("stores mixed case quoted identifiers = " + dbMetaData.storesMixedCaseQuotedIdentifiers());
              }
            catch (Exception e)
              {
                logger.log(Level.WARNING,e.getMessage(),e);
              }
            
            logger.finer("Catalog term = " + dbMetaData.getCatalogTerm());
            logger.finer("Schema term = " + dbMetaData.getSchemaTerm());
            
            try
              {
                if (dbMetaData.supportsSavepoints())
                  {
                    Savepoint savepoint = connection.setSavepoint();
                    connection.releaseSavepoint(savepoint);
                  }
                
                metaData.supportsSavepoints = dbMetaData.supportsSavepoints();
              }
            catch (Exception e)
              {
                logger.log(Level.FINE, "The database metadata reports it supports savepoints, but the database fails with setSavepoint().  Therefore, the database probably does not support savepoints");
              }
            
            logger.finer("supports savepoints = " + metaData.supportsSavepoints);
            
            metaData.supportsBatchUpdates = dbMetaData.supportsBatchUpdates();
            
            if (dbMetaData.storesLowerCaseIdentifiers() || dbMetaData.storesLowerCaseQuotedIdentifiers())
              metaData.storesCase = STORES_LOWERCASE;
            else if (dbMetaData.storesUpperCaseIdentifiers() || dbMetaData.storesUpperCaseQuotedIdentifiers())
              metaData.storesCase = STORES_UPPERCASE;
            else if (dbMetaData.storesMixedCaseIdentifiers() || dbMetaData.storesMixedCaseQuotedIdentifiers())
              metaData.storesCase = STORES_MIXEDCASE;

            logger.finer("maximum concurrent connections = " + dbMetaData.getMaxConnections());
            
            metaData.identifierQuoteString = dbMetaData.getIdentifierQuoteString();
            
            if (metaData.identifierQuoteString.equals(" "))
              metaData.identifierQuoteString = "";
              
            logger.finer("identifier quote string = '" + metaData.identifierQuoteString + "'");
            logger.finer("supports generated keys = " + (metaData.supportsGeneratedKeys = dbMetaData.supportsGetGeneratedKeys()));
            logger.finer("search string escape = " + (metaData.searchStringEscape = dbMetaData.getSearchStringEscape()));
            logger.finer("database url = " + (metaData.databaseUrl = databaseUrl));

            if (metaDataMap.putIfAbsent(databaseUrl, metaData) != null)
              metaData = (MetaData)metaDataMap.get(databaseUrl);
          }

        return metaData;
      }

    /**
     * Returns the identifier quote string ("'", etc).
     * 
     * @return the identifier quote string ("'", etc).
     */
    public String getIdentifierQuoteString() { return identifierQuoteString; }

    /**
     * Returns the search string escape string.
     * 
     * @return the search string escape string.
     */
    public String getSearchStringEscape() { return searchStringEscape; }

    /**
     * Returns the JDBC URL.
     * 
     * @return the JDBC URL
     */
    public String getDatabaseUrl() { return databaseUrl; }

    /**
     * Returns the method for storing case with table names and column names.
     * 
     * @return one of STORES_UNKNOWN, STORES_UPPERCASE, STORES_LOWERCASE, STORES_MIXEDCASE
     */
    public int getStoresCase() { return storesCase; }

    /**
     * Returns true if the JDBC driver supports generated keys via parameters 
     * to one of the various update methods.
     * 
     * @return true if the JDBC driver supports generated keys
     */
    public boolean supportsGeneratedKeys() { return supportsGeneratedKeys; }

    /**
     * Returns true if the JDBC driver supports save points.
     * @return true if the JDBC driver supports save points
     */
    public boolean supportsSavepoints() { return supportsSavepoints; }

    /**
     * Returns true if the JDBC driver supports batch updates.
     * @return true if the JDBC driver supports batch updates
     */
    public boolean supportsBatchUpdates() { return supportsBatchUpdates; }

    /**
     * Set the table types to search for (default is "TABLE" and "VIEW").
     * @param tableTypes an array of table types (see JDBC javadoc)
     */
    public void setTableTypes(String[] tableTypes)
      {
        this.tableTypes = tableTypes;
      }

    /**
     * Use strict method/column matching (default is true).  If true method 
     * property names and column names have to match exactly (less camelcase 
     * and underlines).  If false the method name can exist in the column name
     * as a substring.
     * 
     * @param trueFalse true to use strict method/column matching
     */
    public void setStrictMethodColumnMatching(boolean trueFalse) { strictMethodColumnMatching = trueFalse; }

    /**
     * Use strict class/table name matching (default is false).  If true class 
     * names and table names have to match exactly (less camelcase 
     * and underlines).  If false the class name can exist in the column name
     * as a substring.  Several steps are performed in trying to match class 
     * names to table names.
     * 
     * @param trueFalse true to use strict class/table matching
     */
    public void setStrictClassTableMatching(boolean trueFalse) { strictClassTableMatching = trueFalse; }
    
    /**
     * Set of prefixes to be stripped from table names to help in class to table name matching.
     * @param stripTablePrefixes a set of prefixes (Strings)
     */
    public void setTablePrefixesToStrip(Set stripTablePrefixes) { this.stripTablePrefixes = stripTablePrefixes; }

    /**
     * Set of suffixes to be stripped from table names to help in class to table name matching.
     * @param stripTableSuffixes a set of suffixes (Strings)
     */
    public void setTableSuffixesToStrip(Set stripTableSuffixes) { this.stripTableSuffixes = stripTableSuffixes; }

    /**
     * Set of prefixes to be stripped from column names to help in method to column name matching.
     * @param stripColumnPrefixes a set of prefixes (Strings)
     */
    public void setColumnPrefixesToStrip(Set stripColumnPrefixes) { this.stripColumnPrefixes = stripColumnPrefixes; }

    /**
     * Set of suffixes to be stripped from column names to help in method to column name matching.
     * @param stripColumnSuffixes a set of suffixes (Strings)
     */
    public void setColumnSuffixesToStrip(Set stripColumnSuffixes) { this.stripColumnSuffixes = stripColumnSuffixes; }
        
    /**
     * Add a table mapping.
     *
     * @param searchTableName the name that should be matched
     * @param returnTableName the actual table name in the database
     */
    public void addTableNameMapping(String searchTableName, String returnTableName) 
      {
        tableNameMapping.put(normalizeName(searchTableName), returnTableName);
      }

    /**
     * Returns the table metadata for a given table name.
     * 
     * @param connection JDBC connection
     * @param tableMapper a TableMapper instance (can be null)
     * @param columnMapper ColumnMapper instance (can be null)
     * @param catalogPattern the catalog (can be null)
     * @param schemaPattern the schema (can be null)
     * @param tableName the table name to search for
     * @param object object that can have added annotations and/or interfaces (can be null)
     * 
     * @return a Table metadata instance
     * 
     * @throws java.sql.SQLException
     * @throws jpersist.JPersistException
     */
    public Table getTable(Connection connection, TableMapping tableMapper, ColumnMapping columnMapper, String catalogPattern, String schemaPattern, String tableName, Object object) throws SQLException, JPersistException
      {
        String searchName = normalizeName((catalogPattern != null ? catalogPattern + "." : "")
                          + (schemaPattern != null ? schemaPattern + "." : "")
                          + tableName);
        Table table = (Table)tableCache.get(searchName);

        // table already loaded
        if (table == null)
          {
            if ((table = tableSearch(connection, tableMapper, catalogPattern, schemaPattern, tableName, object, true)) == null)
              table = tableSearch(connection, tableMapper, catalogPattern, schemaPattern, tableName, object, false);

            if (!(table instanceof NullTable) && !table.isTableDetailLoaded())
              loadTableDetail(connection, columnMapper, table);
          }
        
        if (table instanceof NullTable)
          return null;
        else
          return table;
      }
    
    /* load all possiblities and scan for an exact match, or a single match */
    Table tableSearch(Connection connection, TableMapping tableMapper, String catalogPattern, String schemaPattern, String tableName, Object object, boolean exactMatch) throws SQLException, JPersistException
      {
        String searchName = normalizeName((catalogPattern != null ? catalogPattern + "." : "")
                          + (schemaPattern != null ? schemaPattern + "." : "")
                          + tableName);
        Table table = (Table)tableCache.get(searchName);
        
        // table already loaded
        if (table != null)
          return table;
        
        String name = null, catalog = null, schema = null;

        if (logger.isLoggable(Level.FINER))
          logger.finer("Searching for table " + tableName);

        if (catalogPattern != null)
          catalog = storesCase == 0 || storesCase == STORES_UPPERCASE ? catalogPattern.toUpperCase() : catalogPattern.toLowerCase();

        if (schemaPattern != null)
          schema = storesCase == 0 || storesCase == STORES_UPPERCASE ? schemaPattern.toUpperCase() : schemaPattern.toLowerCase();

        // search and load Tabble annotation
        if (object != null)
          {
            Class cs = object.getClass();

            if (cs.isAnnotationPresent(jpersist.annotations.Table.class))
              {
                name = ((jpersist.annotations.Table)cs.getAnnotation(jpersist.annotations.Table.class)).tableName();

                loadTables(connection, catalog, schema, name, exactMatch);
                table = tableScan(catalog, schema, name, true);
              }
          }
        
        // search and load TabbleMapping defined
        if (table == null && object != null && object instanceof TableMapping && (name = ((TableMapping)object).getDatabaseTableName(tableName.toLowerCase())) != null)
          {
            name = storesCase == 0 || storesCase == STORES_UPPERCASE ? name.toUpperCase() : name.toLowerCase();
            
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }
        
        // search and load global TabbleMapping defined
        if (table == null && tableMapper != null && (name = tableMapper.getDatabaseTableName(tableName.toLowerCase())) != null)
          {
            name = storesCase == 0 || storesCase == STORES_UPPERCASE ? name.toUpperCase() : name.toLowerCase();
            
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }
        
        // search and load table hints
        if (table == null && (name = (String)tableNameMapping.get(normalizeName(tableName))) != null)
          {
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }

        // search and load for tableName
        if (table == null)
          {
            loadTables(connection, catalogPattern, schemaPattern, name = tableName, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }
        
        // search and load plurals
        if (table == null)
          {
            String[] plurals = SimpleInflector.pluralize(tableName);

            for (int i = 0; table == null && i < plurals.length; i++)
              {
                loadTables(connection, catalog, schema, name = plurals[i], exactMatch);
                table = tableScan(catalog, schema, name, true);
              }
          }
        
        // search and load for TABLENAME
        if (table == null)
          {
            name = storesCase == 0 || storesCase == STORES_UPPERCASE ? tableName.toUpperCase() : tableName.toLowerCase();
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }
        
        // search and load for TABLE_NAME
        if (table == null)
          {
            name = storesCase == 0 || storesCase == STORES_UPPERCASE ? StringUtils.camelCaseToUpperCaseUnderline(tableName) : StringUtils.camelCaseToLowerCaseUnderline(tableName);
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);
          }
        
        // search and load for opposite of TABLE_NAME
        if (table == null)
          {
            if (catalogPattern != null)
              catalog = storesCase == 0 || storesCase == STORES_UPPERCASE ? catalogPattern.toLowerCase() : catalogPattern.toUpperCase();

            if (schemaPattern != null)
              schema = storesCase == 0 || storesCase == STORES_UPPERCASE ? schemaPattern.toLowerCase() : schemaPattern.toUpperCase();

            // search and load for tablename
            name = storesCase == 0 || storesCase == STORES_UPPERCASE ? tableName.toLowerCase() : tableName.toUpperCase();
            loadTables(connection, catalog, schema, name, exactMatch);
            table = tableScan(catalog, schema, name, true);

            // search and load for table_name
            if (table == null)
              {
                name = storesCase == 0 || storesCase == STORES_UPPERCASE ? StringUtils.camelCaseToLowerCaseUnderline(tableName) : StringUtils.camelCaseToUpperCaseUnderline(tableName);
                loadTables(connection, catalog, schema, name, exactMatch);
                table = tableScan(catalog, schema, name, true);
              }
          }

        if (table == null && !exactMatch && !strictClassTableMatching)
          table = tableScan(catalog, schema, name, false);

        if (table != null)
          {
            if (tableCache.putIfAbsent(searchName, table) != null)
              table = (Table)tableCache.get(searchName);
          }
        else if (!exactMatch)
          {
            tableCache.put(searchName, table = new NullTable());

            if (logger.isLoggable(Level.FINER))
              logger.finer("Table " + tableName + " not found!");
          }

        return table;
      }

    int loadTables(Connection connection, String catalogPattern, String schemaPattern, String tablePattern, boolean exactMatch) throws SQLException, JPersistException
      {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getTables(catalogPattern, schemaPattern, 
                                                 exactMatch ? tablePattern : "%" + tablePattern + "%", 
                                                 tableTypes);
        int tableCount = 0;

        while (resultSet.next())
          {
            Table table = new Table(resultSet.getString("table_name"),
                                    resultSet.getString("table_cat"),
                                    resultSet.getString("table_schem"),
                                    resultSet.getString("table_type"));

            String searchName = normalizeName((table.getCatalogName() != null ? table.getCatalogName() + "." : "")
                              + (table.getSchemaName() != null ? table.getSchemaName() + "." : "")
                              + table.getTableName());

            tables.putIfAbsent(searchName, table);

            if (logger.isLoggable(Level.FINE))
              logger.finer("Found table: " + table);
            
            tableCount++;
          }

        resultSet.close();
        
        return tableCount;
      }

    Table tableScan(String catalogName, String schemaName, String tableName, boolean strictMatch) throws JPersistException 
      {
        String searchName = normalizeName((catalogName != null ? catalogName + "." : "")
                          + (schemaName != null ? schemaName + "." : "")
                          + tableName);
        Table table = (Table)tables.get(searchName);
       
        if (table != null)
          return table;

        tableName = normalizeName(tableName);
        
        catalogName = catalogName != null ? catalogName.toLowerCase() : "";
        schemaName = schemaName != null ? schemaName.toLowerCase() : "";

        Iterator it = tables.entrySet().iterator();

        while (it.hasNext())
          {
            Table itTable = (Table)((Map.Entry)it.next()).getValue();
            String matchName1 = normalizeName(itTable.getTableName()), matchName2 = null,
                   itTableCatalog = itTable.getCatalogName() != null ? itTable.getCatalogName().toLowerCase() : "",
                   itTableSchema = itTable.getSchemaName() != null ? itTable.getSchemaName().toLowerCase() : "";

            if (stripTablePrefixes != null || stripTableSuffixes != null)
              matchName2 = normalizeName(stripName(itTable.getTableName(), stripTablePrefixes, stripTableSuffixes));

            if ((tableName.equals(matchName1) || (matchName2 != null && tableName.equals(matchName2)))
                      && (catalogName.length() == 0 || catalogName.equals(itTableCatalog))
                      && (schemaName.length() == 0 || schemaName.equals(itTableSchema)))
              {
                if (table == null)
                  table = itTable;
                else
                  throw new JPersistException("Scanning produces multiple possible tables for table name '" + tableName + "'\n"
                                            + "To obtain an exact match you can further qualify the naming, or add catalog/schema qualifiers,\n or define prefix/suffix stripping, or table name mapping.");
              }
          }

        if (!strictMatch && table == null)
          {
            it = tables.entrySet().iterator();
            
            while (it.hasNext())
              {
                Table itTable = (Table)((Map.Entry)it.next()).getValue();
                String matchName1 = normalizeName(itTable.getTableName()),
                       itTableCatalog = itTable.getCatalogName() != null ? itTable.getCatalogName().toLowerCase() : "",
                       itTableSchema = itTable.getSchemaName() != null ? itTable.getSchemaName().toLowerCase() : "";
                
                if (matchName1.indexOf(tableName) != -1
                          && (catalogName.length() == 0 || catalogName.equals(itTableCatalog))
                          && (schemaName.length() == 0 || schemaName.equals(itTableSchema)))
                  {
                    if (table == null || matchName1.equals(tableName))
                      table = itTable;
                    else
                      throw new JPersistException("Scanning produces multiple possible tables for table name '" + tableName + "'\n"
                                                + "To obtain an exact match you can further qualify the naming, or add catalog/schema qualifiers,\n or define prefix/suffix stripping, or table name mapping.");
                  }
              }
          }
        
        return table;
      }

    void loadTableDetail(Connection connection, ColumnMapping columnMapper, Table table) throws SQLException, JPersistException
      {
        Statement statement = connection.createStatement();
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getPrimaryKeys(table.getCatalogName(), table.getSchemaName(), table.getTableName());

        Map primaryKeys = new HashMap();

        while (resultSet.next())
          {
            String name = resultSet.getString("column_name");
            primaryKeys.put(name, table.new Key(name, resultSet.getString("table_name"), resultSet.getString("table_cat"), resultSet.getString("table_schem")));
          }

        table.setPrimaryKeys(primaryKeys);

        resultSet.close();

        resultSet = metaData.getBestRowIdentifier(table.getCatalogName(), table.getSchemaName(), table.getTableName(), DatabaseMetaData.bestRowSession, true);

        Set bestRowIds = new HashSet();

        while (resultSet.next())
          bestRowIds.add(resultSet.getString("column_name"));

        table.setBestRowIds(bestRowIds);

        resultSet.close();

        resultSet = metaData.getImportedKeys(table.getCatalogName(), table.getSchemaName(), table.getTableName());

        Map importedKeys = new HashMap();

        while (resultSet.next())
          {
            String name = resultSet.getString("fkcolumn_name");
            importedKeys.put(name, table.new Key(name, resultSet.getString("fktable_cat"), resultSet.getString("fktable_schem"), resultSet.getString("fktable_name"),
                             resultSet.getString("pkcolumn_name"), resultSet.getString("pktable_cat"), resultSet.getString("pktable_schem"), resultSet.getString("pktable_name")));
          }

        table.setImportedKeys(importedKeys);

        resultSet.close();

        resultSet = metaData.getExportedKeys(table.getCatalogName(), table.getSchemaName(), table.getTableName());

        Map exportedKeys = new HashMap();

        while (resultSet.next())
          {
            String name = resultSet.getString("pkcolumn_name");
            exportedKeys.put(name, table.new Key(name, resultSet.getString("pktable_cat"), resultSet.getString("pktable_schem"), resultSet.getString("pktable_name"),
                             resultSet.getString("fkcolumn_name"), resultSet.getString("fktable_cat"), resultSet.getString("fktable_schem"), resultSet.getString("fktable_name")));
          }

        table.setExportedKeys(exportedKeys);

        resultSet.close();

        resultSet = metaData.getColumns(table.getCatalogName(), table.getSchemaName(), table.getTableName(), null);

        Map columns = new HashMap();

        while (resultSet.next())
          {
            String columnName = resultSet.getString("COLUMN_NAME");

            Table.Column column = table.new Column(columnName,
                                                   resultSet.getString("TYPE_NAME"),
                                                   resultSet.getInt("DATA_TYPE"),
                                                   resultSet.getInt("COLUMN_SIZE"),
                                                   resultSet.getInt("DECIMAL_DIGITS"),
                                                   resultSet.getInt("NUM_PREC_RADIX"),
                                                   resultSet.getString("IS_NULLABLE").equalsIgnoreCase("Yes") ? true : false,
                                                   primaryKeys.get(columnName) != null,
                                                   bestRowIds.contains(columnName));

            columns.put(normalizeName(columnName), column);
          }

        table.setColumns(columns);

        resultSet.close();

        if ((resultSet = statement.executeQuery("select * from [" + table.getTableName() + "] where 1 = 0")) != null)
          {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            if (resultSetMetaData != null)
              for (int i = 0; i < resultSetMetaData.getColumnCount(); i++)
                {
                  Table.Column column = table.getColumn(normalizeName(resultSetMetaData.getColumnName(i+1)));

                  if (column != null)
                    {
                      column.setAdditionalInfo(resultSetMetaData.getColumnLabel(i+1),
                                               resultSetMetaData.getColumnClassName(i+1),
                                               resultSetMetaData.isAutoIncrement(i+1),
                                               resultSetMetaData.isReadOnly(i+1),
                                               resultSetMetaData.isSearchable(i+1));

                      if (column.isAutoIncrement())
                        table.setGeneratedKey(column.getColumnName());
                    }
                }

            resultSet.close();
          }
        
        table.setTableDetailLoaded(true);
      }
    
    /**
     * The main table metadata.
     */
    public class Table
      {
        private String tableName, catalogName, schemaName, type, generatedKey;
        private ConcurrentHashMap columnCache = new ConcurrentHashMap();
        private Map primaryKeys, exportedKeys, importedKeys, columns, 
                    columnNameMapping = Collections.synchronizedMap(new HashMap());
        private boolean isTableDetailLoaded;
        private Set bestRowIds;

        Table() { }
        Table(String tableName, String catalogName, String schemaName, String type)
          {
            this.tableName = tableName;
            this.catalogName = catalogName;
            this.schemaName = schemaName;
            this.type = type;
          }

        /**
         * Returns the table name.
         * 
         * @return the table name
         */
        public String getTableName() { return tableName; }
        
        /**
         * Returns the catalog name.
         * 
         * @return the catalog name
         */
        public String getCatalogName() { return catalogName; }
        
        /**
         * Returns the schema name.
         * 
         * @return the schema name
         */
        public String getSchemaName() { return schemaName; }
        
        /**
         * The type of table as defined by setTableTypes().
         * 
         * @return a valid table type name.
         */
        public String getType() { return type; }

        public String toString() { return "catalog = " + catalogName + ", schema = " + schemaName + ", table = " + tableName; }
        
        boolean isTableDetailLoaded() { return isTableDetailLoaded; }
        void setTableDetailLoaded(boolean isTableDetailLoaded) { this.isTableDetailLoaded = isTableDetailLoaded; }
        
        /**
         * May return a set of columns that can best be used for row identifiers.
         * 
         * @return a set of columns that can best be used for row identifiers.
         */
        public Set getBestRowIds() { return bestRowIds; }
        void setBestRowIds(Set bestRowIds) { this.bestRowIds = bestRowIds; }

        /**
         * Returns the column representing a generated key.
         * 
         * @return the column representing a generated key
         */
        public String getGeneratedKey() { return generatedKey; }
        void setGeneratedKey(String generatedKey) { this.generatedKey = generatedKey; }
        
        void setColumns(Map columns)
          {
            this.columns = columns;
          }
        
        /**
         * Returns the column representing a generated key.
         * 
         * @return the column representing a generated key
         */
        public String getPossibleGeneratedKey() 
          {
            if (generatedKey != null)
              return generatedKey;

            Column lastColumnMatch = null;
            
            for (Iterator it = primaryKeys.keySet().iterator(); it.hasNext();)
              {
                String key = (String)it.next();
                Column column = (Column)columns.get(normalizeName(key));
                
                if (importedKeys.get(key) == null)
                  lastColumnMatch = column;
              }
            
            if (lastColumnMatch != null)
              return lastColumnMatch.getColumnName();
            
            return null;
          }

        /**
         * Returns a map of Table.Key instances representing the primary key(s).
         * 
         * @return a map of Table.Key instances representing the primary key(s)
         */
        public Map getPrimaryKeys() { return primaryKeys; }
        void setPrimaryKeys(Map primaryKeys) { this.primaryKeys = primaryKeys; }
        
        /**
         * Returns a map of Table.Key instances representing the exported key(s).
         * 
         * @return a map of Table.Key instances representing the exported key(s)
         */
        public Map getExportedKeys() { return exportedKeys; }
        void setExportedKeys(Map exportedKeys) { this.exportedKeys = exportedKeys; }
        
        /**
         * Returns a map of Table.Key instances representing the imported key(s).
         * 
         * @return a map of Table.Key instances representing the imported key(s)
         */
        public Map getImportedKeys() { return importedKeys; }
        void setImportedKeys(Map importedKeys) { this.importedKeys = importedKeys; }

        /**
         * Add a method property to column name mapping.
         * 
         * @param searchColumnName the search name
         * @param returnColumnName the name to return
         */
        public void addColumnNameMapping(String searchColumnName, String returnColumnName)
          {
            columnNameMapping.put(normalizeName(searchColumnName), returnColumnName);
          }

        /**
         * Returns a Table.Column instance.
         * 
         * @param columnName the column to return
         * 
         * @return a Table.Column instance
         * 
         * @throws jpersist.JPersistException
         */
        public Column getColumn(String columnName) throws JPersistException
          {
            return (Column)columns.get(normalizeName(columnName));
          }
        
        /**
         * Returns a Table.Column instance.
         * 
         * @param columnMapper a ColumnMapper instance
         * @param columnName the column to return
         * @param object an object that may have added annotations and/or interfaces
         * 
         * @return a Table.Column instance
         * 
         * @throws jpersist.JPersistException
         */
        public Column getColumn(ColumnMapping columnMapper, String columnName, Object object) throws JPersistException
          {
            Column column = (Column)columnCache.get(normalizeName(columnName));
            
            if (column != null)
              {
                if (column instanceof NullColumn)
                  return null;

                return column;
              }
            
            return columnSearch(columnMapper, columnName, object);
          }
        
        Column columnSearch(ColumnMapping columnMapper, String columnName, Object object) throws JPersistException
          {
            String normalizedColumnName = normalizeName(columnName), name = null;
            Column column = (Column)columnCache.get(normalizedColumnName);
            Method method = null;
            
            if (column != null)
              return column;

            if (object != null)
              try
                {
                  method = object.getClass().getMethod("get" + columnName, new Class[0]);
                }
              catch (Exception e) {}
            
            if (method != null && method.isAnnotationPresent(jpersist.annotations.Ignore.class))
              column = null;
            else if (method != null && method.isAnnotationPresent(jpersist.annotations.Column.class) && (name = ((jpersist.annotations.Column)method.getAnnotation(jpersist.annotations.Column.class)).tableColumnName()) != null)
              column = (Column)columns.get(normalizeName(name));
            else if (object != null && object instanceof ColumnMapping && (name = ((ColumnMapping)object).getTableColumnName(columnName.toLowerCase())) != null)
              column = (Column)columns.get(normalizeName(name));
            else if (columnMapper != null && (name = columnMapper.getTableColumnName(columnName.toLowerCase())) != null)
              column = (Column)columns.get(normalizeName(name));
            else if ((name = (String)columnNameMapping.get(normalizedColumnName)) != null)
              column = (Column)columns.get(normalizeName(name));
            else column = (Column)columns.get(normalizeName(columnName));
              
            if (column == null && (stripColumnPrefixes != null || stripColumnSuffixes != null))
              {
                Iterator it = columns.entrySet().iterator();

                while (it.hasNext())
                  {
                    Column itColumn = (Column)((Map.Entry)it.next()).getValue();
                    String strippedName = normalizeName(stripName(itColumn.getColumnName(),stripColumnPrefixes,stripColumnSuffixes));

                    if (columnName.equals(strippedName))
                      {
                        if (column == null)
                          column = itColumn;
                        else
                          throw new JPersistException("Scanning produces multiple possible columns for column name '" + columnName + "' found in table '" + getTableName() + "'\n"
                                                    + "To obtain an exact match you can further qualify the naming, or define prefix/suffix stripping, or column name mapping.");
                      }
                  }
              }

            if (column == null && !strictMethodColumnMatching)
              {
                Iterator it = columns.keySet().iterator();

                while (it.hasNext())
                  {
                    name = (String)it.next();

                    if (name.indexOf(normalizedColumnName) != -1)
                      {
                        if (column == null || name.equals(normalizedColumnName))
                          column = (Column)columns.get(name);
                        else
                          throw new JPersistException("Scanning produces multiple possible columns for column name '" + columnName + "' found in table '" + getTableName() + "'\n"
                                                    + "To obtain an exact match you can further qualify the naming, or define prefix/suffix stripping, or column name mapping.");
                      }
                  }
              }

            if (column == null)
              column = new NullColumn();
            
            if (columnCache.putIfAbsent(normalizedColumnName, column) != null)
              column = (Column)columnCache.get(normalizedColumnName);

            if (column instanceof NullColumn)
              {
                if (logger.isLoggable(Level.FINER) && !columnName.equals("dbAssociation"))
                  logger.finer("Column " + columnName + " not matched or ignored!");
                
                return null;
              }
            
            return column;
          }

        /**
         * The Table.Column metadata.
         */
        public class Column
          {
            private String columnName, columnLabel, typeName, className;
            private int dataType, columnSize, decimalDigits, radix;
            private boolean isNullable, isPrimaryKey, isRowId, isAutoIncrement, isReadOnly, isSearchable;

            Column() {}
            Column(String columnName, String typeName, int dataType, int columnSize, int decimalDigits, int radix, boolean isNullable, boolean isPrimaryKey, boolean isRowId)
              {
                this.columnName = columnName;
                this.typeName = typeName;
                this.dataType = dataType;
                this.columnSize = columnSize;
                this.decimalDigits = decimalDigits;
                this.radix = radix;
                this.isNullable = isNullable;
                this.isPrimaryKey = isPrimaryKey;
                this.isRowId = isRowId;
              }

            void setAdditionalInfo(String columnLabel, String className, boolean isAutoIncrement, boolean isReadOnly, boolean isSearchable)
              {
                this.columnLabel = columnLabel;
                this.className = className;
                this.isAutoIncrement = isAutoIncrement;
                //this.isReadOnly = isReadOnly;
                this.isSearchable = isSearchable;
              }
            
            public String getColumnName() { return columnName; }
            public String getColumnLabel() { return columnLabel; }
            public String getClassName() { return className; }
            public String getTypeName() { return typeName; }
            public int getDataType() { return dataType; }
            public int getColumnSize() { return columnSize; }
            public int getDecimalDigits() { return decimalDigits; }
            public int getRadix() { return radix; }
            public boolean isNullable() { return isNullable; }
            public boolean isPrimaryKey() { return isPrimaryKey; }
            public boolean isRowId() { return isRowId; }
            public boolean isAutoIncrement() { return isAutoIncrement; }
            public boolean isReadOnly() { return isReadOnly || isAutoIncrement; }
            public boolean isSearchable() { return isSearchable; }
          }
        
        class NullColumn extends Column { }

        public class Key
          {
            private String localColumnName, localTableCatalog, localTableSchema, localTableName,
                           foreignColumnName, foreignTableCatalog, foreignTableSchema, foreignTableName;

            Key(String localColumnName, String localTableCatalog, String localTableSchema, String localTableName)
              {
                this.localColumnName = localColumnName;
                this.localTableCatalog = localTableCatalog;
                this.localTableSchema = localTableSchema;
                this.localTableName = localTableName;
              }
            
            Key(String localColumnName, String localTableCatalog, String localTableSchema, String localTableName,
                String foreignColumnName, String foreignTableCatalog, String foreignTableSchema, String foreignTableName)
              {
                this.localColumnName = localColumnName;
                this.localTableCatalog = localTableCatalog;
                this.localTableSchema = localTableSchema;
                this.localTableName = localTableName;
                this.foreignColumnName = foreignColumnName;
                this.foreignTableCatalog = foreignTableCatalog;
                this.foreignTableSchema = foreignTableSchema;
                this.foreignTableName = foreignTableName;
              }
            
            public String getForeignColumnName() { return foreignColumnName; }
            public String getForeignTableCatalog() { return foreignTableCatalog; }
            public String getForeignTableSchema() { return foreignTableSchema; }
            public String getForeignTableName() { return foreignTableName; }
            public String getLocalColumnName() { return localColumnName; }
            public String getLocalTableCatalog() { return localTableCatalog; }
            public String getLocalTableSchema() { return localTableSchema; }
            public String getLocalTableName() { return localTableName; }
          }
      }
    
    class NullTable extends Table { }
    
    static String stripName(String name, Set prefixes, Set suffixes)
      {
        if (prefixes != null)
          for (Iterator it = prefixes.iterator(); it.hasNext();)
            {
              String prefix = (String)it.next();
              
              if (name.startsWith(prefix))
                {
                  name = name.substring(prefix.length());
                  break;
                }
            }
        
        if (suffixes != null)
          for (Iterator it = suffixes.iterator(); it.hasNext();)
            {
              String suffix = (String)it.next();
              
              if (name.endsWith(suffix))
                {
                  name = name.substring(0, name.length() - suffix.length());
                  break;
                }
            }
        
        return name;
      }
    
    static String normalizeName(String name)
      {
        return name.replaceAll("_","").toLowerCase();
      }
  }

