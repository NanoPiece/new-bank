package newbank.server;


import java.util.ArrayList;

public class Customer {

	private ArrayList<Account> accounts;

	public Customer() {
		accounts = new ArrayList<>();
	}

	public ArrayList<Account> getAccounts() {
		return accounts;
	}

	// Return the list of accounts of a customer as a string
	public String accountsToString() {
		String accountNameHeading = "Account Name";
		String openingBalanceHeading = "Opening Balance";
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
		s += accountNameHeading+"        "+openingBalanceHeading+"\n";

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

}