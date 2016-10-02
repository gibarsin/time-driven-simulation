package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;

import java.util.HashMap;
import java.util.Map;

public class OscillatorVerletIntegration implements Oscillator {
    private final Map<Particle, ParticleData> posMap; // Map storing for each particle it's last two positions
    private final double k;
    private final double gamma;
    private double systemTime;
    private final double dt;
    private Particle particle;

    public OscillatorVerletIntegration(final double mass, final double r, final double k, final double gamma, final double dt) {
        this.posMap = new HashMap<>();
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

        particle = particle.withForceX(getForceX()); // We use the f(0) to calculate r(-dt)

        // Initialize Map with -dt position (before simulation starts)
        final Vector2D prev = initialPosition();
        posMap.put(particle, new ParticleData(prev)); // prev: r(t-dt)
    }

    public void evolveSystem() {
        particle = particle.withForceX(getForceX());

        final Vector2D newPosition = calculatePosition();
        particle = particle
                .withX(newPosition.x())
                .withY(newPosition.y());

        final Vector2D newVelocity = calculateVelocity();
        particle = particle
                .withVx(newVelocity.x())
                .withVy(newVelocity.y());
        systemTime += dt;
    }

    private double getForceX() {
        return -k * particle.x() - gamma * particle.vx();
    }

    public Vector2D calculateVelocity() {
        //TODO: Make particle return a Vector2D of its position
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); // current: r(t+dt)
        final Vector2D prevPosition = posMap.get(particle).getPrevPrev(); // prevPrev: r(t-dt)

        // TODO: according to the formula, this is v(t) and not v(t+dt)?
        return currentPosition.sub(prevPosition).div(2*dt);

    }

    private Vector2D calculatePosition() {
        final ParticleData data = posMap.get(particle);
        final Vector2D prevPosition = data.getPrevPosition();
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); //TODO: Make particle return a Vector2D of its position
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        // Change the prevPosition to previous's previous && add the current position as prevPosition
        posMap.put(particle, data.updatePrevPositions(new Vector2D(currentPosition), prevPosition)); // prev: r(t), prevPrev: r(t-dt)

        forceFactor.times( (dt * dt) / (particle.mass()) );
        currentPosition.times(2).sub(prevPosition).add(forceFactor);

        return currentPosition;
    }

    /**
     * User Euler to calculate initial position in time = -dt
     * @return vector of initial position to time -dt
     */
    private Vector2D initialPosition() {
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
        final Vector2D velocityFactor = new Vector2D(particle.vx(), particle.vy());
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        velocityFactor.times(-dt);
        forceFactor.times( Math.pow(-dt, 2) / (2 * particle.mass()) );

        // O(dt^3) is not taken into account
        // Notice that current time (t) is never used, so may be we can erase it

        return currentPosition
                .add(velocityFactor)
                .add(forceFactor);
    }

    private static class ParticleData {
        Vector2D prevPosition;
        Vector2D prevPrev;

        ParticleData(Vector2D prevPosition){
            this.prevPosition = prevPosition;
        }

        Vector2D getPrevPosition() {
            return prevPosition;
        }

        Vector2D getPrevPrev() {
            return prevPrev;
        }

        ParticleData updatePrevPositions(Vector2D prevPosition, Vector2D prevPrev) {
            this.prevPosition = prevPosition;
            this.prevPrev = prevPrev;
            return this;
        }
    }

    public Particle getParticle() {
        return this.particle;
    }
}
