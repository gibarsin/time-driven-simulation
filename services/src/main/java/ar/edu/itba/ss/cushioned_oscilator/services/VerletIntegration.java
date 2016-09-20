package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

public class VerletIntegration implements PhysicsIntegration {
    PhysicsIntegration initialIntegration;
    boolean firstIteration;

    public VerletIntegration(PhysicsIntegration initialIntegration){
        this.initialIntegration = initialIntegration;
        this.firstIteration = true;
    }

    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        final Vector2D prevPosition = new Vector2D(particle.prevX(), particle.prevY());

        return calculatePosition(particle, t, dt).sub(prevPosition).div(2*dt);
    }

    @Override
    public Vector2D calculatePosition(Particle particle, double t, double dt) {
        Particle p = particle;

        if(firstIteration){
            Vector2D prevPosition = initialIntegration.calculatePosition(particle, t, -dt);
            p = particle.withPrevX(prevPosition.x()).withPrevY(prevPosition.y());
            firstIteration = false;
        }

        final Vector2D currentPosition = new Vector2D(p.x(), p.y()); //TODO: Make particle return a Vector2D of its position
        final Vector2D prevPosition = new Vector2D(p.prevX(), p.prevY());
        final Vector2D forceFactor = new Vector2D(p.forceX(), p.forceY());

        currentPosition.times(2);
        forceFactor.times( (dt * dt) / (2 * p.mass()) );

        return currentPosition.sub(prevPosition).add(forceFactor);
    }
}
