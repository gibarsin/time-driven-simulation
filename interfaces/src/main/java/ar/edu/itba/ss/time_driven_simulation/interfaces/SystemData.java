package ar.edu.itba.ss.time_driven_simulation.interfaces;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import java.util.Collection;

public interface SystemData {
  /**
   *
   * @return particles contained by this system's data entity
   */
  Collection<Particle> particles();
}
