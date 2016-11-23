package ar.edu.itba.ss.time_driven_simulation.core.systems.oscillator;

import ar.edu.itba.ss.time_driven_simulation.interfaces.NumericIntegrationMethod;
import ar.edu.itba.ss.time_driven_simulation.interfaces.SystemData;
import ar.edu.itba.ss.time_driven_simulation.interfaces.TimeDrivenSimulationSystem;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;
import ar.edu.itba.ss.time_driven_simulation.services.gear.Gear5SystemData;
import ar.edu.itba.ss.time_driven_simulation.services.gear.GearPredictorCorrector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GearOscillatorSystem implements TimeDrivenSimulationSystem {

  private final NumericIntegrationMethod<Gear5SystemData> numericIntegrationMethod;
  private final Gear5SystemData systemData;

  public GearOscillatorSystem(final double mass, final double r, final double k, final double gamma) {

    /*
      template steps:
      - instantiate all needed particles
      - create system data container with system's particles and constants
      - initialize the numeric integration method and the corresponding System's data structure
     */

    // Calculate particle's initial values
    final double beta = gamma / (2 * mass);
    final double initialVy = -beta;
    final Particle particle = Particle.builder(0, r)
            .mass(mass)
            .vy(initialVy)
            .build();

    final Collection<Particle> particles = new HashSet<>();
    particles.add(particle);

    // Numeric Integration Method initialization
    this.systemData = new OscillatorGear5SystemData(particles, k, gamma);

    this.numericIntegrationMethod = new GearPredictorCorrector<>();
  }

  @Override
  public SystemData getSystemData() {
    return this.systemData;
  }

  @Override
  public void evolveSystem(final double dt) {
    numericIntegrationMethod.evolveSystem(this.systemData, dt);
  }

  private static class OscillatorGear5SystemData extends Gear5SystemData {
    private final double k;
    private final double gamma;

    private OscillatorGear5SystemData(final Collection<Particle> particles,
                                     final double k, final double gamma) {
      super(particles);

      // Save constant parameters
      this.k = k;
      this.gamma = gamma;

      init();
    }

    @Override
    protected Map<Integer, Vector2D> setInitialDerivativeValues(final Particle particle) {
      final Map<Integer, Vector2D> initialDerivativeValues = new HashMap<>(sVectors());
      initialDerivativeValues.put(0, new Vector2D(particle.x(), particle.y()));
      initialDerivativeValues.put(1, new Vector2D(particle.vx(), particle.vy()));

      for(int i = 2; i <= order(); i++) {
        final Vector2D rPrev2 = initialDerivativeValues.get(i-2);
        final Vector2D rPrev1 = initialDerivativeValues.get(i-1);
        final Vector2D term1 = new Vector2D(rPrev2).times(-k);
        final Vector2D term2 = new Vector2D(rPrev1).times(gamma);
        final Vector2D rCurr = (term1.sub(term2)).div(particle.mass());
        initialDerivativeValues.put(i, rCurr);
      }

      return initialDerivativeValues;
    }

    @Override
    protected Vector2D getForceWithPredicted(final Particle particle) {
      final Vector2D rPredicted0 = new Vector2D(getPredictedR(particle, 0));
      final Vector2D term1 = rPredicted0.times(-k);
      final Vector2D rPredicted1 = new Vector2D(getPredictedR(particle, 1));
      final Vector2D term2 = rPredicted1.times(gamma);
      return term1.sub(term2);
    }
  }
}
