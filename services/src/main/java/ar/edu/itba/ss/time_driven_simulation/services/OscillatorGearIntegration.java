package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;

import static java.lang.Math.pow;

public class OscillatorGearIntegration implements Oscillator {
  // Variables related to the order of Gear Predictor Corrector
  // Change this parameter to change the order of the algorithm
  // You'll also have to change the order of the alpha values
  private static final int ORDER = 5;
  private static final int[] factorial = new int[ORDER + 1];
  private static final double[] alpha = new double[ORDER + 1];
  private final double[] r;
  private final double[] rPredicted;

  // Oscillator constants
  private final double k;
  private final double gamma;
  private final double dt;

  // Oscillator's particle
  private Particle particle; //TODO: Integrate particle more to the system (currently I use r[0] for position and r[1] for speed

  static {
    for(int i = 0; i < ORDER + 1; i++) {
      factorial[i] = MathUtils.factorial(i);
    }

    //Alpha values taken from theory powerpoint
    alpha[0] = 3.0/16.0;
    alpha[1] = 251.0/360.0;
    alpha[2] = 1.0;
    alpha[3] = 11.0/18.0;
    alpha[4] = 1.0/6.0;
    alpha[5] = 1.0/60.0;
  }

  public OscillatorGearIntegration(final double mass, final double r, final double k, final double gamma, final double dt) {
    // Save final parameters
    this.k = k;
    this.gamma = gamma;
    this.dt = dt;

    // Calculate particle's initial values
    final double beta = gamma / (2 * mass);
    final double initialVx = -beta;
    this.particle = Particle.builder(r, 0)
            .mass(mass)
            .vx(initialVx)
            .build();

    // Initialize r[] values after creating the particle
    this.r = new double[ORDER + 1];
    rInitialize();

    // Initialize rPredicted[] that will be used in the evolution of the system
    this.rPredicted = new double[ORDER + 1];
  }

  private void rInitialize() {
    r[0] = particle.x();
    r[1] = particle.vx();

    for(int i = 2; i < ORDER + 1; i++) {
      r[i] = (-k * r[i - 2] - gamma * r[i - 1]) / particle.mass();
    }
  }

  //Methods down here show the evolution of the system
  public void evolveSystem() {
    final double r2Delta;

    rPredict();
    r2Delta = evaluate();
    rCorrect(r2Delta);
  }

  private void rPredict() {
    for(int i = ORDER; i >= 0; i--) {
      rPredicted[i] = 0.0;
      for(int j = i, k = 0; j < ORDER + 1; j++, k++) {
        rPredicted[i] += (r[j] * pow(dt, k) / factorial[k]);
      }
    }
  }

  /**
   * Evaluate delta Acceleration to calculate a delta R so that the position is corrected in the rCorrect step
   * @return r2Delta
   */
  private double evaluate() {
    final double accelerationDelta = getAcceleration() - rPredicted[2];

    return accelerationDelta * pow(dt, 2) / factorial[2];
  }

  private void rCorrect(final double r2Delta) {
    for(int i = 0; i < ORDER + 1; i++) {
      r[i] = rPredicted[i] + alpha[i] * r2Delta * factorial[i] / pow(dt, i);
    }
  }

  private double getAcceleration() {
    return (-k * rPredicted[0] - gamma * rPredicted[1]) / particle.mass();
  }

  public Particle getParticle() {
    particle = particle.withX(r[0]).withVx(r[1]);

    return particle;
  }
}
