package com.javatraining.cleancode.solid.srp;

import java.util.List;

public record Order(String id, String customerName, String customerEmail,
                    List<String> items, double total) {}
