package newbank.server;

public class CustomerID {
	private String key;
	private String userName;
	private String password;
	
	public CustomerID(String key, String userName, String password) {
		this.key = key; this.userName = userName; this.password = password;
	}

	
	public String getKey() {
		return key;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}
}
