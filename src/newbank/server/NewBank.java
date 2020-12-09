package newbank.server;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.Timer.*;


public class NewBank {

	private static final NewBank bank = new NewBank();
	public HashMap<String,Customer> customers;

	// scheduler for applying interest
	public double interestRate = 0.02;
	public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// scheduled actions
	public Timer timer = new Timer();
	public HashMap<String,TimerTask> scheduledActions = new HashMap<>();

	private NewBank() {
		customers = new HashMap<>();
		addTestData();

		// schedule interest payments
		Calendar startDate = Calendar.getInstance();
		startDate.set(2021, Calendar.JANUARY, 1);
		long oneYearInMilliseconds = 31556952000L;
		long intialDelay = (startDate.getTimeInMillis()-System.currentTimeMillis());
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				payInterest(interestRate);
				System.out.println("HELLO");
			}
		}, intialDelay, oneYearInMilliseconds, TimeUnit.MILLISECONDS);
	}

	private void addTestData() {
		Customer bhagy = new Customer("Bhagy", "bhagy123", "123456");
		bhagy.addAccount(new Account("Current", 1000.0));
		bhagy.addAccount(new Account("Savings", 2000.0));
		bhagy.addAccount(new Account("Checking", 3000000.0));
		customers.put(bhagy.getName(), bhagy);

		Customer christina = new Customer("Christina", "christina123", "123456");
		christina.addAccount(new Account("Savings", 1500.0));
		customers.put(christina.getName(), christina);

		Customer john = new Customer("John", "john123", "123456");
		john.addAccount(new Account("Checking", 250.0));
		customers.put(john.getName(), john);
	}

	public static NewBank getBank() {
		return bank;
	}

	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		for (Map.Entry<String, Customer> customer: customers.entrySet()){
			String username = customer.getValue().getCustomerID().getUserName();
			String pass = customer.getValue().getCustomerID().getPassword();
			if(username.equals(userName)){
				if(pass.equals(password)){
					CustomerID customerID = customer.getValue().getCustomerID();
					return customerID;
				}
			} else {
				continue;
			}
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
				case "6" : return showQueue(request);
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

	private String createLoginAccount(String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		String actualName = input.get(1);
		String userName = input.get(2);
		String password = input.get(3);

		//Password validation (Credit: https://java2blog.com/validate-password-java/)
		boolean validPassword = isValidPassword(password);
		boolean validUsername = isValidUserName(userName);
		if (validPassword==false){
			String output = "Password is not strong enough. Please create a new password:";
			return output;
		} else if (validUsername==false) {
			String output = "The username already exists.\nPlease enter a unique username or type 'esc' to return to the menu screen.";
			return output;
		} else {
			Customer newCustomer = new Customer(actualName, userName, password);       // create new customer
			newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
			bank.customers.put(actualName, newCustomer);        // add the customer to the list of customers and assign their username
			String output = "Account: '" + actualName + "' Created. Please Download the Google Authenticator App and use the key NY4A5CPJZ46LXZCP to set up your 2FA";
			return output;
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
		int authnumber = Integer.parseInt(input.get(4));
		boolean correct = run2FA(authnumber);
		if (correct==true){
			queueAction(customer, request, "transferToExternalUser");
			return "Transfer to external user scheduled";
		}
		return "Transfer to external user failed: Authentication fail";
	}

	private String transferToOtherAccount(CustomerID customer, String request) throws Exception {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));

		int authnumber = Integer.parseInt(input.get(4));
		boolean correct = run2FA(authnumber);
		if (correct==true){
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
		return "Not able to transfer to other account: Authentication fail";
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
		int delay = 30000;

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
	private String showQueue(String request) throws Exception {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		// initialize empty string
		StringBuilder queueString = new StringBuilder();

		// print header
		queueString.append("Scheduled Actions:\n");

		// begin transaction id counter
		int menuOption = 1;
		int authnumber = Integer.parseInt(input.get(1));
		boolean correct = run2FA(authnumber);
		if (correct==true){
			for (String id:scheduledActions.keySet()) {
				// get elements and add to queueString
				List<String> input2 = Arrays.asList(id.split("\\s*,\\s*"));
				queueString.append(menuOption).append(". ").append(input2.get(1)).append(" --> ").append(input2.get(2)).append(": ").append(input2.get(3)).append("\n");
				menuOption++;
			}
			return queueString.toString();
		}
		return "Not able to show scheduled actions: Authentication fail";
	}

	// cancel scheduled action
	private String cancelAction(String index) {

		// get transaction to be cancelled
		List<String> input = Arrays.asList(index.split("\\s*,\\s*"));

		// begin transaction id counter
		int menuOption = 1;

		for (String id:scheduledActions.keySet()) {

			// if transaction id matches the transaction to be cancelled id then cancel it, remove it from the HashMap and return success message
			if (menuOption == Integer.parseInt(input.get(1))){
				scheduledActions.get(id).cancel();
				scheduledActions.remove(id);
				return "Transaction cancelled";
			}
			menuOption++;
		}

		if (Integer.parseInt(input.get(1))==0) {
			return "Back to main menu";
		}
		// if not found then return error message
		return "Not a valid ID!";

	}

	// add yearly interest
	public void payInterest(double rate) {
		for (String customerName: customers.keySet()) {
			for (Account account: customers.get(customerName).getAllAccounts()) {
				if (account.isSavingsAccount()) {
					// calculate interest to be added
					double interest = rate * account.getOpeningBalance();

					// add interest to account
					account.setAmount(account.getOpeningBalance() + interest);
				}
			}
		}
	}

	public boolean run2FA(int authNumber) throws Exception {
		String base32Secret = "NY4A5CPJZ46LXZCP";
		boolean correct = TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret, authNumber, TimeBasedOneTimePasswordUtil.DEFAULT_TIME_STEP_SECONDS*1000);
		return correct;
	}

	public boolean isValidUserName(String userName) {
		for (Map.Entry<String, Customer> customer: customers.entrySet()){
			String username = customer.getValue().getCustomerID().getUserName();
			if(username.equals(userName)){
				return false;
			} else {
				continue;
			}
		}
		return true;
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
			System.out.println("Password must have at least one uppercase character");
			isValid = false;
		}
		String lowerCaseChars = "(.*[a-z].*)";
		if (!password.matches(lowerCaseChars ))
		{
			System.out.println("Password must have at least one lowercase character");
			isValid = false;
		}
		String numbers = "(.*[0-9].*)";
		if (!password.matches(numbers ))
		{
			System.out.println("Password must have at least one number");
			isValid = false;
		}
		String specialChars = "(.*[@,#,$,%].*$)";
		if (!password.matches(specialChars ))
		{
			System.out.println("Password must have at least one special character among @#$%");
			isValid = false;
		}
		return isValid;
	}




}

