package newbank.server;


import java.util.ArrayList;
import java.util.Collections;

public class Customer {

	private ArrayList<Account> accounts;
	private ArrayList<Integer> associatedMicroLoanID;
	private CustomerID customerID;
	private String Name;
	private String IBAN;
	private String pass;

	public Customer(String actualName, String userName, String password, String iban) {
		accounts = new ArrayList<>();
		associatedMicroLoanID = new ArrayList<>();
		customerID = new CustomerID(actualName, userName, password, iban);
		Name = customerID.getName();
		IBAN = iban;
	}

	public CustomerID getCustomerID() {
		return customerID;
	}
	public String getName() {
		return Name;
	}
	public String getIBAN() { return IBAN; }

	public ArrayList<Account> getAccounts() {
		return accounts;
	}


	// Return the list of accounts of a customer as a string
	public String accountsToString() {
		String accountNameHeading = "Account Name";
		String openingBalanceHeading = "Opening Balance";
		String uniqueIBAN = "IBAN: " + customerID.getIBAN();
		String s = ""; // the output variable of this function

		int longestAccountNameCount=accountNameHeading.length();
		for(Account a : accounts) {
			if(a.getAccountName().length() > longestAccountNameCount) {
				longestAccountNameCount = a.getAccountName().length();
			}
		}

		// Header
		if (accountNameHeading.length() < longestAccountNameCount) {
			int difference = longestAccountNameCount-accountNameHeading.length();
			for(int i=0; i<difference; i++){
				accountNameHeading += " ";
			}
		}
		s += accountNameHeading+"        "+openingBalanceHeading+"        "+uniqueIBAN+"\n";

		// Divider
		int dividerLength = s.length();
		for(int i=0; i<dividerLength; i++){
			s += "-";
		}
		s += "\n";

		// Accounts detail
		for(Account a : accounts) {
			s += a.getAccountName();
			for(int i=0;i<longestAccountNameCount-a.getAccountName().length();i++){
				s += " ";
			}
			s += "        ";
			s += a.getOpeningBalance();
			s += "\n";
		}

		// return output
		return s;
	}

	public void addAccount(Account account) {
		accounts.add(account);
	}

	public Account getAccount(String accountName) {
		for(Account a : accounts){
			if (a.getAccountName().equals(accountName)) {
				return a;
			}
		}
		return null;
	}

	public ArrayList<Account> getAllAccounts() {
		return accounts;
	}

	// remove Account
	public void closeAccount(Account account) {
		accounts.remove(account);
	}

	// get number of accounts
	public int numAccounts() {return accounts.size();}

	// Associate microloan with Customer
	public void addMicroLoanID(int loanID) {
		associatedMicroLoanID.add(loanID);
		Collections.sort(associatedMicroLoanID);
	}
	public void removeMicroLoanID(int loanID) {
		associatedMicroLoanID.remove(new Integer(loanID));
		Collections.sort(associatedMicroLoanID);
	}
	public ArrayList<Integer> getAssociatedMicroLoanID() {
		return associatedMicroLoanID;
	}

}