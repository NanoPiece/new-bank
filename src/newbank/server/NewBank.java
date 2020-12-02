package newbank.server;


import java.io.IOException;
import java.util.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.Timer.*;


public class NewBank {

	private static final NewBank bank = new NewBank();
	public HashMap<String,Customer> customers;
	//public ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // queue for activityQueue method
	//public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // queue for activityQueue method
	public Timer timer = new Timer();

	public HashMap<String,TimerTask> scheduledActions = new HashMap<>();

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
	public synchronized String processRequest(CustomerID customer, String request) throws Exception {
		if(customers.containsKey(customer.getKey())) {
			List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
			switch(input.get(0)) {
				case "1" : return showMyAccounts(customer);
				case "2" : return changeMyAccountName(customer, request);
				case "3" : return transferToExternalUser(customer, request);
				case "4" : return transferToOtherAccount(customer, request);
				case "5" : return createNewAccount(customer, request);
				case "6" : return showQueue();
				case "7" : return cancelAction(request);
				case "DISPLAYSELECTABLEACCOUNTS" : return displaySelectableAccounts(customer);
				case "CREATEACCOUNT" : return createLoginAccount(request);
				default : return "FAIL";
			}
		}
		return "FAIL";
	}

	public synchronized String processAccountCreationRequest(String request) throws Exception {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		switch(input.get(0)) {
			case "CREATEACCOUNT" : return createLoginAccount(request);
			default : return "FAIL";
		}
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

	private String transferToExternalUser(CustomerID customer, String request) throws Exception {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		Customer Receiver = bank.getIndex(input.get(1));

		if(Receiver==null)
		{
			return "No user exists!";
		}
		int authnumber = Integer.valueOf(input.get(3));

		boolean correct = run2FA(authnumber);
		if (correct==true){
			queueAction(customer, request, "transferToExternalUser");
			return "Transfer to external user scheduled";
		} else {
			return "Transfer to external user fail: Authentication failed";
		}
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
		String accountType = input.get(1);
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

	// add transaction to the queue with a time delay of 5 minutes
	private void queueAction(CustomerID customer, String request, String action) {

		// time delay in milliseconds (300000 = 5 minutes)
		int delay = 300000;

		// switch to handle different functions
		if(customers.containsKey(customer.getKey())) {
			switch(action) {
				case "transferToExternalUser" :

					// get recipient, customer and account
					List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
					Customer Receiver = bank.getIndex(input.get(1));
					Double amount = Double.valueOf(input.get(2));
					Account account = customers.get(customer.getKey()).getAccount(input.get(3));

					// build id string
					Date date= new Date();
					Timestamp ts = new Timestamp(date.getTime());
					String id = customer.getKey() + "," + input.get(3) + "," + input.get(1) + "," + input.get(2) + "," + ts.toString().replace(' ', '-');

					// create task
					TimerTask task = new TimerTask() {
						@Override
						public void run() {
							account.transfer(Receiver.getAllAccounts().get(0), amount);
							scheduledActions.remove(id);
						}
					};


					// add task to list
					scheduledActions.put(id, task);

					// schedule task
					timer.schedule(task, delay);
			}
		}
	}

	// show scheduled transactions in the queue
	private String showQueue() {

		// initialize empty string
		StringBuilder queueString = new StringBuilder();

		// print header
		queueString.append("Scheduled Actions:\n");

		// begin transaction id counter
		int menuOption = 1; 

		// get elements and add to queueString
		for (String id:scheduledActions.keySet()) {
			List<String> input = Arrays.asList(id.split("\\s*,\\s*"));
			queueString.append(menuOption).append(". ").append(input.get(1)).append(" --> ").append(input.get(2)).append(": ").append(input.get(3)).append("\n");
			menuOption++;
		}
		return queueString.toString();
	}

	// cancel scheduled action
	private String cancelAction(String index) {

		// get transaction to be cancelled
		List<String> input = Arrays.asList(index.split("\\s*,\\s*"));

		// begin transaction id counter
		int menuOption = 1;

		// iterate accross transactions and increment transaction id counter
		for (String id:scheduledActions.keySet()) {

			// if transaction id matches the transaction to be cancelled id then cancel it, remove it from the HashMap and return success message
			if (menuOption == Integer.parseInt(input.get(1))){
				scheduledActions.get(id).cancel();
				scheduledActions.remove(id);
				return "Transaction cancelled";
			}
			menuOption++;
		}
		// if not found then return error message
		return "Not a valid ID!";
	}

	public boolean run2FA(int authNumber) throws Exception {
		String base32Secret = "NY4A5CPJZ46LXZCP";
		boolean correct = TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret, authNumber, TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS*1000);
		return correct;
	}

	//Password validation (Credit: https://java2blog.com/validate-password-java/)
	public static boolean isValidPassword(String password)
	{
		boolean isValid = true;
		if (password.length() > 15 || password.length() < 8)
		{
			System.out.println("Password must be less than 20 and more than 8 characters in length.");
			isValid = false;
		}
		String upperCaseChars = "(.*[A-Z].*)";
		if (!password.matches(upperCaseChars ))
		{
			System.out.println("Password must have atleast one uppercase character");
			isValid = false;
		}
		String lowerCaseChars = "(.*[a-z].*)";
		if (!password.matches(lowerCaseChars ))
		{
			System.out.println("Password must have atleast one lowercase character");
			isValid = false;
		}
		String numbers = "(.*[0-9].*)";
		if (!password.matches(numbers ))
		{
			System.out.println("Password must have atleast one number");
			isValid = false;
		}
		String specialChars = "(.*[@,#,$,%].*$)";
		if (!password.matches(specialChars ))
		{
			System.out.println("Password must have atleast one special character among @#$%");
			isValid = false;
		}
		return isValid;
	}

	public String createLoginAccount(String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		String userName = input.get(1);
			/*
			out.println("Please create a password:");
			String password = in.readLine();

			//Password validation (Credit: https://java2blog.com/validate-password-java/)
			boolean validPassword = isValidPassword(password);
			while (validPassword==false){
				out.println("Password is not strong enough. Please create a new password:");
				password = in.readLine();
			}

			String AccountDetails = userName + "," + password;

			 */
		Customer newCustomer = new Customer();       // create new customer
		newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
		bank.customers.put(userName, newCustomer);        // add the customer to the list of customers and assign their username
		String output = "Account: '" + userName + "' Created. Please Download the Google Authenticator App and use the key NY4A5CPJZ46LXZCP to set up your 2FA";
		return output;
	}

}

