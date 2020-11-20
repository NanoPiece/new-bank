package newbank.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NewBank {
	
	private static final NewBank bank = new NewBank();
	private HashMap<String,Customer> customers;
	
	private NewBank() {
		customers = new HashMap<>();
		addTestData();
	}
	
	private void addTestData() {
		Customer bhagy = new Customer();
		bhagy.addAccount(new Account("Main", 1000.0));
		bhagy.addAccount(new Account("Second", 2000.0));
		bhagy.addAccount(new Account("very long string of accounts", 3000000.0));
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
			case "SHOWMYACCOUNTS" : return showMyAccounts(customer);
			case "CHANGEMYACCOUNTNAME" : return changeMyAccountName(customer, request);
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
}
