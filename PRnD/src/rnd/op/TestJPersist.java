package rnd.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import jpersist.DatabaseManager;
import jpersist.JPersistException;

public class TestJPersist {

	public static void main(String[] args) throws JPersistException {
		DatabaseManager dbm = null;

		try {
			DatabaseManager.setLogLevel(Level.ALL);

			dbm = DatabaseManager.getUrlDefinedDatabaseManager("testdb", 10, "org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:/home/vinodp/Data/Database/TestDB/testdb",null,"PUBLIC");

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
			dbm.saveObject(contact);

			// Load based on information contained in classes
			contact = dbm.loadObject(Contact.class, "where :contactId = ?", "deisenhower");
			System.out.println("\ncontactId = " + contact.getContactId());

			// Load a collection of objects from the database
			Collection<Contact> c = dbm.loadObjects(new ArrayList<Contact>(), new Contact("deisenhower"));

			for (Contact contact2 : c)
				System.out.println("contactId = " + contact2.getContactId());
		} finally {
			// also closes any open jpersist.Database
			dbm.close();
		}
	}

	public static class Contact {
		private String contactId, password, firstName, lastName, companyName, email;
		private List<Support> support = new ArrayList<Support>();
		private List<Order> orders = new ArrayList<Order>();

		public Contact() {
		}

		public Contact(String contactId) {
			this.contactId = contactId;
		}

		public Contact(String contactId, String password, String firstName, String lastName, String companyName, String email) {
			this.contactId = contactId;
			this.password = password;
			this.firstName = firstName;
			this.lastName = lastName;
			this.companyName = companyName;
			this.email = email;
		}

		public String getContactId() {
			return contactId;
		}

		public void setContactId(String id) {
			contactId = id;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String passwd) {
			password = passwd;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String fName) {
			firstName = fName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lName) {
			lastName = lName;
		}

		public String getCompanyName() {
			return companyName;
		}

		public void setCompanyName(String name) {
			companyName = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		// Associations
		public List<Support> getDbAssociation(Support c) {
			return support;
		}

		public void setDbAssociation(Support c, List<Support> s) {
			support = s;
		}

		public List<Order> getDbAssociation(Order c) {
			return orders;
		}

		public void setDbAssociation(Order c, List<Order> o) {
			orders = o;
		}

		// association convenience (is optional)
		public List<Support> getSupport() {
			return support;
		}

		public void setSupport(List<Support> support) {
			this.support = support;
		}

		public List<Order> getOrders() {
			return orders;
		}

		public void setOrders(List<Order> orders) {
			this.orders = orders;
		}
	}

	public static class Order {
		private Long orderId;
		private Integer quantity;
		private Double price;
		private String contactId, product, status;

		public Order() {
		}

		public Order(String product, Integer quantity, Double price, String status) {
			this.product = product;
			this.quantity = quantity;
			this.price = price;
			this.status = status;
		}

		public Order(String contactId, String product, Integer quantity, Double price, String status) {
			this.contactId = contactId;
			this.product = product;
			this.quantity = quantity;
			this.price = price;
			this.status = status;
		}

		public Long getOrderId() {
			return orderId;
		}

		public void setOrderId(Long orderId) {
			this.orderId = orderId;
		}

		public String getContactId() {
			return contactId;
		}

		public void setContactId(String contactId) {
			this.contactId = contactId;
		}

		public String getProduct() {
			return product;
		}

		public void setProduct(String product) {
			this.product = product;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}

		public Double getPrice() {
			return price;
		}

		public void setPrice(Double price) {
			this.price = price;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	public static class Support {
		private Long supportId;
		private String contactId, code, status, phone, email, request;

		public Support() {
		}

		public Support(String code, String status, String phone, String email, String request) {
			this.code = code;
			this.status = status;
			this.phone = phone;
			this.email = email;
			this.request = request;
		}

		public Support(String contactId, String code, String status, String phone, String email, String request) {
			this.contactId = contactId;
			this.code = code;
			this.status = status;
			this.phone = phone;
			this.email = email;
			this.request = request;
		}

		public Long getSupportId() {
			return supportId;
		}

		public void setSupportId(Long id) {
			supportId = id;
		}

		public String getContactId() {
			return contactId;
		}

		public void setContactId(String id) {
			contactId = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getRequest() {
			return request;
		}

		public void setRequest(String request) {
			this.request = request;
		}
	}

}