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
			// ask for user name
			out.println("Enter Username");
			String userName = in.readLine();
			// ask for password
			out.println("Enter Password");
			String password = in.readLine();
			out.println("Checking Details...");
			// authenticate user and get customer ID token from bank for use in subsequent requests
			CustomerID customer = bank.checkLogInDetails(userName, password);
			// if the user is authenticated then get requests from the user and process them 
			if(customer != null) {
				String dashboard = bank.processRequest(customer, "SHOWMYACCOUNTS");
				out.println(dashboard);
				out.println("Log In Successful. What do you want to do?");
				while(true) {
					String request = in.readLine();
					if (request.equals("CHANGEMYACCOUNTNAME")) {
						// Select and validate choice of account to change name
						String selectableAccounts = bank.processRequest(customer, "DISPLAYSELECTABLEACCOUNTS");
						if (selectableAccounts.equals("")){
							out.println("You currently don't have any account at the moment");
							continue;
						}
						out.println("Select the account you wish to edit.");
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
							}catch (NumberFormatException ex) {
								out.println("Please enter an integer only!");
							}
						}
						// Retrieve selected account name
						String accountName = listOfSelections[Integer.parseInt(option)-1].substring(
								selectableAccounts.indexOf(". "));
						accountName = accountName.substring(accountName.indexOf(" ")+1);
						// Request new account name from user
						out.println("Please type in the new name for your selected account.");
						String newAccountName = in.readLine();
						request += "," + accountName + "," + newAccountName;
						// Send request to server and receive response
						String response = bank.processRequest(customer, request);
						out.println(response);
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

}
