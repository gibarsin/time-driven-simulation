package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

import java.util.HashMap;
import java.util.Map;

public class VerletIntegration implements PhysicsIntegration {
    PhysicsIntegration initialIntegration;
    Map<Particle, ParticleData> posMap; // Map storing for each particle it's last two positions: Position 0 is previous and 1 is previous's previous.

    public VerletIntegration(PhysicsIntegration initialIntegration){
        this.initialIntegration = initialIntegration;
        this.posMap = new HashMap<>();
    }

    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        //TODO: Make particle return a Vector2D of its position
        final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); // current: r(t+dt)
        final Vector2D prevPosition = posMap.get(particle).getPrevPrev(); // prevPrev: r(t-dt)

        // TODO: according to the formula, this is v(t) and not v(t+dt)?
        return currentPosition.sub(prevPosition).div(2*dt);

    }

    @Override
    public Vector2D calculatePosition(Particle particle, double t, double dt) {

        if(t == 0){
            Vector2D prev = initialIntegration.calculatePosition(particle, t, -dt);
            posMap.put(particle, new ParticleData(prev)); // prev: r(t-dt)
        }

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
    private static class ParticleData{
        Vector2D prevPosition;
        Vector2D prevPrev;

        public ParticleData(Vector2D prevPosition){
            this.prevPosition = prevPosition;
        }
        public ParticleData(Vector2D prevPosition, Vector2D prevPrev){
            this.prevPosition = prevPosition;
            this.prevPrev = prevPrev;
        }

        public Vector2D getPrevPosition() {
            return prevPosition;
        }

        public Vector2D getPrevPrev() {
            return prevPrev;
        }

        public ParticleData updatePrevPositions(Vector2D prevPosition, Vector2D prevPrev) {
            this.prevPosition = prevPosition;
            this.prevPrev = prevPrev;
            return this;
        }
    }
}
