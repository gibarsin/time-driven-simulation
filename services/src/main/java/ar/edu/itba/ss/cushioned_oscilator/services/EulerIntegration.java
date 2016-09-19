package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EulerIntegration implements PhysicsIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EulerIntegration.class);

    @Override
    public Vector2D calculatePosition(final Particle particle, final double t, final double dt) {
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); //TODO: Make particle return a Vector2D of its position
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

    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        final Vector2D currentVelocity = new Vector2D(particle.vx(), particle.vy());
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        forceFactor.times(dt / particle.mass());

        // O(dt^2) is not taken into account

        return currentVelocity
                .add(forceFactor);
    }
}
