package com.autohub.enums;
public enum SubscriptionPlan {

    BASIC(999.0, 10, 1),
    STANDARD(2999.0, 20, 1),
    PREMIUM(4999.0, 100, 1);

    private final Double amount;
    private final Integer vehicleLimit;
    private final Integer validityMonths;

    SubscriptionPlan(Double amount, Integer vehicleLimit, Integer validityMonths) {
        this.amount = amount;
        this.vehicleLimit = vehicleLimit;
        this.validityMonths = validityMonths;
    }

    public Double getAmount() {
        return amount;
    }

    public Integer getVehicleLimit() {
        return vehicleLimit;
    }

    public Integer getValidityMonths() {
        return validityMonths;
    }
}