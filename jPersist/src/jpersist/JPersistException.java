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

import jcommontk.utils.ExceptionUtils.JCommonException;

public class JPersistException extends JCommonException
  {
    private static final long serialVersionUID = 100L;
    public static final String DATABASE_CLOSED = "Database is closed";
    public static final String SQL_STATEMENT_NULL = "SQL statement is null or empty";
    
    public JPersistException(Throwable cause) { super(cause); }
    
    public JPersistException(String message) { super(message); }
    
    public JPersistException(String message, Throwable cause) { super(message, cause); }
  }

