package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;

import static java.lang.Math.*;

public class OscillatorAnalyticIntegration implements Oscillator {
    private final double k;
    private final double gamma;
    private double systemTime;
    private final double dt;
    private Particle particle;
    private final double beta;


    /**
     * Set the initial conditions of the damped oscillator
     * @param mass the mass of the particle attached to the system
     * @param r the initial position of the particle
     * @param k the constant of the oscillator
     * @param gamma the damping factor
     * @param dt the time differential to which the oscillator calculates the new conditions
     */
    public OscillatorAnalyticIntegration(final double mass, final double r, final double k, final double gamma, final double dt) {
        this.k = k;
        this.gamma = gamma;
        this.dt = dt;
        beta = gamma / (2 * mass);
        this.systemTime = dt;


        // Create the particle with position, mass and initial velocity
        final double initialVx = -beta;

        this.particle = Particle.builder(r, 0)
                .mass(mass)
                .vx(initialVx)
                .build();
    }

    public void evolveSystem() {
        final double newX = calculatePosition();

        particle = particle.withX(newX);
        systemTime += dt;
    }

    private double calculatePosition() {
        final double mass = particle.mass();

        // Used for visualization simplicity only
        final double aux1 = exp(-beta * systemTime);
        final double aux2 = sqrt((k / mass) - Math.pow(beta, 2));

        return aux1 * cos(aux2 * systemTime);
    }

    public Particle getParticle() {
        return this.particle;
    }
}
