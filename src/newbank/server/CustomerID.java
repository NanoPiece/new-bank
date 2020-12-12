package newbank.server;

import java.util.Random;

public class CustomerID {
	private String Name;
	private String userName;
	private String password;
	private String IBAN;

	public CustomerID(String name, String userName, String password, String iban) {
		this.Name = name; this.userName = userName; this.password = password; this.IBAN = iban;
	}


	public String getName() {
		return Name;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getIBAN(){ return IBAN;}
}