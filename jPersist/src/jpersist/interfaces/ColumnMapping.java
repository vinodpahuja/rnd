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

/**
 * This interface is optionally implemented to provide overriding mapping 
 * between methods and table columns, and is only needed when a match can't be 
 * made due to a vast difference in naming and/or a collision will occur.
 */

public interface ColumnMapping 
  {
    /**
     * Method returns the lowercase mapped name and should return names for every 
     * method/column mapping and every column/method mapping (inverse), even if that 
     * simply means returning the value passed in.
     *
     * @param name the lowercase name that is being mapped
     *
     * @return the lowercase mapped name
     */
  
    String getTableColumnName(String name);
  }
