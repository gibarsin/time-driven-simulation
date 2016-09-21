package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

import java.util.HashMap;
import java.util.Map;

public class VerletIntegration implements PhysicsIntegration {
    PhysicsIntegration initialIntegration;
    Map<Particle, Vector2D[]> prevPos; // Map storing for each particle it's last two positions: Position 0 is previous and 1 is previous's previous.

    public VerletIntegration(PhysicsIntegration initialIntegration){
        this.initialIntegration = initialIntegration;
        this.prevPos = new HashMap<>();
    }

    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        //TODO: Make particle return a Vector2D of its position
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); // this is r(t+dt)
        final Vector2D prevPosition = prevPos.get(particle)[1]; // this is r(t-dt)

        return currentPosition.sub(prevPosition).div(2*dt); // this is v(t) and not v(t+dt)
    }

    @Override
    public Vector2D calculatePosition(Particle particle, double t, double dt) {

        if(t == 0){
            Vector2D prev = initialIntegration.calculatePosition(particle, t, -dt);
            prevPos.put(particle, new Vector2D[]{prev, null}); // The array now remains: [r(t-dt), null]
        }

        final Vector2D prevPosition = prevPos.get(particle)[0];
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); //TODO: Make particle return a Vector2D of its position
        final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

        // Change the prevPosition to previous's previous && add the current position as prevPosition
        prevPos.put(particle, new Vector2D[]{new Vector2D(currentPosition), prevPosition}); // The array now remains: [r(t), r(t-dt)]

        forceFactor.times( (dt * dt) / (particle.mass()) );
        currentPosition.times(2).sub(prevPosition).add(forceFactor);

        return currentPosition;
    }
}
