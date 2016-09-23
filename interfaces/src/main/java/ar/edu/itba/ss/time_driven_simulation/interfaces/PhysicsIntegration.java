package ar.edu.itba.ss.time_driven_simulation.interfaces;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;

public interface PhysicsIntegration {
    Vector2D calculatePosition(Particle particle, double t, double dt);

    Vector2D calculateVelocity(Particle particle, double t, double dt);
}
