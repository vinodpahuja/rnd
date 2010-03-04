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

public interface PreparedUpdate
  {
    public String[] getGeneratedKeys() throws Exception;
    
    public boolean hasNextUpdate();
    public void prepareUpdate(PreparedStatement preparedStatement) throws Exception;
    public void setReturnValue(int returnValue) throws Exception;
    public void setGeneratedKey(long keyValue) throws Exception;
  }

