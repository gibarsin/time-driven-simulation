package ar.edu.itba.ss.cushioned_oscilator.services;

import ar.edu.itba.ss.cushioned_oscilator.interfaces.PhysicsIntegration;
import ar.edu.itba.ss.cushioned_oscilator.models.Particle;
import ar.edu.itba.ss.cushioned_oscilator.models.Vector2D;

public class AnalyticIntegration implements PhysicsIntegration {

    private final double k;
    private final double gamma;
    private final double beta;


    public AnalyticIntegration(double mass, final double k, final double gamma){
        this.k = k;
        this.gamma = gamma;
        this.beta = gamma / (2 * mass);
    }

    @Override
    public Vector2D calculateVelocity(Particle particle, double t, double dt) {
        return new Vector2D(0,0);
    }

    @Override
    public Vector2D calculatePosition(Particle particle, double t, double dt) {
        double time = t + dt;
        double m = particle.mass();

        // Used for visualization simplicity only
        double aux1 = Math.exp(- ( gamma/(2*m) ) * time);
        double aux2 = Math.sqrt( (k/m) - ((gamma * gamma)/(4*m*m)));

        double rx = aux1 * Math.cos(aux2 * time);
        // double rx = Math.exp(- (gamma/2*m) * time) * Math.cos( ((k/m) - Math.sqrt((gamma * gamma)/4*m*m))  *   t);

        return new Vector2D(rx, 0);
    }
}
