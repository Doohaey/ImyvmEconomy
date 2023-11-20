package com.imyvm.economy;

public class TaxRate {
    public TaxRate(Double taxRate, TaxType taxType) {
        this.taxRate = taxRate;
        this.taxType = taxType;
    }

    public enum TaxType{
        STOCK_TAX,
        TRAFFIC_TAX
    }
    Double taxRate;
    TaxType taxType;

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }

    public TaxType getTaxType() {
        return taxType;
    }

    public void setTaxType(TaxType taxType) {
        this.taxType = taxType;
    }
}
