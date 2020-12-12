package newbank.server;

import java.util.Random;

public class CustomerID {
	private String key;
	private String userName;
	private String password;
	private String IBAN;

	public CustomerID(String key, String userName, String password) {
		this.key = key; this.userName = userName; this.password = password; this.IBAN = generateIBAN();
	}


	public static String generateIBAN() {
		int accountNumber = 10000000;
		Random ID = new Random();
		accountNumber += ID.nextInt(90000000);
		String IBAN = "GB24NWBK999999" + accountNumber;
		return IBAN;
	}


	public String getKey() {
		return key;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getIBAN(){ return IBAN;}
}