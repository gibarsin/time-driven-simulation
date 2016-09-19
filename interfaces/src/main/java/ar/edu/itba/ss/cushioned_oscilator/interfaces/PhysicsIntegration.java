package ar.edu.itba.ss.cushioned_oscilator.interfaces;

import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

public interface PhysicsIntegration {
    Vector2D calculatePosition(Particle particle, double t, double dt);

    Vector2D calculateVelocity(Particle particle, double t, double dt);
}
