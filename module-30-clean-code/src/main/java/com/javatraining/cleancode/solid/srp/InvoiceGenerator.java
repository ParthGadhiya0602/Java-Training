package com.javatraining.cleancode.solid.srp;

/**
 * SRP - single responsibility: generate invoice text.
 * Only changes when invoice format or legal requirements change.
 */
public class InvoiceGenerator {

    public String generate(Order order) {
        return "INVOICE #" + order.id()
               + " | Customer: " + order.customerName()
               + " | Items: " + order.items().size()
               + " | Total: $" + order.total();
    }
}
