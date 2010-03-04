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
 * between classes and tables and is only needed when a match can't be 
 * made due to a vast difference in naming and/or a collision will occur.
 */

public interface TableMapping 
  {
    /**
     * Method returns the mapped name.
     *
     * @param name the lowercase name that is being mapped
     *
     * @return the lowercase mapped name
     */
  
    String getDatabaseTableName(String name);
/*    
    public static class TableInfo
      {
        private String schemaName, catalogName, tableName;

        public TableInfo(String schemaName, String catalogName, String tableName)
          {
            this.schemaName = schemaName;
            this.catalogName = catalogName;
            this.tableName = tableName;
          }
        
        public String getSchemaName() { return schemaName; }
        public String getCatalogName() { return catalogName; }
        public String getTableName() { return tableName; }
      }
 */
  }
