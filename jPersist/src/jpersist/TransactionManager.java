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

import java.sql.Savepoint;
import java.util.Set;

/**
 * Encloses multiple database calls in a single transaction.  This class must 
 * be used with jpersist.PersistentObject.save(TransactionManager) calls to be
 * effective and/or using the TransactionManager.getDatabase() to work 
 * with the database handler that is involved in the transaction.
 * <p>An example of this is:
 * <pre>
 *   new TransactionManager(dbm) {
 *     public void run() throws JPersistException 
 *       {
 *         // Inserting individually
 *         new Contact("alincoln", ...).save(this);
 *         // and/or
 *         getDatabase().saveObject(new Order("alincoln", ...));
 *         // and/or
 *         saveObject(new Order("alincoln", ...));
 *       }
 *   }.executeTransaction();
 * </pre>
 * While the transaction manager handles beginning the transaction, ending the 
 * transaction, committing the transaction, and rolling back the transaction, 
 * it's also possible to use save points, rollback, and commit at any 
 * point within.
 */
public abstract class TransactionManager
  {
    Database database;
    boolean closeDatabase;

    /**
     * This constructor takes a database manager.
     *
     * @param databaseManager a jpersist.DatabaseManager instance
     */
    public TransactionManager(DatabaseManager databaseManager) throws JPersistException
      {
        this.database = databaseManager.getDatabase();
        closeDatabase = true;
      }

    /**
     * This constructor takes a database.
     *
     * @param database a jpersist.Database instance
     */
    public TransactionManager(Database database) throws JPersistException
      {
        this.database = database;
      }

    /**
     * Returns the database being used for the transaction.  This is useable for
     * any database functionality, including commits, Savepoints, and rollbacks.
     */
    public Database getDatabase() { return database; }

    /**
     * Override this method with your own.
     */
    public abstract void run() throws Exception;
    
    /**
     * Calls the jpersist.Database version.
     */
    public int saveObject(Object object) throws JPersistException
      {
        return database.saveObject(object);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int saveObject(Object object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return database.saveObject(object, nullValuesToInclude);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int saveObject(Object object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return database.saveObject(object, externalClauses, externalClausesParameters);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int saveObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return database.saveObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int deleteObject(Object object) throws JPersistException
      {
        return database.deleteObject(object);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int deleteObject(Object object, Set<String> nullValuesToInclude) throws JPersistException
      {
        return database.deleteObject(object, nullValuesToInclude);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int deleteObject(Object object, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return database.deleteObject(object, externalClauses, externalClausesParameters);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public int deleteObject(Object object, Set<String> nullValuesToInclude, String externalClauses, Object... externalClausesParameters) throws JPersistException
      {
        return database.deleteObject(object, nullValuesToInclude, externalClauses, externalClausesParameters);
      }
    
    /**
     * Calls the jpersist.Database version.
     */
    public void commit() throws JPersistException { database.commit(); }
    
    /**
     * Calls the jpersist.Database version.
     */
    public void rollback() throws JPersistException { database.rollback(); }
    
    /**
     * Calls the jpersist.Database version.
     */
    public void rollback(Savepoint savepoint) throws JPersistException { database.rollback(savepoint); }
    
    /**
     * Calls the jpersist.Database version.
     */
    public Savepoint setSavepoint() throws JPersistException { return database.setSavepoint(); }
    
    /**
     * Calls the jpersist.Database version.
     */
    public Savepoint setSavepoint(String name) throws JPersistException { return database.setSavepoint(name); }
   
    /**
     * Calls the jpersist.MetaData version.
     */
    public boolean supportsSavepoints() throws JPersistException { return database.getMetaData().supportsSavepoints(); }
    
    /**
     * Call this method to execute the transaction on the updates defined in the run() method.  
     * If no exceptions occur the transaction will be commited.  Othwerwise, the 
     * transaction will be rolled back.
     */
    public void executeTransaction() throws JPersistException
      {
        try
          {
            database.beginTransaction();
            
            run();
          }
        catch (Exception e)
          {
            database.rollback();
            
            throw new JPersistException("Transaction was rolled back", e);
          }
        finally
          {
            database.endTransaction();
            
            if (closeDatabase)
              database.close();
          }
      }
  }
