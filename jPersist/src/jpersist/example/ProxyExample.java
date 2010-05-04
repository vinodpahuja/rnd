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

package jpersist.example;

import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jpersist.Database;
import jpersist.DatabaseManager;
import jpersist.JPersistException;
import jpersist.interfaces.ResultObject;

public class ProxyExample
  {
    static Logger logger = Logger.getLogger(ProxyExample.class.getName());

    public interface Contacts extends ResultObject
      {
        public String getContactId();
        public void setContactId(String contactId);
        public String getPassword();
        public void setPassword(String password);
        public String getFirstName();
        public void setFirstName(String firstName);
        public String getLastName();
        public void setLastName(String lastName);
        public String getCompanyName();
        public void setCompanyName(String companyName);
        public String getEmail();
        public void setEmail(String email);
      }
    
    public ProxyExample() throws JPersistException
      {
        DatabaseManager dbm = null;

        try
          {
            DatabaseManager.setLogLevel(Level.OFF);
            dbm = DatabaseManager.getXmlDefinedDatabaseManager("jpersist");
            Database db = dbm.getDatabase();
            Contacts c = null;

            try
              {
                db.setResultSetConcurrency(ResultSet.CONCUR_UPDATABLE);
                c = (Contacts)db.executeQuery("select * from contacts where 1 = 0").castToInterface(Contacts.class);

                /*
                 * A number of databases lack support for inserting data into a result set
                 */
                for (int i = 0; i < 5; i++)
                  {
                    c.moveToInsertRow();

                    c.setContactId("contactId" + System.currentTimeMillis());
                    c.setPassword("password_" + i);
                    c.setFirstName("First Name " + i);
                    c.setLastName("Last Name " + i);
                    c.setCompanyName("Company Name " + i);
                    c.setEmail("email " + i);

                    c.insertRow();
                  }
              }
            catch (JPersistException e)
              {
                e.printStackTrace();
                System.out.println("A number of databases lack support for inserting data into a result set.");
              }

            c = (Contacts)db.executeQuery("select * from contacts").castToInterface(Contacts.class);

            while (c.hasNext() && c.next() != null)
              System.out.println(c.getContactId() + ", " + c.getFirstName() + ", " + c.getLastName());

            db.close();
          }
        finally
          {
            dbm.close();
          }
      }
    
    public static void main(String[] args) throws JPersistException
      {
        new ProxyExample();
      }
  }
