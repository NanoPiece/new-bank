package newbank.server;


import java.sql.Array;
import java.util.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.Timer.*;


public class NewBank {

	private static final NewBank bank = new NewBank();
	public HashMap<String,Customer> customers;
	public HashMap<String,MicroLoan> microLoans;
	public int microLoansIndex = 3;


	// scheduler for applying interest
	public double interestRate = 0.02;
	public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// scheduled actions
	public Timer timer = new Timer();
	public HashMap<String,TimerTask> scheduledActions = new HashMap<>();
	private Customer Receiver;

	private NewBank() {
		customers = new HashMap<>();
		microLoans = new HashMap<>();
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
		Customer bhagy = new Customer("Bhagy", "bhagy123", "123456", "GB24NWBK99999911111111");
		bhagy.addAccount(new Account("Current", 1000.0));
		bhagy.addAccount(new Account("Savings", 2000.0));
		bhagy.addAccount(new Account("Checking", 3000000.0));
		customers.put(bhagy.getIBAN(), bhagy);

		Customer john = new Customer("John", "john123456", "123456", "GB24NWBK99999922222222");
		john.addAccount(new Account("Savings", 1500.0));
		customers.put(john.getIBAN(), john);

		Customer john2 = new Customer("John", "john123", "123456", "GB24NWBK99999933333333");
		john2.addAccount(new Account("Checking", 250.0));
		customers.put(john2.getIBAN(), john2);

		MicroLoan bhagy1 = new MicroLoan("Bhagy","Buy a new car",
				5000.0, 3.0, microLoans.size()+1, bhagy.getIBAN());
		customers.get(bhagy.getIBAN()).addMicroLoanID(bhagy1.getLoanID());
		microLoans.put(Integer.toString(bhagy1.getLoanID()),bhagy1);

		MicroLoan bhagy2 = new MicroLoan("Bhagy","Buy Playstation 5",
				1200.0, 5.0, microLoans.size()+1, bhagy.getIBAN());
		customers.get(bhagy.getIBAN()).addMicroLoanID(bhagy2.getLoanID());
		microLoans.put(Integer.toString(bhagy2.getLoanID()),bhagy2);
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
		if(customers.containsKey(customer.getIBAN())) {
			List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
			switch(input.get(0)) {
				case "1" : return showMyAccounts(customer);
				case "3" : return transferToExternalUser(customer, request);
				case "4" : return transferToOtherAccount(customer, request);
				case "5" : return createNewAccount(customer, request);
				case "5a" : return closeAccount(customer, request);
				case "6" : return showQueue(request);
				case "7" : return cancelAction(request);
				case "MICROLOAN-1" : return showMicroLoanDashboard(customer);
				case "MICROLOAN-2" : return submitLoanApplication(customer, request);
				case "MICROLOAN-3A" : return showAllLoanApplications(customer);
				case "MICROLOAN-3B" : return acceptLoanOffer(customer, request);
				case "MICROLOAN-4" : return cancelLoanApplication(customer, request);
				case "MICROLOAN-5" : return repayLoan(customer, request);
				case "DISPLAYSELECTABLEACCOUNTS" : return displaySelectableAccounts(customer);
				case "NUMBEROFUSERACCOUNTS": return String.valueOf(customers.get(customer.getIBAN()).numAccounts());
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

	public String generateIBAN() {
		int accountNumber = 10000000;
		Random ID = new Random();
		accountNumber += ID.nextInt(90000000);
		String IBAN = "GB24NWBK999999" + accountNumber;
		return IBAN;
	}

	private String createLoginAccount(String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		String actualName = input.get(1);
		String userName = input.get(2);
		String password = input.get(3);
		String iban = generateIBAN();

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
			Customer newCustomer = new Customer(actualName, userName, password, iban);       // create new customer
			newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
			bank.customers.put(iban, newCustomer);        // add the customer to the list of customers and assign their username
			String output = "Account: '" + actualName + "' Created. Please Download the Google Authenticator App and use the key NY4A5CPJZ46LXZCP to set up your 2FA";
			return output;
		}
	}

	private String showMyAccounts(CustomerID customer) {
		return (customers.get(customer.getIBAN())).accountsToString();
	}

	private String displaySelectableAccounts(CustomerID customer) {
		ArrayList<Account> accounts = customers.get(customer.getIBAN()).getAllAccounts();
		String output = "";
		for(int i=1; i<= accounts.size(); i++){
			String account = accounts.get(i-1).getAccountName();
			output += i + ". " + account + "\n";
		}
		return output;
	}

	private String transferToExternalUser(CustomerID customer, String request) throws Exception {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));

		Customer Receiver = bank.getIndex(input.get(2));

		if(Receiver==null)
		{
			return "No user exists!";
		}

		int authnumber = Integer.parseInt(input.get(5));
		boolean correct = run2FA(authnumber);
		if (correct == true) {
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
			if(customers.get(customer.getIBAN()).getAllAccounts().size()<2)
			{
				return "You don't have 2 accounts!";
			}
			Account account_from = customers.get(customer.getIBAN()).getAccount(input.get(1));
			Account account_to = customers.get(customer.getIBAN()).getAccount(input.get(2));
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
		Customer thisCustomer = customers.get(customer.getIBAN());
		if (accountType.equals("1")) {
			accountType = "Current Account";
		}
		if (accountType.equals("2")) {
			accountType = "Savings Account";
		}
		thisCustomer.addAccount(new Account(accountType, 00.0));
		return "Account '" + accountType + "' Created.\n";
	}

	// close a customers account
	private String closeAccount(CustomerID customer, String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		//System.out.println(input.get(1));
		int accountToClose = Integer.parseInt(input.get(1));
		int accountToTransfer = Integer.parseInt(input.get(2));
		Customer thisCustomer = customers.get(customer.getIBAN());

		int accountIndex = 1;
		Account transferAccount = null;

		// get account to transfer to
		for (Account account : thisCustomer.getAllAccounts()) {
			if (accountIndex == accountToTransfer) {
				transferAccount = account;
				break;
			}
			accountIndex++;
		}

		if (transferAccount == null) {
			return "Not a valid transfer account!";
		}

		accountIndex = 1;

		for (Account account: thisCustomer.getAllAccounts()) {
			if (accountIndex == accountToClose) {
				account.transfer(transferAccount, account.getOpeningBalance());
				thisCustomer.closeAccount(account);
				return "Account closed.";
			}
			accountIndex++;
		}
		return "Not a valid choice.";
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
		if(customers.containsKey(customer.getIBAN())) {
			switch(action) {
				case "transferToExternalUser" :

					// get recipient, customer and account
					List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
					Customer Receiver = bank.getIndex(input.get(2));
					Double amount = Double.valueOf(input.get(3));
					Account account = customers.get(customer.getIBAN()).getAccount(input.get(3));

					// build id string
					Date date= new Date();
					Timestamp ts = new Timestamp(date.getTime());
					String id = customer.getIBAN() + "," + input.get(4) + "," + input.get(1) + "," + input.get(3) + "," + ts.toString().replace(' ', '-');

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


	private String showMicroLoanDashboard(CustomerID customer) {
		// Header
		String header = "ID" + "   " + "Borrower" + "     " + "Lender" + "     " + "Description" +
				"               " + "Total Amount" + "   " + "Outstanding Amount" + "   " +
				"Interest (%/Y)" + "   " + "Status" + "\n";
		String output = "";
		output += header;
		// Divider
		for (int i=0; i<header.length(); i++){
			output += "-";
		}
		output += "-----";
		output += "\n";
		// Content
		ArrayList<Integer> associatedMicroLoanID =
				(customers.get(customer.getIBAN())).getAssociatedMicroLoanID();
		for (Integer loanID : associatedMicroLoanID) {
			MicroLoan loan = microLoans.get(Integer.toString(loanID));
			output += Integer.toString(loan.getLoanID());
			output += emptySpaceNeedForMicroLoanDashboard("ID", Integer.toString(loan.getLoanID()),
					3);
			output += loan.getBorrowerID();
			output += emptySpaceNeedForMicroLoanDashboard("Borrower", loan.getBorrowerID(),
					5);
			output += loan.getLenderID();
			output += emptySpaceNeedForMicroLoanDashboard("Lender", loan.getLenderID(),
					5);
			output += loan.getDescription();
			output += emptySpaceNeedForMicroLoanDashboard("Description", loan.getDescription(),
					15);
			output += loan.getTotalAmount();
			output += emptySpaceNeedForMicroLoanDashboard("Total Amount",
					Double.toString(loan.getTotalAmount()), 3);
			output += loan.getOutstandingAmount();
			output += emptySpaceNeedForMicroLoanDashboard("Outstanding Amount",
					Double.toString(loan.getOutstandingAmount()), 3);
			output += loan.getAnnualInterestRate();
			output += emptySpaceNeedForMicroLoanDashboard("Interest (%/Y)",
					Double.toString(loan.getAnnualInterestRate()), 3);
			output += loan.getStatus();
			output += "\n";
		}
		return output;
	}
	private String emptySpaceNeedForMicroLoanDashboard(String header, String value,
													   int defaultEmptySpaceAfterHeader) {
		String output = "";
		int spaceRequired = 0;
		if (value == null) {
			value = "null";
		}
		if (header.length() == value.length()) {
			spaceRequired = defaultEmptySpaceAfterHeader;
		} else {
			spaceRequired = header.length()-value.length()+defaultEmptySpaceAfterHeader;
			if (spaceRequired<0) {
				spaceRequired = 0;
			}
		}
		for (int i=0; i<spaceRequired; i++){
			output += " ";
		}
		return output;
	}

	private String submitLoanApplication(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = Description, 2 = Loan Amount, 3 = interest rate
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		MicroLoan newLoan = new MicroLoan(customer.getName(), input.get(1),
				Double.parseDouble(input.get(2)), Double.parseDouble(input.get(3)), microLoansIndex,
				customer.getIBAN());
		customers.get(customer.getIBAN()).addMicroLoanID(newLoan.getLoanID());
		microLoans.put(Integer.toString(newLoan.getLoanID()),newLoan);
		microLoansIndex += 1;
		return "Your loan application has been submitted.";
	}

	private String showAllLoanApplications(CustomerID customer) {
		// Header
		String header = "ID" + "   " + "Borrower" + "     " + "Lender" + "     " + "Description" +
				"               " + "Total Amount" + "   " + "Outstanding Amount" + "   " +
				"Interest (%/Y)" + "   " + "Status" + "\n";
		String output = "";
		output += header;
		// Divider
		for (int i=0; i<header.length(); i++){
			output += "-";
		}
		output += "-----";
		output += "\n";
		// Entries
		for (Map.Entry<String, MicroLoan> entry : microLoans.entrySet() ) {
			if (entry.getValue().getLenderID() == null && !entry.getValue().getBorrowerIBAN().equals(customer.getIBAN())){
				MicroLoan loan = entry.getValue();
				output += Integer.toString(loan.getLoanID());
				output += emptySpaceNeedForMicroLoanDashboard("ID", Integer.toString(loan.getLoanID()),
						3);
				output += loan.getBorrowerID();
				output += emptySpaceNeedForMicroLoanDashboard("Borrower", loan.getBorrowerID(),
						5);
				output += loan.getLenderID();
				output += emptySpaceNeedForMicroLoanDashboard("Lender", loan.getLenderID(),
						5);
				output += loan.getDescription();
				output += emptySpaceNeedForMicroLoanDashboard("Description", loan.getDescription(),
						15);
				output += loan.getTotalAmount();
				output += emptySpaceNeedForMicroLoanDashboard("Total Amount",
						Double.toString(loan.getTotalAmount()), 3);
				output += loan.getOutstandingAmount();
				output += emptySpaceNeedForMicroLoanDashboard("Outstanding Amount",
						Double.toString(loan.getOutstandingAmount()), 3);
				output += loan.getAnnualInterestRate();
				output += emptySpaceNeedForMicroLoanDashboard("Interest (%/Y)",
						Double.toString(loan.getAnnualInterestRate()), 3);
				output += loan.getStatus();
				output += "\n";
			}
		}

		return output;
	}

	private String acceptLoanOffer(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = selected microloan key
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		// Edit microloan details
		MicroLoan loan = microLoans.get(input.get(1));
		loan.setLenderIBAN(customer.getIBAN());
		loan.setLenderID(customer.getName());
		loan.setStatus("Active");
		// Edit customer detail
		Customer lender = customers.get(customer.getIBAN());
		lender.addMicroLoanID(loan.getLoanID());
		// Complete money transfer
		Customer borrower = customers.get(loan.getBorrowerIBAN());
		ArrayList<Account> lenderAccounts = lender.getAllAccounts();
		ArrayList<Account> borrowerAccounts = borrower.getAllAccounts();
		lender.getAccount(lenderAccounts.get(0).getAccountName()).withdraw(loan.getTotalAmount());
		borrower.getAccount(borrowerAccounts.get(0).getAccountName()).deposit(loan.getTotalAmount());
		// Return complete message
		return "You have accepted the loan application. " +
				"The money is now wired to the lender's default account " +
				"and the interest rate calculation will begin now.";
	}

	private String cancelLoanApplication(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = selected microloan ID
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		MicroLoan loan = microLoans.get(input.get(1));
		if (loan.getStatus().equals("Draft")) {
			Customer borrower = customers.get(customer.getIBAN());
			borrower.removeMicroLoanID(Integer.parseInt(input.get(1)));
			microLoans.remove(input.get(1));
			return "Your loan application has been canceled";
		} else {
			return "This loan can no longer be canceled.";
		}
	}

	private String repayLoan(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = selected microloan ID, 2 = repayment amount
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		MicroLoan loan = microLoans.get(input.get(1));
		Double repayAmount = Double.parseDouble(input.get(2));
		if (loan.getStatus().equals("Active")) {
			Customer borrower = customers.get(customer.getIBAN());
			Customer lender = customers.get(loan.getLenderIBAN());
			// Validate appropriate amount
			if (repayAmount <= loan.getOutstandingAmount()) {
				// Complete money transfer
				ArrayList<Account> lenderAccounts = lender.getAllAccounts();
				ArrayList<Account> borrowerAccounts = borrower.getAllAccounts();
				lender.getAccount(lenderAccounts.get(0).getAccountName()).deposit(repayAmount);
				borrower.getAccount(borrowerAccounts.get(0).getAccountName()).withdraw(repayAmount);
				// Update loan status and attributes
				loan.updatePaidAmount(repayAmount);
				loan.updateOutstandingAmount();
				if (loan.getOutstandingAmount() == 0.0) {
					loan.setStatus("Closed");
					return "You have fully repaid your loan. This loan will be closed.";
				} else {
					String amount = Double.toString(loan.getOutstandingAmount());
					return "Repayment has been successful. The loan's current outstanding amount is: " + amount;
				}
			} else {
				return "You tried to repay more than the total amount outstanding. Process has not been completed. " +
						"Please try again.";
			}
		} else {
			return "This loan is not currently active. Please select an active loan.";
		}
	}

}

