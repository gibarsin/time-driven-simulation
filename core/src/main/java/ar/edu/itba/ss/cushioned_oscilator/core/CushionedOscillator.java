package ar.edu.itba.ss.cushioned_oscilator.core;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

class CushionedOscillator {
  private final double k;
  private final double gamma;
  private double systemTime;
  private final double dt;
  private Particle particle;

  // The class which will calculate the new conditions
  private final PhysicsIntegration physicsIntegration;

  //TODO: Receive the class that will calculate the new conditions
  /**
   * Set the initial conditions of the damped oscillator
   * @param mass the mass of the particle attached to the system
   * @param r the initial position of the particle
   * @param k the constant of the oscillator
   * @param gamma the damping factor
   * @param dt the time differential to which the oscillator calculates the new conditions
   */
  CushionedOscillator(final double mass, final double r, final double k, final double gamma, final double dt,
                      final PhysicsIntegration physicsIntegration) {
    this.k = k;
    this.gamma = gamma;
    this.dt = dt;
    this.physicsIntegration = physicsIntegration;
    double beta = gamma / (2 * mass);
    this.systemTime = 0;


    // Create the particle with position, mass and initial velocity
    final double initialVx = -beta;

    this.particle = Particle.builder(r, 0)
            .mass(mass)
            .vx(initialVx)
            .build();
  }

  void evolveSystem() {
    //TODO: Delete immutability for Particles

    particle = particle.withForceX(getParticleForceX()); //TODO: Changing

    // It's IMPORTANT to maintain the order in which calculatePosition and calculateVelocity are called, since velocity depends on position.
    final Vector2D newPosition = physicsIntegration.calculatePosition(particle, systemTime, dt);

    particle = particle
            .withX(newPosition.x())
            .withY(newPosition.y());

    final Vector2D newVelocity = physicsIntegration.calculateVelocity(particle, systemTime, dt);

    particle = particle
            .withVx(newVelocity.x())
            .withVy(newVelocity.y());

    // TODO: update whole velocity and position with a vector
//    particle = particle
//            .withX(newPosition.x())
//            .withY(newPosition.y())
//            .withVx(newVelocity.x())
//            .withVy(newVelocity.y());

    systemTime += dt;
  }

  private double getParticleForceX() {
    return -k * particle.x() - gamma * particle.vx();
  }

  Particle getParticle(){
    return this.particle;
  }


}