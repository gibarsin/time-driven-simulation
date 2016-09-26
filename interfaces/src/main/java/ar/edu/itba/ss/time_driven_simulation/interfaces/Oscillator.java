package ar.edu.itba.ss.time_driven_simulation.interfaces;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;

public interface Oscillator {
  Particle getParticle();
  void evolveSystem();
}
