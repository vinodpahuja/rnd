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

/**
 * Encloses multiple database updates using prepared statements and/or 
 * batch update processing.  This class must be used with 
 * jpersist.PersistentObject.save(UpdateManager) calls to be
 * effective and/or using the UpdateManager.getDatabase() to work 
 * with the database handler that is involved in the grouped updates.
 * <p>An example of this is:
 * <pre>
 *   new UpdateManager(dbm) {
 *     public void run() throws JPersistException 
 *       {
 *         // Inserting individually
 *         new Contact("alincoln", ...).save(this);
 *         // and/or
 *         getDatabase().saveObject(new Order("alincoln", ...));
 *         // and/or
 *         saveObject(new Order("alincoln", ...));
 *       }
 *   }.executeUpdates();
 * or:
 *   }.executeBatchUpdates();
 * </pre>
 * Since the update manager extends the transaction manager,
 * it's possible to use save points, rollback, and commit at any 
 * point within.
 */
public abstract class UpdateManager extends TransactionManager
  {
    /**
     * This constructor takes a database manager.
     *
     * @param databaseManager a jpersist.DatabaseManager instance
     */
    public UpdateManager(DatabaseManager databaseManager) throws JPersistException
      {
        super(databaseManager);
      }

    /**
     * This constructor takes a database.
     *
     * @param database a jpersist.Database instance
     */
    public UpdateManager(Database database) throws JPersistException
      {
        super(database);
      }
    
    /**
     * Call this method to execute the updates defined in the run() method.  
     * This method also starts a transaction. If no 
     * exceptions occur the transaction will be commited.  Othwerwise, the 
     * transaction will be rolled back.
     */
    public void executeUpdates() throws JPersistException
      {
        super.executeTransaction();
      }
    
    /**
     * Call this method to execute the batch updates defined in the run() method.  
     * This method also starts a transaction. If no 
     * exceptions occur the transaction will be commited.  Othwerwise, the 
     * transaction will be rolled back.
     * 
     * @return the update counts for the batch processing
     */
    public Integer[] executeBatchUpdates() throws JPersistException
      {
        Integer[] updateCounts = null;
        
        try
          {
            database.beginTransaction();
            database.beginBatch();
            
            run();
          }
        catch (Exception e)
          {
            database.endBatch();
            database.rollback();
            
            throw new JPersistException("Transaction was rolled back", e);
          }
        finally
          {
            database.executeBatch();
            
            updateCounts = database.getBatchUpdateCounts();
            
            database.endBatch();
            database.endTransaction();
            
            if (closeDatabase)
              database.close();
          }
            
        return updateCounts;
      }
  }
