
package newbank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NewBankClientHandler extends Thread{

	private NewBank bank;
	private BufferedReader in;
	private PrintWriter out;

	public NewBankClientHandler(Socket s) throws IOException {
		bank = NewBank.getBank();
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = new PrintWriter(s.getOutputStream(), true);
	}

	public void createUser() {
		try {
			out.println("Please create a username:");
			String userName = in.readLine();         // accept a username from customer
			Customer newCustomer = new Customer();       // create new customer
			newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
			bank.customers.put(userName, newCustomer);        // add the customer to the list of customers and assign their username
			out.println("User: '" + userName + "' Created");
		} catch (IOException e) {
			e.printStackTrace();
		}
		run();
	}

	public void run() {
		// keep getting requests from the client and processing them
		try {
			out.println("Please Choose From The Below Options:\n");      // offer log in or create account
			out.println("1. Log In");
			out.println("2. Create User");
			String customerAction = in.readLine();

			while (!(customerAction.equals("1")) && (!(customerAction.equals("2")))) {
				out.println("Please try again");          // ensure customer's entry is valid
				customerAction = in.readLine();
			}
			if (customerAction.equals("2")) {
				createUser();                // direct to account creation where they will be able to choose a username and continue
			}
			if (customerAction.equals("1")) {
				// ask for user name
				out.println("Enter Username");
			}
			String userName = in.readLine();
			// ask for password
			out.println("Enter Password");
			String password = in.readLine();
			out.println("Checking Details...");
			// authenticate user and get customer ID token from bank for use in subsequent requests
			CustomerID customer = bank.checkLogInDetails(userName, password);
			// if the user is authenticated then get requests from the user and process them
			if(customer != null) {
				//Customer current= bank.getIndex(userName);
				out.println("Log In Successful. What do you want to do?");
				while(true) {
					out.println(showMenu());
					String request = in.readLine();
					if (request.equals("1")){
						String dashboard = bank.processRequest(customer, "1");
						out.println(dashboard);
					} else if (request.equals("2")){
						out.println("Enter the Account that you want to change the name for:  ");
						String accountName = SelectAccount(customer);

						// Request new account name from user
						out.println("Please type in the new name for your selected account.");
						String newAccountName = in.readLine();
						newAccountName = newAccountName.trim();

						request += "," + accountName + "," + newAccountName;
						// Send request to server and receive response
						String response = bank.processRequest(customer, request);
						out.println(response);

					} else if(request.equals("3")){

						out.println("Enter the Username of Receiver: ");
						String receiver = in.readLine();

						out.println("Enter the Amount to transfer:  ");
						String amount_totransfer = in.readLine();

						out.println("Enter the Account that you want to transfer from:  ");
						String accountName = SelectAccount(customer);

						request += "," + receiver + "," + amount_totransfer + "," + accountName;

						// Send request to server and receive response
						String response = bank.processRequest(customer, request);
						out.println(response);

					} else if (request.equals("4")){

						out.println("Enter the Account that you want to transfer from:  ");
						String account_from = SelectAccount(customer);

						out.println("Enter the Account that you want to transfer to:  ");
						String account_to = SelectAccount(customer);

						out.println("Enter the Amount to transfer:  ");
						String string_amount = in.readLine();

						request += "," + account_from + "," + account_to + "," + string_amount;
						// Send request to server and receive response
						String response = bank.processRequest(customer, request);
						out.println(response);

					} else if (request.equals("5")){
						out.println("Please select an account type:\n");
						out.println("1. Current Account");  // take account type
						out.println("2. Savings Account");
						String accountType = in.readLine();
						request += "," + accountType;
						while (!(accountType.equals("1")) && (!(accountType.equals("2")))) {
							out.println("Please try again");   // ensure customer's entry is valid
						}
						String response = bank.processRequest(customer, request);
						out.println(response);
					} else if (request.equals("6")){
						out.println("Thank you and have a nice day!");
						System.exit(0);
					} else if(!request.equalsIgnoreCase("6")) {
						out.println("Wrong choice enter again.");
					} else {
						System.out.println("Request from " + customer.getKey());
						String responce = bank.processRequest(customer, request);
						out.println(responce);
					}
				}
			}
			else {
				out.println("Log In Failed");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	private String showMenu()
	{
		return "1. Show My Accounts\n2. Change Account Names\n3. Transfer to another user\n4. Transfer to another owned account\n5. Create New Account\n6. Quit";
	}

	private String SelectAccount(CustomerID customer){
		//out.println("Enter the Account that you want to transfer from:  ");
		String selectableAccounts = bank.processRequest(customer, "DISPLAYSELECTABLEACCOUNTS");
		String option = "";
		String[] listOfSelections = selectableAccounts.split("\\n");
		boolean b = true;
		while (b){
			try{
				out.println(selectableAccounts);
				option = in.readLine();
				option = option.trim();
				while (Integer.parseInt(option) > listOfSelections.length ||
						Integer.parseInt(option) <= 0){
					out.println("Please select a valid option:");
					out.println(selectableAccounts);
					option = in.readLine();
					option = option.trim();
				}
				b = false;
			}catch (NumberFormatException | IOException ex) {
				out.println("Please enter an integer only!");
			}
		}
		// Retrieve selected account name
		String accountName = listOfSelections[Integer.parseInt(option)-1].substring(
				selectableAccounts.indexOf(". "));

		return accountName.substring(accountName.indexOf(" ")+1);
	}

}
