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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is extended by classes that wish to be persistent.
 */
@SuppressWarnings("unchecked")
public class PersistentObject implements Serializable
  {
    private static final long serialVersionUID = 100L;

    static final int OBJECT_CAN_PERSIST = 0;
    static final int MISSING_ROW_ID = 1;
    static final int NO_TABLE_INFO = 2;
    
    private int objectPersistence = OBJECT_CAN_PERSIST;
    private long objectChecksum;
    private Set ignoreAssociationClasses = null;
    private Map objectKeyValues = null;
    private boolean objectHasChanged,
                    reloadAfterSave = true,
                    ignoreAssociations;

    long getObjectChecksum()
      {
        return objectChecksum;
      }

    void setObjectChecksum(long objectChecksum)
      {
        this.objectChecksum = objectChecksum;
      }

    Map getObjectKeyValues()
      {
        if (objectKeyValues == null)
          objectKeyValues = new HashMap();
      
        return objectKeyValues;
      }

    void setObjectPersistence(int reason)
      {
        objectPersistence = reason;
      }

    /**
     * Returns true is associations are being ignored and not loaded or saved, false otherwise.
     *
     * @return true is associations are being ignored and not loaded or saved, false otherwise.
     */
    protected boolean getIgnoreAssociations()
      {
        return ignoreAssociations;
      }

    /**
     * Set whether loads/saves ignore associations.
     *
     * @param ignoreAssociations true to ignore saving/loading associations, false otherwise.
     */
    protected void setIgnoreAssociations(boolean ignoreAssociations)
      {
        this.ignoreAssociations = ignoreAssociations;
      }
    
    /**
   * Adds the class to the set of ignored associations.
   * 
   * 
   * @param objectClass the class to ignore
   */
    protected void addIgnoreAssociation(Class objectClass)
      {
        if (ignoreAssociationClasses == null)
          ignoreAssociationClasses = new HashSet();
        
        ignoreAssociationClasses.add(objectClass);
      }
    
    /**
   * Removes the class from the set of ignored associations.
   * 
   * 
   * @param objectClass the class to remove
   */
    protected void removeIgnoreAssociation(Class objectClass)
      {
        if (ignoreAssociationClasses != null)
          ignoreAssociationClasses.remove(objectClass);
      }

    boolean classInIgnoreAssociation(Class objectClass) 
      {
        if (ignoreAssociationClasses != null && ignoreAssociationClasses.contains(objectClass))
          return true;
        
        return false;
      }
    
    /**
     * Returns true if the object is to be reloaded following a save.
     *
     * @return true if the object is to be reloaded following a save.
     */
    protected boolean getReloadAfterSave()
      {
        return reloadAfterSave;
      }

    /**
     * Set whether objects are reloaded after saves.  If the database changes 
     * any of the columns associated with an object, it should be reloaded.
     *
     * @param reloadAfterSave true if the object is to be reloaded following a save
     */
    protected void setReloadAfterSave(boolean reloadAfterSave)
      {
        this.reloadAfterSave = reloadAfterSave;
      }
    
    /**
     * Returns whether an object can be persistent, and the reason why or why not.  
     * Objects are checked during loading to see if they can be made persistent.  
     * If they can OBJECT_CAN_PERSIST will be returned.  If the object can't persist, 
     * then the reason why, usually MISSING_ROW_ID, will be returned.
     *
     * @return whether an object can be persistent, and the reason why or why not
     */
    protected int getObjectPersistence()
      {
        return objectPersistence;
      }
    
    /**
     * Returns the key value that was originally loaded with the object.  Even if the 
     * field in the object changes the original key's value remains for updates 
     * and deletes.
     *
     * @param key a key field defined in the object
     * @return the key value that was originally loaded with the object
     */
    protected Object getObjectKeyValue(Object key)
      {
        if (objectKeyValues != null)
          return objectKeyValues.get(key);
        
        return null;
      }

    /**
     * Make object transient/new.  Will insert instead of update.
     */
    protected void makeObjectTransient()
      {
        objectKeyValues = null;
        objectChecksum = 0;
        objectHasChanged = false;
      }

    /**
   * Returns true if the object has changed in some way and needs to be saved.
   * 
   * 
   * @return true if the object has changed in some way and needs to be saved.
   */
    protected boolean objectHasChanged()
      {
        return objectHasChanged;
      }

    /**
   * Set the state of the object to has changed which will cause it to be saved.
   * 
   * 
   * @param objectHasChanged true if the object has changed and needs to be saved
   */
    protected void setObjectHasChanged(boolean objectHasChanged)
      {
        this.objectHasChanged = objectHasChanged;
      }

    /**
     * Returns true if object is in the persistent state (has been saved/loaded and will be updated)
     * @return true if object is in the persistent state (has been saved/loaded and will be updated)
     */
    protected boolean isPersistent() 
      {
        return getObjectChecksum() != 0 && getObjectPersistence() == PersistentObject.OBJECT_CAN_PERSIST; 
      }

    /**
     * Returns true if the object can be persistent.  If an object doesn't have table information 
     * or row id information, it can not be persistent.
     *
     * @return true if the object can be persistent.
     */
    protected boolean canPersist() 
      {
        return getObjectPersistence() == PersistentObject.OBJECT_CAN_PERSIST; 
      }

    /**
     * Saves the object to a matching database table using the jpersist.Database instance.
     * 
     * @param database a database instance
     *
     * @throws JPersistException
     */
    public int save(Database database) throws JPersistException
      {
        return database.saveObject(this);
      }

    /**
     * Saves the object to a matching database table using the jpersist.DatabaseManager instance.
     * 
     * @param databaseManager a database manager instance
     *
     * @throws JPersistException
     */
    public int save(DatabaseManager databaseManager) throws JPersistException
      {
        return databaseManager.saveObject(this);
      }
    
    /**
     * Saves the object to a matching database table using the jpersist.TransactionManager instance.
     * 
     * @param transactionManager a jpersist.TransactionManager instance
     *
     * @throws JPersistException
     */
    public int save(TransactionManager transactionManager) throws JPersistException
      {
        return transactionManager.getDatabase().saveObject(this);
      }
    
    /**
     * Deletes the object from a matching database table using the jpersist.Database instance.
     * 
     * @param database a database instance
     *
     * @throws JPersistException
     */
    protected int delete(Database database) throws JPersistException 
      {
        return database.deleteObject(this);
      }
    
    /**
     * Deletes the object from a matching database table using the jpersist.DatabaseManager instance.
     * 
     * @param databaseManager a database manager instance
     *
     * @throws JPersistException
     */
    protected int delete(DatabaseManager databaseManager) throws JPersistException 
      {
        return databaseManager.deleteObject(this);
      }
    
    /**
     * Deletes the object from a matching database table using the jpersist.TransactionManager instance.
     * 
     * @param transactionManager a jpersist.TransactionManager instance
     *
     * @throws JPersistException
     */
    protected int delete(TransactionManager transactionManager) throws JPersistException
      {
        return transactionManager.getDatabase().deleteObject(this);
      }
  }
