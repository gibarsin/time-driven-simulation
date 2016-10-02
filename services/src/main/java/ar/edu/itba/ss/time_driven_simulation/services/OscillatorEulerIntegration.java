package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OscillatorEulerIntegration implements Oscillator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OscillatorEulerIntegration.class);

    private final double k;
    private final double gamma;
    private double systemTime;
    private final double dt;
    private Particle particle;

    public OscillatorEulerIntegration(final double mass, final double r, final double k, final double gamma, final double dt) {
        this.k = k;
        this.gamma = gamma;
        this.dt = dt;
        double beta = gamma / (2 * mass);
        this.systemTime = 0;

        // Create the particle with position, mass and initial velocity
        final double initialVx = -beta;

        this.particle = Particle.builder(r, 0)
                .mass(mass)
                .vx(initialVx)
                .build();
    }

    public void evolveSystem() {
        particle = particle.withForceX(getForceX());
        final Vector2D newPosition = calculatePosition();
        final Vector2D newVelocity = calculateVelocity();

        particle = particle
                .withX(newPosition.x())
                .withY(newPosition.y())
                .withVx(newVelocity.x())
                .withVy(newVelocity.y());
        systemTime += dt;
    }

    private double getForceX() {
        return -k * particle.x() - gamma * particle.vx();
    }

    private Vector2D calculatePosition() {
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
        final Vector2D velocityFactor = new Vector2D(particle.vx(), particle.vy());
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        velocityFactor.times(dt);
        forceFactor.times( Math.pow(dt, 2) / (2 * particle.mass()) );

        // O(dt^3) is not taken into account
        // Notice that current time (t) is never used, so may be we can erase it

        return currentPosition
                .add(velocityFactor)
                .add(forceFactor);
    }

    private Vector2D calculateVelocity() {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        forceFactor.times(dt / particle.mass());

        // O(dt^2) is not taken into account

        return currentVelocity
                .add(forceFactor);
    }

    public Particle getParticle() {
        return this.particle;
    }
}
