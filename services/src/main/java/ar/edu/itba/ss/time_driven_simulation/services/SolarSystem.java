package ar.edu.itba.ss.time_driven_simulation.services;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class SolarSystem {
    private double dt;
    private double systemTime = 0;
    private final double G = 6.693E-11;
    private final Map<Particle, ParticleData> posMap; // Map storing for each particle it's last two positions

    private Particle sun;
    private Particle earth;
    private Particle mars;
    private Particle ship;

    private Particle[] solarSystem;

    public SolarSystem(final double dt){
        this.dt = dt;

        this.sun = Particle.builder(0, 0)
                .mass(1.988E30)
                .vx(0)
                .vy(0)
                .radio(695700 * 1000)
                .build();

        this.earth = Particle.builder(1.391734353396533E8 * 1000, -0.571059040560652E8 * 1000)
                .mass(5.972E24)
                .vx(10.801963811159256 * 1000)
                .vy(27.565215006898345 * 1000)
                .radio(6371 * 1000)
                .build();

        this.mars = Particle.builder(0.831483493435295E8 * 1000, -1.914579540822006E8 * 1000)
                .mass(6.4185E23)
                .vx(23.637912321314047 * 1000)
                .vy(11.429021426712032 * 1000)
                .radio(3389.9 * 1000)
                .build();

        // Determine ship initial conditions
        double alpha = atan2(earth.y() - sun.y() , earth.x() - sun.x()); // Angle between the Sun and Earth
        double distanceToEarth = 1500 * 1000;
        this.ship = Particle.builder(earth.x() + cos(alpha) * distanceToEarth, earth.y() + sin(alpha) * distanceToEarth)
                .mass(2E5)
                .vx(earth.vx() + 7120 * cos(alpha))
                .vy(earth.vy() + 7120 * sin(alpha))
                .radio(1000) // Random radio
                .build();

        this.solarSystem = new Particle[]{sun, earth, mars, ship};

        // Calculate the initial forces over each particle of the system
        Vector2D[] forces = new Vector2D[solarSystem.length];
        for(int i=0; i< solarSystem.length; i++){
            forces[i] = totalForce(solarSystem[i]);
        }
        posMap = new HashMap<>();
        Vector2D prev;

        // Assign f(0) to all particles and save r(-dt) in positionMap (Same as in Verlet's Algorithm)
        for(int i=0; i< solarSystem.length; i++){
            solarSystem[i] = solarSystem[i].withForceX(forces[i].x()).withForceY(forces[i].y());
            prev = initialPosition(solarSystem[i]);
            posMap.put(solarSystem[i], new ParticleData(prev)); // prev: r(t-dt)
        }

        // NOTE: After this cycle do not use sun, earth, etc. local variables since they have old
        // content (Because they are inmutables). Use solarSystem[] instead.


    }

    public void evolveSystem(){
        Particle p1;
        Particle[] updatedSystem = new Particle[solarSystem.length];
        Vector2D force;

        // Evovle system using verlet's algorithm
        for (int i=0; i<solarSystem.length; i++){
            p1 = solarSystem[i];
            force = totalForce(p1);
            p1 = p1.withForceX(force.x()).withForceY(force.y());
            final Vector2D newPosition = calculatePosition(p1);
            p1 = p1.withX(newPosition.x()).withY(newPosition.y());
            final Vector2D newVelocity = calculateVelocity(p1);
            p1 = p1.withVx(newVelocity.x()).withVy(newVelocity.y());
            updatedSystem[i] = p1;
        }
        solarSystem = updatedSystem;
        systemTime += dt;
    }

    public Vector2D totalForce(Particle p1){
        Particle p2;
        Vector2D force = new Vector2D(0,0);

        // Add the gravitational force of all other particles in the system
        for(int j=0; j<solarSystem.length; j++){
            p2 = solarSystem[j];
            if (!p1.equals(p2)) {
                force.add(calculateForce(p1, p2));
            }

        }
        return force;
    }

    public Vector2D calculateForce(Particle p1, Particle p2) {
        double distancePow2 = Math.pow(p2.x()-p1.x(), 2) +  Math.pow(p2.y()-p1.y(), 2);
        double forceModule = G * p1.mass() * p2.mass() / distancePow2;
        double alpha = Math.atan2(p2.y() - p1.y() , p2.x() - p1.x());

        return new Vector2D(forceModule * Math.cos(alpha), forceModule * Math.sin(alpha));
    }

    public Vector2D calculatePosition(Particle particle) {
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

    public Vector2D calculateVelocity(Particle particle){
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

        velocityFactor.times(-dt);
        forceFactor.times( Math.pow(-dt, 2) / (2 * particle.mass()) );

        // O(dt^3) is not taken into account
        // Notice that current time (t) is never used, so may be we can erase it

        return currentPosition
                .add(velocityFactor)
                .add(forceFactor);
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
        List<Particle> ans = new ArrayList<>();

        for(int i=0; i<solarSystem.length;  i++){
            ans.add(solarSystem[i]);
        }

        return ans;
    }

}
