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
 * This class should be used in place of InputStream in your objects, 
 * and is used to handle InputStream for PreparedStatement.[get/set]AsciiStream.
 */

public class AsciiStreamAdapter implements AsciiStream
  {
    int length;
    InputStream inputStream;
    
    public AsciiStreamAdapter (int length, InputStream inputStream)
      {
        this.length = length;
        this.inputStream = inputStream;
      }
    
    /**
     * Returns the length of data in the stream.
     * @return the length of data in the stream.
     */
  
    public int getLength() { return length; }
    
    /**
     * Return the InputStream to read from.
     * @return the InputStream to read from
     */
    
    public InputStream getInputStream() { return inputStream; }
  }
