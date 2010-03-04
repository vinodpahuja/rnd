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

import java.io.InputStream;

/**
 * This interface should be used in place of InputStream in your objects, 
 * and is used to handle InputStream for PreparedStatement.[get/set]BinaryStream.
 */

public interface BinaryStream
  {
    /**
     * Returns the length of data in the stream.
     * @return the length of data in the stream.
     */
  
    int getLength();
    
    /**
     * Return the InputStream to read from.
     * @return the InputStream to read from
     */
    
    InputStream getInputStream();
  }
