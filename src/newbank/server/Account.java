package newbank.server;

public class Account {
	
	private String accountName;
	private double openingBalance;

	public Account(String accountName, double openingBalance) {
		this.accountName = accountName;
		this.openingBalance = openingBalance;
	}
	
	public String toString() {
		return (accountName + ": " + openingBalance);
	}

	// Getters functions
	public String getAccountName() { return (accountName); }
	public double getOpeningBalance() { return (openingBalance); }

	// Setter functions
	public void setAccountName(String name) {
		this.accountName = name;
	}

}
