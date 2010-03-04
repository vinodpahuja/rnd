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

import java.util.ArrayList;
import java.util.logging.Level;
import jpersist.Database;
import jpersist.DatabaseManager;
import jpersist.JPersistException;
import jpersist.UpdateManager;

public class UpdateAndBatchExample 
  {
    public UpdateAndBatchExample(DatabaseManager dbm) throws JPersistException
      {
        Database db = dbm.getDatabase();
        
        // Clean out contacts
        db.executeUpdate("delete from contacts");
        
        // Inserting contacts - efficiently uses a single prepared statement 
        db.saveObject(new Contact("contact0", "mypasswd0", "FirstName0", "LastName0", "Company0", "email0@company.com"));
        db.saveObject(new Contact("contact1", "mypasswd1", "FirstName1", "LastName1", "Company1", "email1@company.com"));
        db.saveObject(new Contact("contact2", "mypasswd2", "FirstName2", "LastName2", "Company2", "email2@company.com"));
        db.saveObject(new Contact("contact3", "mypasswd3", "FirstName3", "LastName3", "Company3", "email3@company.com"));
        db.saveObject(new Contact("contact4", "mypasswd4", "FirstName4", "LastName4", "Company4", "email4@company.com"));

        /**
         * Inserting contacts - uses batch updating
         * and efficiently uses a single prepared statement 
         */
        db.beginBatch();
        
        db.saveObject(new Contact("contact5", "mypasswd5", "FirstName5", "LastName5", "Company5", "email5@company.com"));
        db.saveObject(new Contact("contact6", "mypasswd6", "FirstName6", "LastName6", "Company6", "email6@company.com"));
        db.saveObject(new Contact("contact7", "mypasswd7", "FirstName7", "LastName7", "Company7", "email7@company.com"));
        db.saveObject(new Contact("contact8", "mypasswd8", "FirstName8", "LastName8", "Company8", "email8@company.com"));
        db.saveObject(new Contact("contact9", "mypasswd9", "FirstName9", "LastName9", "Company9", "email9@company.com"));

        db.executeBatch();
        db.endBatch();
        
        for (Contact c : db.queryObject(Contact.class))
          System.out.println(c);
        
        db.close();

        /**
         * The following are equivalent to the above.
         */
        
        // Inserting contacts - efficiently uses a single prepared statement 
        new UpdateManager(dbm) 
          {
            public void run() throws JPersistException 
              {
                for (int i = 10; i < 50; i++)
                  saveObject(new Contact("contact" + i, "mypasswd" + i, "FirstName" + i, "LastName" + i, "Company" + i, "email" + i + "@company.com"));
              }
          }.executeUpdates();

        /**
         * Inserting contacts - uses batch updating
         * and efficiently uses a single prepared statement 
         */
        new UpdateManager(dbm) 
          {
            public void run() throws JPersistException 
              {
                for (int i = 51; i < 100; i++)
                  saveObject(new Contact("contact" + i, "mypasswd" + i, "FirstName" + i, "LastName" + i, "Company" + i, "email" + i + "@company.com"));
              }
          }.executeBatchUpdates();
          
        for (Contact c : dbm.loadObjects(new ArrayList<Contact>(), Contact.class))
          System.out.println(c);
      }

    public static void main(String[] args) throws JPersistException
      {
        DatabaseManager dbm = null;

        try
          {
            DatabaseManager.setLogLevel(Level.OFF);

            dbm = DatabaseManager.getXmlDefinedDatabaseManager("jpersist");

            new UpdateAndBatchExample(dbm);
          }
        finally
          {
            // also closes any open jpersist.Database
            dbm.close();
          }
      }
    
    public static class Contact
      {
        private String contactId, password, firstName, lastName, companyName, email;

        public Contact() {}

        public Contact(String contactId)
          {
            this.contactId = contactId;
          }

        public Contact(String contactId, String password, String firstName, 
                       String lastName, String companyName, String email)
          {
            this.contactId = contactId;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.companyName = companyName;
            this.email = email;
          }

        public String getContactId() { return contactId; }
        public void setContactId(String id) { contactId = id; }
        public String getPassword() { return password; }
        public void setPassword(String passwd) { password = passwd; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String fName) { firstName = fName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lName) { lastName = lName; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String name) { companyName = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String toString()
          {
            return contactId + ", " + firstName + ", " + lastName 
                             + ", " + companyName + ", " + email;
          }
      }
  }
