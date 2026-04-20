package com.javatraining.cleancode.solid.isp;

/** Implements only the interfaces that make sense for a human. */
public class HumanWorker implements Workable, Feedable {

    private final String name;

    public HumanWorker(String name) { this.name = name; }

    @Override public String work()             { return name + " is working"; }
    @Override public String eat(String food)   { return name + " is eating " + food; }
}
