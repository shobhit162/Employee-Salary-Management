package com.acme.employeemanagement.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    /** Seeding is opt-in so a real environment can never be populated by accident. */
    private boolean enabled = false;

    private int employeeCount = 10_000;

    /**
     * Fixed so that every run of the seeder produces byte-identical data.
     * Screenshots, demos and manual checks then all describe the same org.
     */
    private long randomSeed = 20260824L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
