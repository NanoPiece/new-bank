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
	public int microLoansIndex = 1;
	//public ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // queue for activityQueue method
	//public ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // queue for activityQueue method
	public Timer timer = new Timer();

	public HashMap<String,TimerTask> scheduledActions = new HashMap<>();

	private NewBank() {
		customers = new HashMap<>();
		microLoans = new HashMap<>();
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

		MicroLoan bhagy1 = new MicroLoan("Bhagy","Buy a new car",
				5000.0, 3.0, microLoansIndex);
		customers.get("Bhagy").addMicroLoanID(bhagy1.getLoanID());
		microLoans.put(Integer.toString(bhagy1.getLoanID()),bhagy1);
		microLoansIndex +=1;

		MicroLoan bhagy2 = new MicroLoan("Bhagy","Buy Playstation 5",
				100.0, 5.0, microLoansIndex);
		customers.get("Bhagy").addMicroLoanID(bhagy2.getLoanID());
		microLoans.put(Integer.toString(bhagy2.getLoanID()),bhagy2);
		microLoansIndex +=1;
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
				case "5a" : return closeAccount(customer, request);
				case "6" : return showQueue();
				case "7" : return cancelAction(request);
				case "MICROLOAN-1" : return showMicroLoanDashboard(customer);
				case "MICROLOAN-2" : return submitLoanApplication(customer, request);
				case "MICROLOAN-3A" : return showAllLoanApplications(customer);
				case "MICROLOAN-3B" : return acceptLoanOffer(customer, request);
				case "MICROLOAN-4" : return cancelLoanApplication(customer, request);
				case "MICROLOAN-5" : return repayLoan(customer, request);
				case "DISPLAYSELECTABLEACCOUNTS" : return displaySelectableAccounts(customer);
				case "NUMBEROFUSERACCOUNTS": return String.valueOf(customers.get(customer.getKey()).numAccounts());
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
		//System.out.println(input.get(1));
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

	// close a customers account
	private String closeAccount(CustomerID customer, String request) {
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		//System.out.println(input.get(1));
		int accountToClose = Integer.parseInt(input.get(1));
		int accountToTransfer = Integer.parseInt(input.get(2));
		Customer thisCustomer = customers.get(customer.getKey());

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
				(customers.get(customer.getKey())).getAssociatedMicroLoanID();
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
		MicroLoan newLoan = new MicroLoan(customer.getKey(), input.get(1),
				Double.parseDouble(input.get(2)), Double.parseDouble(input.get(3)), microLoansIndex);
		customers.get(customer.getKey()).addMicroLoanID(newLoan.getLoanID());
		microLoans.put(Integer.toString(newLoan.getLoanID()),newLoan);
		microLoansIndex += 1;
		return "Your loan application has been submitted to the market.";
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
			if (entry.getValue().getLenderID() == null && !entry.getValue().getBorrowerID().equals(customer.getKey())) {
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
				output += "\n\n";
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
		loan.setLenderID(customer.getKey());
		loan.setStatus("Active");
		// Edit customer detail
		Customer lender = customers.get(customer.getKey());
		lender.addMicroLoanID(loan.getLoanID());
		// Complete money transfer
		Customer borrower = customers.get(loan.getBorrowerID());
		ArrayList<Account> lenderAccounts = lender.getAllAccounts();
		ArrayList<Account> borrowerAccounts = borrower.getAllAccounts();
		lender.getAccount(lenderAccounts.get(0).getAccountName()).withdraw(loan.getTotalAmount());
		borrower.getAccount(borrowerAccounts.get(0).getAccountName()).deposit(loan.getTotalAmount());
		// Return complete message
		return "You have accepted the selected loan application. " +
				"The money is now wired to the lender's default account " +
				"and the interest rate calculation will begin now.";
	}

	private String cancelLoanApplication(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = selected microloan ID
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		MicroLoan loan = microLoans.get(input.get(1));
		if (loan.getStatus().equals("Draft")) {
			Customer borrower = customers.get(customer.getKey());
			borrower.removeMicroLoanID(Integer.parseInt(input.get(1)));
			microLoans.remove(input.get(1));
			return "Your loan application has been canceled";
		} else {
			return "This loan canot be canceled anymore.";
		}
	}

	private String repayLoan(CustomerID customer, String request) {
		// Convert request from String to List
		// index: 0 = requestCommand, 1 = selected microloan ID, 2 = repayment amount
		List<String> input = Arrays.asList(request.split("\\s*,\\s*"));
		MicroLoan loan = microLoans.get(input.get(1));
		Double repayAmount = Double.parseDouble(input.get(2));
		if (loan.getStatus().equals("Active")) {
			Customer borrower = customers.get(customer.getKey());
			Customer lender = customers.get(loan.getLenderID());
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
			return "The status of this loan is not active. Please select a loan that is active.";
		}
	}
}

