package newbank.server;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MicroLoan {
    /* --- Attributes --- */
    private String description;
    private String status;
    private double totalAmount;
    private double paidAmount;
    private double annualInterestRate;
    private double outstandingAmount;
    private String borrowerID;
    private String borrowerIBAN;
    private String lenderID;
    private String lenderIBAN;
    private int loanID;

    /* --- Methods --- */
    // Create a new MicroLoan object
    public MicroLoan(String borrowerID, String description,
                     double totalAmount, double annualInterestRate, int loanID, String borrowerIBAN) {
        this.borrowerID = borrowerID;
        this.borrowerIBAN = borrowerIBAN;
        this.description = description;
        this.totalAmount = totalAmount;
        this.annualInterestRate = annualInterestRate;
        this.paidAmount = 0.0;
        this.outstandingAmount = this.totalAmount - this.paidAmount;
        this.lenderID = null;
        this.status = "Draft";
        this.loanID = loanID;
    }

    // Return displayable strings


    /* --- Getters and setters --- */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double requestedAmount) { this.totalAmount = requestedAmount; }
    public double getPaidAmount() { return paidAmount; }
    public void updatePaidAmount(double paidAmount) {
        this.paidAmount += paidAmount;
        round(this.paidAmount, 1);
    }
    public double getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(double interestRate) { this.annualInterestRate = interestRate; }
    public String getBorrowerID() { return borrowerID; }
    public void  setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }
    public String getBorrowerIBAN() { return borrowerIBAN; }
    public void setBorrowerIBAN(String borrowerIBAN) { this.borrowerIBAN = borrowerIBAN; }
    public String getLenderID() { return lenderID; }
    public void setLenderID(String lenderID) { this.lenderID = lenderID; }
    public String getLenderIBAN() { return lenderIBAN; }
    public void setLenderIBAN(String lenderIBAN) { this.lenderIBAN = lenderIBAN; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getLoanID() { return loanID;}
    public double getOutstandingAmount() { return outstandingAmount; }
    public void updateOutstandingAmount() {
        this.outstandingAmount = this.totalAmount - this.paidAmount;
        round(this.outstandingAmount, 1);
    }

    // Round doubles method
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

