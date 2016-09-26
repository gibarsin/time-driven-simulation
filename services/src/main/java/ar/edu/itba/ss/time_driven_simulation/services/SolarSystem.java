package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.ParticleType;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class SolarSystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolarSystem.class);
  private double dt;
  private static final double G = 6.693E-11;
  private static final double SHIP_V0 = 2000d + 7120;
  private static final double SHIP_RADIUS = 1e2;
  private final Map<ParticleType, Particle> particles;
  private final Map<Particle, ParticleData> posMap; // Map storing for each particle it's last two positions

  private Particle[] solarSystem;
  private ParticleType shipLandedTo = ParticleType.COMMON;
  private Vector2D normalVersor;

  public SolarSystem(final double dt){
    this.dt = dt;

    final Particle sun = Particle.builder(0, 0)
            .mass(1.988E30)
            .vx(0)
            .vy(0)
            .radio(695700 * 1000)
            .type(ParticleType.SUN)
            .build();

    final Particle earth = Particle.builder(1.391734353396533E8 * 1000, -0.571059040560652E8 * 1000)
            .mass(5.972E24)
            .vx(10.801963811159256 * 1000)
            .vy(27.565215006898345 * 1000)
            .radio(6371 * 1000)
            .type(ParticleType.EARTH)
            .build();

    final Particle mars = Particle.builder(0.831483493435295E8 * 1000, -1.914579540822006E8 * 1000)
            .mass(6.4185E23)
            .vx(23.637912321314047 * 1000)
            .vy(11.429021426712032 * 1000)
            .radio(3389.9 * 1000)
            .type(ParticleType.MARS)
            .build();

    // Determine ship initial conditions
    double alpha = atan2(earth.y() - sun.y() , earth.x() - sun.x()); // Angle between the Sun and Earth

    ////////////////
    double sunEarthDistance = Math.sqrt(Math.pow(earth.x()-sun.x(), 2) + Math.pow(earth.y()-sun.y(), 2));
    Vector2D normalVersor = new Vector2D(earth.x()-sun.x(), earth.y()-sun.y());
    normalVersor.div(sunEarthDistance); // Normalize Vector
    Vector2D tgVersor = new Vector2D(- normalVersor.y(), normalVersor.x());

//    double theta = atan2(earth.vy(), earth.vx());
//    double c = cos(theta);
//    double s = sin(theta);
    //////////


    // Ship's velocity's angle must be equal to the Earth's velocity
    double distanceToEarth = 1500 * 1000;
    final Particle ship = Particle.builder(earth.x() + (earth.radio() + distanceToEarth) * normalVersor.x(),
            earth.y() + (earth.radio() + distanceToEarth) * normalVersor.y() )
            .mass(2E5)
            .vx(earth.vx() + SHIP_V0 * tgVersor.x())
            .vy(earth.vy() + SHIP_V0 * tgVersor.y())
            .radio(SHIP_RADIUS) // Random radio
            .type(ParticleType.SHIP)
            .build();

    this.solarSystem = new Particle[]{sun, earth, mars, ship};

    // Calculate the initial forces over each particle of the system
    Vector2D[] forces = new Vector2D[solarSystem.length];
    for(int i=0; i< solarSystem.length; i++){
      forces[i] = totalForce(solarSystem[i]);
    }
    posMap = new HashMap<>();
    particles = new HashMap<>(4);
    Vector2D prev;

    // Assign f(0) to all particles and save r(-dt) in positionMap (Same as in Verlet's Algorithm)
    for(int i=0; i< solarSystem.length; i++){
      solarSystem[i] = solarSystem[i].withForceX(forces[i].x()).withForceY(forces[i].y());
      prev = initialPosition(solarSystem[i]);
      posMap.put(solarSystem[i], new ParticleData(prev)); // prev: r(t-dt)
      particles.put(solarSystem[i].type(), solarSystem[i]);
    }

    // NOTE: After this cycle do not use sun, earth, etc. local variables since they have old
    // content (Because they are inmutables). Use solarSystem[] instead.
  }

  public void evolveSystem(){
    Particle p1;
    Particle[] updatedSystem = new Particle[solarSystem.length];
    Vector2D force;

    // Evolve system using verlet's algorithm
    for (int i=0; i<solarSystem.length; i++){
      p1 = solarSystem[i];
      force = totalForce(p1);
      p1 = p1.withForceX(force.x()).withForceY(force.y());
      final Vector2D newPosition = calculatePosition(p1);
      p1 = p1.withX(newPosition.x()).withY(newPosition.y());
      final Vector2D newVelocity = calculateVelocity(p1);
      p1 = p1.withVx(newVelocity.x()).withVy(newVelocity.y());
      updatedSystem[i] = p1;
      particles.put(p1.type(), p1);
    }
    solarSystem = updatedSystem;
  }

  private Vector2D totalForce(Particle p1){
    Particle p2;
    Vector2D force = new Vector2D(0,0);

    // Add the gravitational force of all other particles in the system
    for (final Particle aSolarSystem : solarSystem) {
      p2 = aSolarSystem;
      if (!p1.equals(p2)) {
        force.add(calculateForce(p1, p2));
      }

    }
    return force;
  }

  // Calculate Force caused in P1 by P2
  private Vector2D calculateForce(Particle p1, Particle p2) {
    double distancePow2 = Math.pow(p2.x()-p1.x(), 2) +  Math.pow(p2.y()-p1.y(), 2);
    double forceModule = G * p1.mass() * p2.mass() / distancePow2;
    double alpha = Math.atan2(p2.y() - p1.y() , p2.x() - p1.x());

    double distance = Math.sqrt(distancePow2);
    Vector2D normalVersor = new Vector2D(p2.x()- p1.x(), p2.y()-p1.y());
    normalVersor.div(distance); // Normalize Vector

    return new Vector2D(forceModule * normalVersor.x(), forceModule * normalVersor.y());
  }

  private Vector2D calculatePosition(Particle particle) {
    final ParticleData data = posMap.get(particle);
    Vector2D prevPosition = data.getPrevPosition();

    final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
    final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

    // Change the prevPosition to previous's previous && add the current position as prevPosition
    posMap.put(particle, data.updatePrevPositions(new Vector2D(currentPosition), prevPosition)); // prev: r(t), prevPrev: r(t-dt)

    forceFactor.times( (dt * dt) / (particle.mass()) );
    currentPosition.times(2).sub(prevPosition).add(forceFactor);

    return currentPosition;
  }

  private Vector2D calculateVelocity(Particle particle){
    final Vector2D currentPosition = new Vector2D(particle.x(), particle.y()); // current: r(t+dt)
    final Vector2D prevPosition = posMap.get(particle).getPrevPrev(); // prevPrev: r(t-dt)

    return currentPosition.sub(prevPosition).div(2*dt);
  }

  /**
   * Use Euler to calculate initial position in time = -dt
   * @return vector of initial position to time -dt
   */
  private Vector2D initialPosition(Particle particle) {
    final Vector2D currentPosition = new Vector2D(particle.x(), particle.y());
    final Vector2D velocityFactor = new Vector2D(particle.vx(), particle.vy());
    final Vector2D forceFactor = new Vector2D(particle.forceX(), particle.forceY());

//    velocityFactor.times(-dt);
//    forceFactor.times( Math.pow(-dt, 2) / (2 * particle.mass()) );

    // O(dt^3) is not taken into account
    // Notice that current time (t) is never used, so may be we can erase it

    return currentPosition.sub(velocityFactor.times(dt));
  }

  public boolean shipCrashed() {
    final Particle ship = particles.get(ParticleType.SHIP);
    for (Particle particle : particles.values()) {
      if (!ship.equals(particle) && particle.type() == ParticleType.MARS) {
        if (areColliding(ship, particle)) {
          shipLandedTo = particle.type();
          return true;
        }
      }
    }
    return false;
  }

  private boolean areColliding(final Particle pi, final Particle pj) {
    return distanceBetween(pi, pj) <= 0;
  }

  private double distanceBetween(final Particle p1, final Particle p2) {
    return sqrt(pow(p2.x() - p1.x(), 2) + pow(p2.y() - p1.y(), 2)) - p1.radio() - p2.radio();
  }

  public String shipLandedTo() {
    return shipLandedTo.toString();
  }

  private static class ParticleData {
    Vector2D prevPosition;
    Vector2D prevPrev;

    ParticleData(Vector2D prevPosition){
      this.prevPosition = prevPosition;
    }

    Vector2D getPrevPosition() {
      return prevPosition;
    }

    Vector2D getPrevPrev() {
      return prevPrev;
    }

    ParticleData updatePrevPositions(Vector2D prevPosition, Vector2D prevPrev) {
      this.prevPosition = prevPosition;
      this.prevPrev = prevPrev;
      return this;
    }
  }

  public List<Particle> getParticles(){
    return Arrays.asList(solarSystem);
  }

}
