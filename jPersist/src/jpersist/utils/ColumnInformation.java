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

package jpersist.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import jpersist.*;

/**
 * This class provides column information for a given result set.
 */

public class ColumnInformation
  {
    int displaySize, type, precision, scale;
    String className, label, name, typeName, schemaName, tableName;

    ColumnInformation(String className, String label, String name, int displaySize, int type, 
                      String typeName, int precision, int scale, String schemaName, String tableName)
      {
        this.className = className;
        this.label = label;
        this.name = name;
        this.displaySize = displaySize;
        this.type = type;
        this.typeName = typeName;
        this.precision = precision;
        this.scale = scale;
        this.schemaName = schemaName;
        this.tableName = tableName;
      }

    public String getClassName() { return className; }
    public String getLabel() { return label; }
    public String getName() { return name; }
    public int getDisplaySize() { return displaySize; }
    public int getType() { return type; }
    public String getTypeName() { return typeName; }
    public int getPrecision() { return precision; }
    public int getScale() { return scale; }
    public String getSchemaName() { return schemaName; }
    public String getTableName() { return tableName; }

    public boolean isTypeQuoted()
      {
        return isTypeQuoted(type);
      }
    
    public static boolean isTypeQuoted(int dataType)
      {
        if (dataType == Types.DATE ||
            dataType == Types.LONGVARCHAR ||
            dataType == Types.TIME ||
            dataType == Types.TIMESTAMP ||
            dataType == Types.VARCHAR)
          return true;

        return false;
      }
    
    public static ColumnInformation[] getColumnInformation(ResultSet resultSet) throws JPersistException, SQLException
      {
        ResultSetMetaData metaData = resultSet.getMetaData();
        ColumnInformation columnInformation[] = new ColumnInformation[metaData.getColumnCount()];

        for (int i = 0; i < metaData.getColumnCount(); i++)
          {
            columnInformation[i] = new ColumnInformation(metaData.getColumnClassName(i+1),
                                                         metaData.getColumnLabel(i+1),
                                                         metaData.getColumnName(i+1),
                                                         metaData.getColumnDisplaySize(i+1),
                                                         metaData.getColumnType(i+1),
                                                         metaData.getColumnTypeName(i+1),
                                                         metaData.getPrecision(i+1),
                                                         metaData.getScale(i+1),
                                                         metaData.getSchemaName(i+1),
                                                         metaData.getTableName(i+1));
          }
        
        return columnInformation;
      }
  }

