package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

import java.util.HashMap;
import java.util.Map;

public class BeemanIntegration implements PhysicsIntegration {
    private final PhysicsIntegration initialIntegration;
    private final Map<Particle, Vector2D> prevAccelerations; //Store particle and previous acceleration

    public BeemanIntegration(final PhysicsIntegration initialIntegration) {
        this.initialIntegration = initialIntegration;
        this.prevAccelerations = new HashMap<>();
    }


    @Override
    public Vector2D calculateVelocity(final Particle particle, final double t, final double dt) {
        final Vector2D predictedVelocity = calculatePredictedVelocity(particle, dt);
        final Vector2D nextAcceleration = null; //TODO: Calculate next acceleration
        final Vector2D correctedVelocity = calculateCorrectedVelocity(particle, nextAcceleration, dt);

        return correctedVelocity;
    }

    private Vector2D calculatePredictedVelocity(final Particle particle, final double dt) {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY())
                .times(dt).times(3/2).div(particle.mass());
        final Vector2D prevAccelerationFactor = prevAccelerations.get(particle)
                .times(dt)
                .div(2);

        accelerationFactor.sub(prevAccelerationFactor);

        return currentVelocity.add(accelerationFactor);
    }


    private Vector2D calculateCorrectedVelocity(final Particle particle, final Vector2D nextAcceleration, final double dt) {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY())
                .times(dt).times(2/3).div(particle.mass());
        final Vector2D prevAccelerationFactor = prevAccelerations.get(particle).times(dt).times(1/12);

        nextAcceleration.times(dt).times(5/12);
        return currentVelocity.add(nextAcceleration).add(accelerationFactor).sub(prevAccelerationFactor);
    }

    @Override
    public Vector2D calculatePosition(final Particle particle, final double t, final double dt) {
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
        final Vector2D velocityFactor = new Vector2D(particle.vx(), particle.vy());
        final Vector2D accelerationFactor = new Vector2D(particle.forceX(), particle.forceY());
        final Vector2D prevAccelerationFactor = prevAccelerations.get(particle)
                .times(Math.pow(dt,2))
                .div(6);

        accelerationFactor
                .times(Math.pow(dt, 2) * 2/3)
                .div(particle.mass())
                .sub(prevAccelerationFactor);
        velocityFactor.times(dt);

        return currentPosition
                .add(velocityFactor)
                .add(accelerationFactor);
    }

}
