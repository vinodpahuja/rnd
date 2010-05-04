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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import jpersist.Database;
import jpersist.JPersistException;
import jpersist.Result;

/**
 * This class provides several static utility methods for retrieving data from a database handler.
 */

@SuppressWarnings("unchecked")
public class DatabaseUtils
  {
    public static boolean rowExists(Database db, String sql, Object... objects) throws JPersistException
      {
        Result result = db.parameterizedQuery(sql, objects);

        try
          {
            return result.hasNext();
          }
        finally
          {
            result.close();
          }
      }

    public static void preparedQuery(Database db, PreparedQuery handler, String sql) throws JPersistException
      {
        try
          {
            Statement statement = db.getPreparedStatementForQuery(sql);
            
            try
              {
                while (handler.hasNextQuery())
                  {
                    handler.prepareQuery((PreparedStatement)statement);

                    ResultSet result = ((PreparedStatement)statement).executeQuery();

                    while (result.next())
                      handler.resultSet(result);
                  }
              }
            finally
              {
                statement.close();
              }
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }
    
    public static int preparedUpdate(Database db, PreparedUpdate handler, String sql) throws JPersistException
      {
        try
          {
            int returnValue = 0;
            String generatedKeys[] = handler.getGeneratedKeys();
            Statement statement = null;

            if (generatedKeys != null && generatedKeys.length > 0)
              statement = db.getConnection().prepareStatement(sql, generatedKeys);
            else if (generatedKeys != null)
              statement = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            else
              statement = db.getConnection().prepareStatement(sql);

            try
              {
                while (handler.hasNextUpdate())
                  {
                    handler.prepareUpdate((PreparedStatement)statement);

                    int rval = ((PreparedStatement)statement).executeUpdate();

                    handler.setReturnValue(rval);

                    returnValue += rval;

                    if (generatedKeys != null)
                      {
                        ResultSet keys = statement.getGeneratedKeys();

                        while (keys.next())
                          handler.setGeneratedKey(keys.getLong(1));
                      }
                  }

                return returnValue;
              }
            finally
              {
                statement.close();
              }
          }
        catch (Exception e)
          {
            throw new JPersistException(e);
          }
      }

    public static int multiRowPreparedUpdate(Database db, String sql, List rows) throws JPersistException
      {
        return multiRowPreparedUpdate(db, sql, rows, null);
      }
    
    public static int multiRowPreparedUpdate(Database db, String sql, List rows, List keys) throws JPersistException
      {
        MultiRowPreparedUpdateHandler mr = new MultiRowPreparedUpdateHandler(rows,keys);

        return preparedUpdate(db, mr, sql);
      }

    static class MultiRowPreparedUpdateHandler extends PreparedUpdateAdapter
      {
        List keys;
        Iterator i;
        
        MultiRowPreparedUpdateHandler(List rows, List keys)
          {
            this.keys = keys;
            
            i = rows.iterator();
          }
        
        public boolean hasNextUpdate()
          {
            return i.hasNext();
          }
        
        public void prepareUpdate(PreparedStatement ps) throws JPersistException
          {
            Object[] objects = (Object[])i.next();
            
            try
              {
                Database.setPreparedStatementObjects(ps, objects);
              }
            catch (SQLException ex)
              {
                throw new JPersistException(ex);
              }
          }
        
        public void setGeneratedKey(long key) 
          {
            if (keys != null)
              keys.add(new Long(key));
          }
      }
  }

