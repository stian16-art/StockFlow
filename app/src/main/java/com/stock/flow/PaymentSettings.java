package com.stock.flow;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Payment settings - itinatakda ng owner sa Profile, ginagamit
 * ng POS para sa QR generation at card reference display.
 * Naka-save sa "default_inventory/<uid>/paymentSettings".
 */
@IgnoreExtraProperties
public class PaymentSettings {

    private String gcashNumber;
    private String gcashName;
    private String mayaNumber;
    private String mayaName;
    private String maribankNumber;
    private String maribankName;
    private String cardDetails;

    public PaymentSettings() {
        // Kailangan ng Firebase para sa deserialization
    }

    public String getGcashNumber() {
        return gcashNumber;
    }

    public void setGcashNumber(String gcashNumber) {
        this.gcashNumber = gcashNumber;
    }

    public String getGcashName() {
        return gcashName;
    }

    public void setGcashName(String gcashName) {
        this.gcashName = gcashName;
    }

    public String getMayaNumber() {
        return mayaNumber;
    }

    public void setMayaNumber(String mayaNumber) {
        this.mayaNumber = mayaNumber;
    }

    public String getMayaName() {
        return mayaName;
    }

    public void setMayaName(String mayaName) {
        this.mayaName = mayaName;
    }

    public String getMaribankNumber() {
        return maribankNumber;
    }

    public void setMaribankNumber(String maribankNumber) {
        this.maribankNumber = maribankNumber;
    }

    public String getMaribankName() {
        return maribankName;
    }

    public void setMaribankName(String maribankName) {
        this.maribankName = maribankName;
    }

    public String getCardDetails() {
        return cardDetails;
    }

    public void setCardDetails(String cardDetails) {
        this.cardDetails = cardDetails;
    }
}
