package newbank.server;
import java.util.Random;

public class CustomerID {
	private String key;
	
	public CustomerID(String key) {
		this.key = key;
	}

	/*public static String generateIBAN() {
		int accountNumber = 10000000;
		Random ID = new Random();
		accountNumber += ID.nextInt(90000000);
		String IBAN = "GB24NWBK999999" + accountNumber;
		return IBAN;
	}*/

	public String getKey() {
		return key;
	}
}
