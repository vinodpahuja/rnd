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

package jpersist.interfaces;

import java.util.List;
import jpersist.Database;

/**
 * This interface is optionally implemented to provide overriding 
 * auto-generated key handling abilities.
 */

public interface GeneratedKeys 
  {
    /**
     * This method allows for overriding jPersists handling of auto-generated keys.
     *
     * @param db the current database handler
     * @param keysRequested the List containing the keys requested (if any)
     * @param keysReturned the List containing the keys returned (if any)
     *
     * @return should return true if the keys we're handled successfully, 
     *         and false if they were not.
     */
    boolean setGeneratedKeys(Database db, List keysRequested, List keysReturned);
  }
