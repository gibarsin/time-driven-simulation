package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

import java.util.HashMap;
import java.util.Map;

public class BeemanIntegration implements PhysicsIntegration{
    PhysicsIntegration initialIntegration;
    Map<Particle, ParticleData> accMap; // Map storing for each particle it's last two accelerations

    public BeemanIntegration(PhysicsIntegration initialIntegration){
        this.initialIntegration = initialIntegration;
        this.accMap = new HashMap<>();
    }


    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        return null;
    }

    @Override
    public Vector2D calculatePosition(Particle particle, double t, double dt) {
        return null;
    }

    private static class ParticleData{
        Vector2D prevAcceleration;
        Vector2D prevPrev;

        public ParticleData(Vector2D prevAcceleration){
            this.prevAcceleration = prevAcceleration;
        }

        public Vector2D getPrevAcc() {
            return prevAcceleration;
        }

        public Vector2D getPrevPrev() {
            return prevPrev;
        }

        public ParticleData updatePrevPositions(Vector2D prevPosition, Vector2D prevPrev) {
            this.prevAcceleration = prevPosition;
            this.prevPrev = prevPrev;
            return this;
        }
    }

}
