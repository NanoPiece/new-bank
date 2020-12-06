package newbank.server;


import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
		Customer bhagy = new Customer();
		bhagy.addAccount(new Account("Current", 1000.0));
		bhagy.addAccount(new SavingsAccount("Savings", 2000.0));
		bhagy.addAccount(new Account("Checking", 3000000.0));
		customers.put("Bhagy", bhagy);

		Customer christina = new Customer();
		christina.addAccount(new SavingsAccount("Savings", 1500.0));
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
				case "6" : return showQueue();
				case "7" : return cancelAction(request);
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
		queueAction(customer, request, "transferToExternalUser");
		return "Transfer to external user scheduled";
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
			thisCustomer.addAccount(new Account(accountType, 00.0));
		}
		if (accountType.equals("2")) {
			accountType = "Savings Account";
			thisCustomer.addAccount(new SavingsAccount(accountType, 00.0));
		}

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
}

