package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.ParticleType;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.Math.*;

public class SolarSystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolarSystem.class);
  private double dt;
  private static final double KM_TO_M = 1000.0;
  private static final double G = 6.693E-11;
  private static final double SHIP_ORBITAL_V0 = 7.12 * KM_TO_M;
  private static final double SHIP_RADIUS = 1e2;
  private static final double SHIP_DISTANCE_TO_EARTH = 1500 * KM_TO_M;
  private static final int HOURS_PER_DAY = 24;
  private static final int SECONDS_PER_HOUR = 3600;
  private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
  private final Map<ParticleType, Particle> particles;
  private final Map<Particle, ParticleData> posMap; // Map storing for each particle it's last two positions
  private double totalSimulatedTime;

  private Particle[] solarSystem = new Particle[0];
  private ParticleType shipLandedTo = ParticleType.COMMON;
  private SolarSystemState minDistanceToMarsSSState;

  public SolarSystem(final double dt){
    this.dt = dt;
    this.totalSimulatedTime = 0;

    final Particle sun = Particle.builder(0, 0)
            .mass(1.988E30)
            .vx(0)
            .vy(0)
            .radio(695700 * KM_TO_M)
            .type(ParticleType.SUN)
            .build();

    final Particle earth = Particle.builder(1.391734353396533E8 * KM_TO_M,
            -0.571059040560652E8 * KM_TO_M)
            .mass(5.972E24)
            .vx(10.801963811159256 * KM_TO_M)
            .vy(27.565215006898345 * KM_TO_M)
            .radio(6371 * KM_TO_M)
            .type(ParticleType.EARTH)
            .build();

    final Particle mars = Particle.builder(0.831483493435295E8 * KM_TO_M, -1.914579540822006E8 * KM_TO_M)
            .mass(6.4185E23)
            .vx(23.637912321314047 * KM_TO_M)
            .vy(11.429021426712032 * KM_TO_M)
            .radio(3389.9 * KM_TO_M)
            .type(ParticleType.MARS)
            .build();

    this.posMap = new HashMap<>();
    this.particles = new HashMap<>(4);

//    this.solarSystem = new Particle[]{sun, earth, mars};

    final Particle[] blueBodies = new Particle[]{sun, earth, mars};

    addParticlesToSolarSystem(blueBodies);

    minDistanceToMarsSSState = new SolarSystemState(particles.values(), Double.MAX_VALUE, totalSimulatedTime);

    // NOTE: After this cycle do not use sun, earth, etc. local variables since they have old
    // content (Because they are inmutables). Use solarSystem[] instead.
  }

  private void addParticlesToSolarSystem(final Particle... particles) {
    final int prevLength = solarSystem.length;
    Particle[] updatedSystem = Arrays.copyOf(solarSystem, solarSystem.length + particles.length);

    // Calculate the initial forces over each new particle of the system
    Vector2D[] forces = new Vector2D[particles.length];
    for (int i = prevLength, j = 0 ; i < updatedSystem.length ; i++, j++) {
      updatedSystem[i] = particles[j]; // add particle to solar system
      forces[j] = totalForce(particles[j]); // calculate it's initial force
    }

    solarSystem = updatedSystem; // update the Solar System

    // Assign f(0) to all particles and save r(-dt) in positionMap (Same as in Verlet's Algorithm)
    for (int i = prevLength, j = 0 ; i < updatedSystem.length ; i++, j++) {
      solarSystem[i] = solarSystem[i].withForceX(forces[j].x()).withForceY(forces[j].y());
      Vector2D prev = initialPosition(solarSystem[i]);
      this.posMap.put(solarSystem[i], new ParticleData(prev)); // prev: r(t-dt)
      this.particles.put(solarSystem[i].type(), solarSystem[i]);
    }
  }

  public SolarSystemState getMinDistanceToMarsSSState() {
    return minDistanceToMarsSSState;
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
      p1 = p1.withAgeInDays(p1.ageInDays()+dt/SECONDS_PER_DAY);
      updatedSystem[i] = p1;
      particles.put(p1.type(), p1);
    }
    solarSystem = updatedSystem;
    totalSimulatedTime += dt;
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

    return currentPosition.sub(velocityFactor.times(dt));
  }

  public boolean shipCrashed() {
    final Particle ship = particles.get(ParticleType.SHIP);
    if (ship == null) { // there is no ship or it hasn't taken off yet
      return false;
    }

    double distance;
    for (Particle particle : particles.values()) {
      if (ship.equals(particle)) {
        continue;
      }

      distance = distanceBetween(ship, particle);
      if (particle.type() == ParticleType.MARS && minDistanceToMarsSSState.distanceToMars > distance) {
        minDistanceToMarsSSState = new SolarSystemState(new HashSet<>(particles.values()),
                distance, totalSimulatedTime);
      }
      if (distance <= 0) {
        shipLandedTo = particle.type();
        return true;
      }
    }
    return false;
  }

  private double distanceBetween(final Particle p1, final Particle p2) {
    return sqrt(pow(p2.x() - p1.x(), 2) + pow(p2.y() - p1.y(), 2)) - p1.radio() - p2.radio();
  }

  public String shipLandedTo() {
    return shipLandedTo.toString();
  }

  public void takeOff(final double shipTakeOffV0, final Vector2D shipTakeOffAngle) {
    final Particle earth = particles.get(ParticleType.EARTH);
    final Particle sun = particles.get(ParticleType.SUN);

    // Determine ship's initial conditions
    double sunEarthDistance = Math.sqrt(Math.pow(earth.x()-sun.x(), 2) + Math.pow(earth.y()-sun.y(), 2));
    Vector2D normalVersor = new Vector2D(earth.x()-sun.x(), earth.y()-sun.y());
    normalVersor.div(sunEarthDistance); // Normalize Vector
    // if there is a user defined vector => use that. If not, use default, that is, tangential vector earth-sun
    Vector2D tgVersor = shipTakeOffAngle == null ?
            new Vector2D(- normalVersor.y(), normalVersor.x()) :
            shipTakeOffAngle.div(shipTakeOffAngle.norm2()); // vector to versor

    final double shipV0 = SHIP_ORBITAL_V0 + shipTakeOffV0;
    final Particle ship =
            Particle.builder( earth.x() + (earth.radio() + SHIP_DISTANCE_TO_EARTH) * normalVersor.x(),
                    earth.y() + (earth.radio() + SHIP_DISTANCE_TO_EARTH) * normalVersor.y() )
                    .mass(2E5)
                    .vx(earth.vx() + shipV0 * tgVersor.x())
                    .vy(earth.vy() + shipV0 * tgVersor.y())
                    .radio(SHIP_RADIUS) // Random radio
                    .type(ParticleType.SHIP)
                    .build();

    addParticlesToSolarSystem(ship);
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

  public static class SolarSystemState {
    private final Collection<Particle> particles;
    private final double distanceToMars; // Initially, the max possible value;
    private final double simulationTime;

    private SolarSystemState(final Collection<Particle> particles,
                             final double distanceToMars,
                             final double simulationTime) {
      this.particles = particles;
      this.distanceToMars = distanceToMars;
      this.simulationTime = simulationTime;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Min distance to Mars: ").append(distanceToMars).append(System.lineSeparator());
      sb.append("Simulation Time to min distance: ").append(simulationTime).append(System.lineSeparator());
      particles.forEach(particle -> sb.append(particle.toString()).append(System.lineSeparator()));
      return sb.toString();
    }
  }
}
