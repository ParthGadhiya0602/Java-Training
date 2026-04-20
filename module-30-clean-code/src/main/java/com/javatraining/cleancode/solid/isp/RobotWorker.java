package com.javatraining.cleancode.solid.isp;

/** Implements only the interfaces that make sense for a robot — no forced eat() stub. */
public class RobotWorker implements Workable, Rechargeable {

    private final String model;
    private int batteryLevel;

    public RobotWorker(String model, int initialBattery) {
        this.model = model;
        this.batteryLevel = initialBattery;
    }

    @Override public String work() { return model + " is working autonomously"; }

    @Override public String charge(int percent) {
        batteryLevel = Math.min(100, batteryLevel + percent);
        return model + " charged to " + batteryLevel + "%";
    }

    public int batteryLevel() { return batteryLevel; }
}
