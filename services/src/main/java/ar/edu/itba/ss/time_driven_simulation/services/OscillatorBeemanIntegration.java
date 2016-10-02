package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;

public class OscillatorBeemanIntegration implements Oscillator {
    private final Vector2D prevAcceleration; //Store particle and previous acceleration

    private final double k;
    private final double gamma;
    private final double dt;
    private Particle particle;

    public OscillatorBeemanIntegration(final double mass, final double r, final double k, final double gamma, final double dt) {
        this.k = k;
        this.gamma = gamma;
        this.dt = dt;
        double beta = gamma / (2 * mass);

        // Create the particle with position, mass and initial velocity
        final double initialVx = -beta;

        this.particle = Particle.builder(r, 0)
                .mass(mass)
                .vx(initialVx)
                .build();
        this.prevAcceleration = getPrevForce(particle).div(particle.mass());
    }

    private Vector2D getPrevForce(final Particle particle) {
        final double fx = getPreviousForceComponent(particle.mass(), particle.x(), particle.vx());
        final double fy = getPreviousForceComponent(particle.mass(), particle.y(), particle.vy());
        return new Vector2D(fx, fy);
    }

    private double getPreviousForceComponent(final double m, final double r, final double v) {
        final double theta = k * dt - gamma;
        final double sigma = k * Math.pow(dt, 2) / (2*m);
        return (-k * r + theta * v)/(1 - sigma + theta * dt/m);
    }

    public void evolveSystem() {
        particle = particle.withForceX(getForceX());
        final Vector2D newPosition = calculatePosition();
        final Vector2D newVelocity = calculateVelocity(newPosition);

        particle = particle
                .withX(newPosition.x())
                .withY(newPosition.y())
                .withVx(newVelocity.x())
                .withVy(newVelocity.y());

        prevAcceleration.setX(particle.forceX() / particle.mass());
    }

    private Vector2D calculateVelocity(final Vector2D newPosition) {
//        final Vector2D predictedVelocity = calculatePredictedVelocity();
//        final Vector2D nextAcceleration = getForceX(newPosition.x(), predictedVelocity.x())
//                .div(particle.mass());

//        return calculateCorrectedVelocity(nextAcceleration);
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D nextAccelerationFactor = getForceX(newPosition.x(), particle.vx()).div(particle.mass());
        final Vector2D currAccelerationFactor = new Vector2D(particle.forceX(), particle.forceY()).div(particle.mass());
        final Vector2D prevAccelerationFactor = new Vector2D(prevAcceleration);

        nextAccelerationFactor.div(3).times(dt);
        currAccelerationFactor.div(6).times(5).times(dt);
        prevAccelerationFactor.div(6).times(dt);

        return currentVelocity
                .add(nextAccelerationFactor)
                .add(currAccelerationFactor)
                .sub(prevAccelerationFactor);
    }

    private double getForceX() {
        return -k * particle.x() - gamma * particle.vx();
    }

    private Vector2D getForceX(final double x, final double vx) {
        return new Vector2D(-k * x - gamma * vx, 0);
    }

    private Vector2D calculatePredictedVelocity() {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY())
                .times(dt)
                .times(3/2)
                .div(particle.mass());
        final Vector2D prevAccelerationFactor = new Vector2D(prevAcceleration)
                .times(dt)
                .div(2);

        return currentVelocity.add(accelerationFactor).sub(prevAccelerationFactor);
    }


    private Vector2D calculateCorrectedVelocity(final Vector2D nextAcceleration) {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY())
                .times(dt)
                .times(2/3)
                .div(particle.mass());

        final Vector2D prevAccelerationFactor = new Vector2D(prevAcceleration)
                .times(dt)
                .times(1/12);

        nextAcceleration
                .times(dt)
                .times(5/12);
        return currentVelocity
                .add(nextAcceleration)
                .add(accelerationFactor)
                .sub(prevAccelerationFactor);
    }

    private Vector2D calculatePosition() {
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
        final Vector2D velocityFactor = new Vector2D(particle.vx(), particle.vy())
                .times(dt);
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY())
                .times(Math.pow(dt, 2))
                .times(2/3)
                .div(particle.mass());
        final Vector2D prevAccelerationFactor = new Vector2D(prevAcceleration)
                .times(Math.pow(dt,2))
                .div(6);

        return currentPosition
                .add(velocityFactor)
                .add(accelerationFactor)
                .sub(prevAccelerationFactor);
    }

    public Particle getParticle() {
        return this.particle;
    }

}
