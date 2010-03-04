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

public abstract class PreparedUpdateAdapter implements PreparedUpdate
  {
    int returnValue;
    long generatedKey;
    boolean hasNext = true;

    public boolean hasNextUpdate() 
      {
        if (hasNext)
          return !(hasNext = false);

        return false;
      }

    public abstract void prepareUpdate(PreparedStatement preparedStatement) throws Exception;
    
    public String[] getGeneratedKeys() { return null; }
    
    public void setReturnValue(int returnValue) throws Exception { this.returnValue = returnValue;}
    public int getReturnValue() { return this.returnValue;}

    public void setGeneratedKey(long key) throws Exception { this.generatedKey = key; }
    public long getGeneratedKey() { return generatedKey; }
  }

