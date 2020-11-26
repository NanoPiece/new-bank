package newbank.server;


import java.util.*;


public class NewBank {

	private static final NewBank bank = new NewBank();
	public HashMap<String,Customer> customers;

	private NewBank() {
		customers = new HashMap<>();
		addTestData();
	}

	private void addTestData() {
		Customer bhagy = new Customer();
		bhagy.addAccount(new Account("Current", 1000.0));
		bhagy.addAccount(new Account("Savings", 2000.0));
		bhagy.addAccount(new Account("Checking", 3000000.0));
		customers.put("Bhagy", bhagy);

		Customer christina = new Customer();
		christina.addAccount(new Account("Savings", 1500.0));
		customers.put("Christina", christina);

		Customer john = new Customer();
		john.addAccount(new Account("Checking", 250.0));
		customers.put("John", john);
	}

	public static NewBank getBank() {
		return bank;
	}

	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		if(customers.containsKey(userName)) {
			return new CustomerID(userName);
		}
		return null;
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request) {
		if(customers.containsKey(customer.getKey())) {
			List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
			switch(input.get(0)) {
				case "1" : return showMyAccounts(customer);
				case "2" : return changeMyAccountName(customer, request);
				case "3" : return transferToExternalUser(customer, request);
				case "4" : return transferToOtherAccount(customer, request);
				case "5" : return createNewAccount(customer, request);
				case "DISPLAYSELECTABLEACCOUNTS" : return displaySelectableAccounts(customer);
				default : return "FAIL";
			}
		}
		return "FAIL";
	}

	private String showMyAccounts(CustomerID customer) {
		return (customers.get(customer.getKey())).accountsToString();
	}

	private String changeMyAccountName(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = accountName, 2 = newAccountName
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		// get account
		Account account = customers.get(customer.getKey()).getAccount(input.get(1));
		// change account name
		account.setAccountName(input.get(2));

		return "You account name has been modified.";
	}

	private String displaySelectableAccounts(CustomerID customer) {
		ArrayList<Account> accounts = customers.get(customer.getKey()).getAllAccounts();
		String output = "";
		for(int i=1; i<= accounts.size(); i++){
			String account = accounts.get(i-1).getAccountName();
			output += i + ". " + account + "\n";
		}
		return output;
	}

	private String transferToExternalUser(CustomerID customer, String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		Customer Receiver = bank.getIndex(input.get(1));
		if(Receiver==null)
		{
			return "No user exists!";
		}
		Double amount = Double.valueOf(input.get(2));
		Account account = customers.get(customer.getKey()).getAccount(input.get(3));
		account.transfer(Receiver.getAllAccounts().get(0), amount);
		return "Transfer to external user Complete";
	}

	private String transferToOtherAccount(CustomerID customer, String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		if(customers.get(customer.getKey()).getAllAccounts().size()<2)
		{
			return "You don't have 2 accounts!";
		}
		Account account_from = customers.get(customer.getKey()).getAccount(input.get(1));
		Account account_to = customers.get(customer.getKey()).getAccount(input.get(2));
		Double amount = Double.valueOf(input.get(3));
		account_from.transfer(account_to, amount);
		return "Internal transfer to other account Complete";
	}


	private String createNewAccount(CustomerID customer, String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		System.out.println(input.get(1));
		String accountType = (input.get(1));
		Customer thisCustomer = customers.get(customer.getKey());
		if (accountType.equals("1")) {
			accountType = "Current Account";
		}
		if (accountType.equals("2")) {
			accountType = "Savings Account";
		}
		thisCustomer.addAccount(new Account(accountType, 00.0));
		return "Account '" + accountType + "' Created.\n";
	}
  
	Customer getIndex(String newP)
	{
		return customers.getOrDefault(newP,null);
	}

}

