
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

		public void run() {
			// keep getting requests from the client and processing them
			try {
				clearScreen();
				out.println("Welcome to New Bank\n\nPlease choose from the options below:\n");      // offer log in or create account
				out.println("1. Log In");
				out.println("2. Create User");
				String customerAction = in.readLine();

				while (!(customerAction.equals("1")) && (!(customerAction.equals("2")))) {
					out.println("Please try again");          // ensure customer's entry is valid
					customerAction = in.readLine();
				}
				if (customerAction.equals("2")) {
					out.println("What is your name?");
					String actualName = in.readLine();
					out.println("Please create a username:");
					String userName = in.readLine();
					out.println("Please create a password:");
					String password = in.readLine();

					String request = "CREATEACCOUNT" + "," + actualName + "," + userName + "," + password;
					String response = bank.processAccountCreationRequest(request);
					out.println(response);          // direct to account creation where they will be able to choose a username and continue
					while(response == "Password is not strong enough. Please create a new password:"){
						password = in.readLine();
						request = "CREATEACCOUNT" + "," + actualName + "," + userName + "," + password;
						response = bank.processAccountCreationRequest(request);
						out.println(response);
					}

					while (response == "The username already exists.\nPlease enter a unique username or type 'esc' to return to the menu screen.") {
						userName = in.readLine();
						if (userName.equals("esc")){
							out.println("Loading menu screen...\n");
							sleep();
							break;
						}
					}
					if (!userName.equals("esc")) {
						clearScreen();
						out.println("User: '" + userName + "' Created\n");
						out.println("Loading menu screen...\n");
						sleep();
						run();
					} else if (userName.equals("esc")){
						out.println("Loading menu screen...\n");
						sleep();
						run();
					}

				}
				if (customerAction.equals("1")) {
					clearScreen();
					out.println("Enter Username");              // ask for user name
				}
				String userName = in.readLine();
				// ask for password
				out.println("Enter Password");
				String password = in.readLine();
				out.println("Checking Details...");

				// authenticate user and get customer ID token from bank for use in subsequent requests
				CustomerID customer = bank.checkLogInDetails(userName, password);
				while (customer==null){
					out.println("Log In Failed. Please try again");
					out.println("Enter Username");
					userName = in.readLine();
					// ask for password
					out.println("Enter Password");
					password = in.readLine();
					out.println("Checking Details...");
					customer = bank.checkLogInDetails(userName, password);
				}
				// if the user is authenticated then get requests from the user and process them

				//Customer current= bank.getIndex(userName);
				out.println("Log In Successful. What do you want to do?");
				while(true) {
					out.println(showMenu());
					String request = in.readLine();
					if (request.equals("1")){
						clearScreen();
						String dashboard = bank.processRequest(customer, "1");
						out.println(dashboard);
						returnToMenu();
					} else if(request.equals("2")){
						clearScreen();
						out.println("Enter the Name of Receiver: ");
						String receiver = in.readLine();

						out.println("Enter the IBAN of Receiver: ");
						String iban = in.readLine();

						out.println("Enter the Amount to transfer:  ");
						String amount_totransfer = in.readLine();

						boolean valid = false;
						while(!valid){
							try{
								int check = Integer.parseInt(amount_totransfer);
								valid = true;
							}catch (NumberFormatException ex) {
								out.println("Invalid input, please try again.\n");
								out.println("Enter the Amount to transfer:  ");
								amount_totransfer = in.readLine();
							}
						}

						out.println("Enter the Account that you want to transfer from:  ");
						String accountName = SelectAccount(customer);

						out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
						String authNumber = in.readLine();

						request += "," + receiver + "," + iban + "," + amount_totransfer + "," + accountName + "," + authNumber;

						String response = bank.processRequest(customer, request);
						out.println(response);
						returnToMenu();

					} else if (request.equals("3")){
						clearScreen();
						out.println("Enter the Account that you want to transfer from:  ");
						String account_from = SelectAccount(customer);

						out.println("Enter the Account that you want to transfer to:  ");
						String account_to = SelectAccount(customer);

						out.println("Enter the Amount to transfer:  ");
						String amount_totransfer = in.readLine();

						boolean valid = false;
						while(!valid){
							try{
								int check = Integer.parseInt(amount_totransfer);
								valid = true;
							}catch (NumberFormatException ex) {
								out.println("Invalid input, please try again.\n");
								out.println("Enter the Amount to transfer:  ");
								amount_totransfer = in.readLine();
							}
						}

						out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
						String authNumber = in.readLine();

						request += "," + account_from + "," + account_to + "," + amount_totransfer + "," + authNumber;

						String response = bank.processRequest(customer, request);
						out.println(response);
						returnToMenu();

					} else if (request.equals("5")) {
						clearScreen();

						// check if user has accounts to close
						if (Integer.parseInt(bank.processRequest(customer, "NUMBEROFUSERACCOUNTS")) == 0) {
							out.println("User has no accounts!\n");
						} else if (Integer.parseInt(bank.processRequest(customer, "NUMBEROFUSERACCOUNTS")) == 1) { // do not allow to close if only one account remains
							out.println("User must have at least one account!");
						} else {
							// show accounts
							out.println(bank.processRequest(customer, "1"));

							// get account to close
							out.println("Select which account you wish to close (Type in the number on the left of the account name):\n");
							String accountToClose = in.readLine();
							request += "," + accountToClose;

							String accountToTransferFundsTo;

							// get account to transfer money to
							do {
								clearScreen();
								out.println(bank.processRequest(customer, "1"));
								out.println("Select which account you wish to transfer the remaining funds to (Type in the number on the left of the account name):\n");
								accountToTransferFundsTo = in.readLine();
								if (accountToTransferFundsTo.equals(accountToClose)) {
									out.println("Can't be the same account.\n");
								}
							} while (accountToTransferFundsTo.equals(accountToClose));
							request += "," + accountToTransferFundsTo;

							// send request and get response
							String response = bank.processRequest(customer, request);
							out.println(response);
						}

						// return menu
						returnToMenu();
					} else if (request.equals("4")){
						clearScreen();
						out.println("Please select an account type:\n");
						out.println("1. Current Account");  // take account type
						out.println("2. Savings Account");
						String accountType = in.readLine();

						out.println("Please select how much do you want to put in to this new account:\n");
						String amountToAdd = in.readLine();

						boolean valid = false;
						while(!valid){
							try{
								int check = Integer.parseInt(amountToAdd);
								while (check==0.0){
									out.println("You cannot create a new account without adding any funds in it.\n");
									out.println("Please select how much do you want to put in to this new account:\n");
									amountToAdd = in.readLine();
									check = Integer.parseInt(amountToAdd);
								}
								valid = true;
							}catch (NumberFormatException ex) {
								out.println("Invalid input, please try again.\n");
								out.println("Please select how much do you want to put in to this new account:\n");
								amountToAdd = in.readLine();
							}
						}

						request += "," + accountType + "," + amountToAdd;
						while (!(accountType.equals("1")) && (!(accountType.equals("2")))) {
							out.println("Please try again");   // ensure customer's entry is valid
						}
						String response = bank.processRequest(customer, request);
						out.println(response);

					} else if (request.equals("6")){
						out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
						String authNumber = in.readLine();
						request += "," + authNumber;
						String response = bank.processRequest(customer, request);
						out.println(response);

					} else if (request.equals("7")){
						// cancel a scheduled transfer
						// show scheduled transfers
						out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
						String authNumber = in.readLine();
						request = "6" + "," + authNumber;
						String response = bank.processRequest(customer, request);
						out.println(response);
						while (response.equals("Not able to show scheduled actions: Authentication fail")){
							out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
							authNumber = in.readLine();
							request = "6" + "," + authNumber;
							response = bank.processRequest(customer, request);
							out.println(response);
						}
						// get id of transfer to be cancelled
						out.println("Enter number of transaction you wish to cancel:");
						String cancelTransaction = in.readLine();
						request = "7" + "," + cancelTransaction + "," + authNumber;

						response = bank.processRequest(customer, request);
						out.println(response);
					} else if (request.equals("8")) {
						clearScreen();
						out.println("Logging out...");
						sleep();
						run();
					} else if (request.equals("9")){
						clearScreen();
						out.println("Thank you and have a nice day!");
						System.exit(0);
					} else if (request.equals("10")) {
						clearScreen();
						out.println("Welcome to Micro-Loan!");
						while (!request.equals("6")) {
							out.println(showMicroLoanOptions());
							request = in.readLine();
							if (request.equals("1")){
								// 1. View Micro-Loan Dashboard
								clearScreen();
								request = "MICROLOAN-1";
								String response = bank.processRequest(customer, request);
								out.println(response);
							} else if (request.equals("2")) {
								// 2. Apply For New Loan
								clearScreen();
								request = "MICROLOAN-2";
								out.println("How much would you like to apply for? (0.0 - 10000.0)");
								String loanAmount = in.readLine();
								while (!checkValidInputRange(loanAmount, 0.0, 10000.0)) {
									out.println("Please enter a valid figure.");
									loanAmount = in.readLine();
								}
								out.println("What is the purpose for your loan?");
								String description = in.readLine();
								out.println("At what interest rate would you like to repay this loan at? " +
										"(0.0 to 10.0 per year)");
								String interestRate = in.readLine();
								while (!checkValidInputRange(interestRate, 0.0, 10.0)){
									out.println("Please enter a valid figure.");
									interestRate = in.readLine();
								}
								request += "," + description + "," + loanAmount + "," + interestRate;
								String response = bank.processRequest(customer, request);
								out.println(response);
								out.println("");
								clearScreen();
							} else if (request.equals("3")) {
								// 3. Accept Loan
								clearScreen();
								request = "MICROLOAN-3A";
								out.println("Please select which loan you would like to provide money for by entering the ID.");
								// Display selectable loans
								Boolean validInput = false;
								String response = bank.processRequest(customer, request);
								out.println(response);
								String selectedMicroLoan = "";
								// Confirm input
								while (!validInput){
									selectedMicroLoan = in.readLine();
									String lines[] = response.split("\\r?\\n");
									for (int i=2; i<lines.length; i++) {
										String line[] = lines[i].split("\\s+");
										if (line[0].equals(selectedMicroLoan)) {
											validInput = true;
										}
									}
									if (!validInput) {
										out.println("Please enter a valid ID.");
									}
								}
								// Send request and print response
								request = "MICROLOAN-3B" + "," + selectedMicroLoan;
								response = bank.processRequest(customer, request);
								out.println(response);
							} else if (request.equals("4")) {
								// 4. Cancel Loan Application
								clearScreen();
								request = "MICROLOAN-1";
								String response = bank.processRequest(customer, request);
								out.println("Please select the loan that you want to cancel by typing in the ID.");
								out.println(response);
								boolean validInput = false;
								String selectedMicroLoan = "";
								// Confirm input
								while (!validInput){
									selectedMicroLoan = in.readLine();
									String lines[] = response.split("\\r?\\n");
									for (int i=2; i<lines.length; i++) {
										String line[] = lines[i].split("\\s+");
										if (line[0].equals(selectedMicroLoan)) {
											validInput = true;
										}
									}
									if (!validInput) {
										out.println("Please enter a valid ID.");
									}
								}
								// Send request
								request = "MICROLOAN-4" + "," + selectedMicroLoan;
								response = bank.processRequest(customer, request);
								out.println(response);
							} else if (request.equals("5")) {
								// 5. Repay loan
								clearScreen();
								request = "MICROLOAN-1";
								String response = bank.processRequest(customer, request);
								out.println("Please select the loan that you want to repay by typing in the ID.");
								out.println(response);
								boolean validInput = false;
								String selectedMicroLoan = "";
								// Confirm ID input
								while (!validInput){
									selectedMicroLoan = in.readLine();
									String lines[] = response.split("\\r?\\n");
									String reply = "Please enter a valid ID.";
									for (int i=2; i<lines.length; i++) {
										String line[] = lines[i].split("\\s+");
										if (line[0].equals(selectedMicroLoan)) {
											validInput = true;
										}
									}
									if (!validInput) {
										out.println(reply);
									}
								}
								// Confirm amount
								out.println("Please enter the amount you would like to repay (Up to total amount outstanding: ");
								String repayAmount = "";
								Boolean validated = false;
								while (!validated){
									try {
										repayAmount = in.readLine();
										Double n = Double.parseDouble(repayAmount);
										if (n > 0) {
											validated = true;
										}
									} catch (Exception e) {
										out.println("Please enter a valid number");
									}
								}
								// Send request
								request = "MICROLOAN-5" + "," + selectedMicroLoan + "," + repayAmount;
								response = bank.processRequest(customer, request);
								out.println(response);
							} else if (request.equals("6")) {
								// 6. Return to Main Menu
								returnToMenu();
							} else {
								// Unrecognisable input
								clearScreen();
								out.print("Invalid Entry\n");
							}
						}
					} else if(!request.equalsIgnoreCase("10")) {
						clearScreen();
						out.println("Invalid Entry\n");
					} else {
						System.out.println("Request from " + customer.getName());
						String responce = bank.processRequest(customer, request);
						out.println(responce);
						returnToMenu();
					}
				}

			} catch (Exception e) {
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

	private String
	showMenu() {
		return "Please choose from the options below: \n\n" +
				"1. Show My Accounts\n" +
				"2. Transfer to another user\n" +
				"3. Transfer to another owned account\n" +
				"4. Create New Account\n" +
				"5. Close an Account\n" +
				"6. Show scheduled transfers\n" +
				"7. Cancel a scheduled transfer\n" +
				"8. Log out\n" +
				"9. Quit\n" +
				"10. Micro-Loan";
	}

	private String showMicroLoanOptions() {
		return "Please choose from the options below: \n\n" +
				"1. View My Micro-Loan Dashboard\n" +
				"2. Apply For New Loan\n" +
				"3. Provide Money to Loan\n" +
				"4. Cancel Loan Application\n" +
				"5. Repay loan\n" +
				"6. Return to Main Menu";
	}

	private Boolean checkValidInputRange(String amount, Double min, Double max) {
		Double test;
		try {
			test = Double.parseDouble(amount);
		} catch (Exception e) {
			return false;
		}
		if (test >= min && test <= max) {
			return true;
		} else {
			return false;
		}
	}

	public void clearScreen() {
		out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"); // utilised while this function will not work in IntelliJ
		out.print("\033[H\033[2J");
		out.flush();
	}

	private void returnToMenu() throws InterruptedException { ;
		out.println("\nReturning to menu screen...");
		sleep();
		clearScreen();
		showMenu();
	}

	private void sleep() throws InterruptedException {
		Thread.sleep(3000);
	}

	private String SelectAccount(CustomerID customer) throws Exception {
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
