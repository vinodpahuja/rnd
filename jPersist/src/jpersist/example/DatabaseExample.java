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

import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import jpersist.Database;
import jpersist.DatabaseManager;
import jpersist.Entity;
import jpersist.JPersistException;
import jpersist.PersistentObject;
import jpersist.Result;
import jpersist.TransactionManager;
import jpersist.annotations.UpdateNullValues;

/* uncomment for DBCP (Apache Commons Connection Pooling) - Need commons-pool and commons-dbcp
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
*/

public class DatabaseExample
  {
    public DatabaseExample(DatabaseManager dbm) throws JPersistException
      {
        // Clean out contacts
        dbm.executeUpdate("delete from contacts");

        // Inserting contact with associations
        Contact contact = new Contact("deisenhower", "mypasswd5", "Dwight", "Eisenhower", "United States", "deisenhower@unitedstates.gov");

        contact.getSupport().add(new Support("Request", "New", "no phone", "deisenhower@unitedstates.gov", "Can I have my bust on a dollar, please."));
        contact.getSupport().add(new Support("Response", "Pending", "no phone", "deisenhower@unitedstates.gov", "Yes, but you may have to share it."));
        contact.getSupport().add(new Support("Request", "New", "no phone", "deisenhower@unitedstates.gov", "Share it with who?"));

        contact.getOrders().add(new Order("Dwight D. Eisenhower Dollar", new Integer(100), new Double(1.00), "unverified"));
        contact.getOrders().add(new Order("Susan B. Anthony Dollar", new Integer(100), new Double(1.00), "unverified"));

        // Saving within an automatic transaction (covers all relationships)
        contact.save(dbm);

        // Add an associated record and update
        contact.getSupport().add(new Support("Response", "Closed", "no phone", "deisenhower@unitedstates.gov", "You'll have to share with Susan Anthony."));
        contact.save(dbm);

        /*
         * Saving within a transaction manager
         */
        new TransactionManager(dbm)
          {
            public void run() throws JPersistException 
              {
                // Inserting individually
                new Contact("alincoln", "mypasswd1", "Abraham", "Lincoln", null, "alincoln@unitedstates.gov").save(this);
                new Support("alincoln", "Request", "New", "no phone", "alincoln@unitedstates.gov", "Can I have my bust on a penny, please.").save(this);
                new Order("alincoln", "Abraham Lincoln Penny", new Integer(100), new Double(.01), "unverified").save(this);

                new Contact("tjefferson", "mypasswd2", "Thomas", "Jefferson", "United States", "tjefferson@unitedstates.gov").save(this);
                new Support("tjefferson", "Request", "New", "no phone", "tjefferson@unitedstates.gov", "Can I have my bust on a nickel, please.").save(this);
                new Order("tjefferson", "Thomas Jefferson Nickel", new Integer(100), new Double(.05), "unverified").save(this);

                // Insert new contact only
                Contact contact1 = new Contact("fdroosevelt", "mypasswd3", "Franklin", "Roosevelt", "United States", "fdroosevelt@unitedstates.gov");
                contact1.save(this);

                // Add associated records and update
                contact1.getSupport().add(new Support("fdroosevelt", "Request", "New", "no phone", "fdroosevelt@unitedstates.gov", "Can I have my bust on a dime, please."));
                contact1.getOrders().add(new Order("fdroosevelt", "Franklin Delano Roosevelt Dime", new Integer(100), new Double(.10), "unverified"));
                contact1.save(this);

                // can still freely use commit, savepoint and rollback

                commit();

                Savepoint savepoint = null;

                if (supportsSavepoints())
                  savepoint = setSavepoint();

                new Contact("gwashington", "mypasswd4", "George", "Washington", "United States", "gwashington@unitedstates.gov").save(this);
                new Support("gwashington", "Request", "New", "no phone", "gwashington@unitedstates.gov", "Can I have my bust on a quarter, please.").save(this);
                new Order("gwashington", "George Washington Quarter", new Integer(100), new Double(.25), "unverified").save(this);

                if (supportsSavepoints())
                  rollback(savepoint);

                // same end result
                getDatabase().saveObject(new Contact("jkennedy", "mypasswd4", "John", "Kennedy", "United States", "gwashington@unitedstates.gov"));
                getDatabase().saveObject(new Support("jkennedy", "Request", "New", "no phone", "gwashington@unitedstates.gov", "Can I have my bust on the half dollar, please."));

                // also same end result
                saveObject(new Support("jkennedy", "Response", "Pending", "no phone", "nobody@unitedstates.gov", "Yes, we'll let you know"));
                saveObject(new Order("jkennedy", "John Fitzgerald Kennedy Half Dollar", new Integer(100), new Double(.50), "unverified"));
              }
          }.executeTransaction();

        /*
         * The jpersist.DatabaseManager way to load objects
         */

        // Load based on information contained in classes (load associations seperately)
        contact = dbm.loadObject(Contact.class, false, "where :contactId like 'tjef%'");
        dbm.loadAssociations(contact);
        System.out.println("\ncontactId = " + contact.getContactId());

        // or Load based on information contained in objects
        contact = dbm.loadObject(new Contact("tjef%"));
        System.out.println("contactId = " + contact.getContactId());

        // or with variable argument parameters
        contact = dbm.loadObject(Contact.class, "where :contactId like ?", "tjef%");
        System.out.println("contactId = " + contact.getContactId() + "\n");

        // Load a collection of objects from the database (without associations)
        Collection<Contact> c = dbm.loadObjects(new ArrayList<Contact>(), Contact.class, false);

        for (Contact contact2 : c)
          System.out.println("contactId = " + contact2.getContactId());

        System.out.println();

        /*
         * The jpersist.Database way to load objects
         */

        Database db = dbm.getDatabase();

        try
          {
            // Query all
            Result<Contact> result = db.queryObject(Contact.class, "order by :lastName");

            // Result is Iterable
            for (Contact contact3 : result)
              {
                System.out.println(contact3);

                // Update a couple of mistakes
                if (contact3.getContactId().startsWith("jkennedy"))
                  {
                    contact3.setEmail("jkennedy@unitedstates.gov");

                    for (Support support : contact3.getSupport())
                      if (support.getEmail().startsWith("gwash") || support.getEmail().startsWith("nobody"))
                        support.setEmail("jkennedy@unitedstates.gov");

                    contact3.save(db);
                  }
                else if (contact3.getContactId().startsWith("fdroosevelt"))
                  {
                    contact3.setCompanyName(null);

                    db.saveObject(contact3);
                  }
              }

            result.close();

            result = db.queryObject(Contact.class, "order by :lastName");

            // Print and delete
            while (result.hasNext() && (contact = result.next()) != null)
              {
                System.out.println(contact);
                db.deleteObject(contact);
              }

            result.close();
          }
        finally
          {
            // also closes any open results
            db.close();
          }
      }
/*
    static DataSource getDbcpDataSource() throws ClassNotFoundException
      {
        Class.forName("org.hsqldb.jdbcDriver");

        GenericObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:hsqldb:file:/hsqldb/jdbc/jpersist", null, null);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        return new PoolingDataSource(connectionPool);
      }
*/
    public static void main(String[] args) throws JPersistException
      {
        DatabaseManager dbm = null;

        try
          {
            DatabaseManager.setLogLevel(Level.OFF);

            dbm = DatabaseManager.getXmlDefinedDatabaseManager("jpersist");
            //dbm = DatabaseManager.getUrlDefinedDatabaseManager("jpersist", 10, "jpersist", 
            //      "org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:/hsqldb/jdbc/jpersist");
            //dbm = DatabaseManager.getDataSourceDatabaseManager("jpersist", 10, getDbcpDataSource(), null, null);

            for (int i = 0; i < 1; i++)
              {
                new DatabaseExample(dbm);
                System.out.println("count=" + (i + 1));
              }
          }
        finally
          {
            // also closes any open jpersist.Database
            dbm.close();
          }
      }
    
    @UpdateNullValues
    public static class Contact extends PersistentObject 
      {
        private static final long serialVersionUID = 100L;

        private String contactId, password, firstName, lastName, companyName, email;
        private List<Support> support = new ArrayList<Support>();
        private List<Order> orders = new ArrayList<Order>();

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

        // Associations
        public List<Support> getDbAssociation(Support c) { return support; }
        public void setDbAssociation(Support c, List<Support> s) { support = s; }
        public List<Order> getDbAssociation(Order c) { return orders; }
        public void setDbAssociation(Order c, List<Order> o) { orders = o; }
        
        // association convenience (is optional)
        public List<Support> getSupport() { return support; }
        public void setSupport(List<Support> support) { this.support = support; }
        public List<Order> getOrders() { return orders; }
        public void setOrders(List<Order> orders) { this.orders = orders; }

        public String toString()
          {
            String returnString = contactId + ", " + firstName + ", " + lastName 
                                + ", " + companyName + ", " + email + "\n";

            if (support != null)
              for (Support s : support)
                returnString += s + "\n";

            if (orders != null)
              for (Order o : orders)
                returnString += o + "\n";

            return returnString;
          }
      }
    
    public static class Order extends Entity // can optionally extend Entity for esthetics
      {
        private static final long serialVersionUID = 100L;

        private Long orderId;
        private Integer quantity;
        private Double price;
        private String contactId, product, status;

        public Order() {}

        public Order(String product, Integer quantity, Double price, String status)
          {
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.status = status;
          }

        public Order(String contactId, String product, Integer quantity, Double price, String status)
          {
            this.contactId = contactId;
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.status = status;
          }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getContactId() { return contactId;}
        public void setContactId(String contactId) { this.contactId = contactId; }
        public String getProduct() { return product; }
        public void setProduct(String product) { this.product = product; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String toString() 
          { 
            return orderId + ", " + contactId + ", "
                           + quantity + ", " + price + ", " 
                           + product + ", " + status; 
          }
      }
    
    public static class Support extends PersistentObject
      {
        private static final long serialVersionUID = 100L;

        private Long supportId;
        private String contactId, code, status, phone, email, request;

        public Support() {}

        public Support(String code, String status, String phone, String email, String request)
          {
            this.code = code;
            this.status = status;
            this.phone = phone;
            this.email = email;
            this.request = request;
          }

        public Support(String contactId, String code, String status, 
                       String phone, String email, String request)
          {
            this.contactId = contactId;
            this.code = code;
            this.status = status;
            this.phone = phone;
            this.email = email;
            this.request = request;
          }

        public Long getSupportId() { return supportId; }
        public void setSupportId(Long id) { supportId = id; }
        public String getContactId() { return contactId; }
        public void setContactId(String id) { contactId = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRequest() { return request; }
        public void setRequest(String request) { this.request = request; }

        public String toString() 
          { 
            return supportId + ", " + contactId + ", " + code + "," 
                             + status + ", " + phone + ", " 
                             + email + ", " + request; 
          }
      }
  }
