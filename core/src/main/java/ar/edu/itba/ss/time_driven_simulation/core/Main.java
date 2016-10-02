package ar.edu.itba.ss.time_driven_simulation.core;

import ar.edu.itba.ss.time_driven_simulation.interfaces.Oscillator;
import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.models.ParticleType;
import ar.edu.itba.ss.time_driven_simulation.models.Vector2D;
import ar.edu.itba.ss.time_driven_simulation.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static ar.edu.itba.ss.time_driven_simulation.core.Main.EXIT_CODE.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  // File Management
  private static final String DESTINATION_FOLDER = "output";
  private static final String STATIC_FILE = "static.dat";
  private static final String OUTPUT_FILE = "output.dat";
  private static final String OVITO_FILE = "graphics.xyz";
  private static final String SS_REPORT_FILE = "ss_report.dat";
  private static final String SS_MIN_DISTANCE_FILE = "ss_min_distance_";

  // Real Constants
  private static final int HOURS_PER_DAY = 24;
  private static final int SECONDS_PER_HOUR = 3600;
  private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
  private static final double KM_TO_M = 1000.0;

  // Solar System Constants
  private static final int SOLAR_SYSTEM_N = 4;
  private static final double SOLAR_SYSTEM_L = 1e12;
  private static final double SOLAR_SYSTEM_W = 1e12;
  private static final double DAYS_TO_TAKE_OFF = 755;
  private static final double SHIP_TAKE_OFF_V0 = 15 * KM_TO_M;
  // To use default, that is, tangential angle Earth-Sun, use 'null'
//  private static final Vector2D SHIP_TAKE_OFF_ANGLE = null;
  // To use own angle, make your own vector. X and Y components will be used for vx and vy respectively
  // Vector will be converted to a versor to be used.
  private static final Vector2D SHIP_TAKE_OFF_ANGLE = new Vector2D(-1, -.6873);

  private enum OutputType {
    SOLAR_SYSTEM,
    COMMON
  }
  private static final String HELP_TEXT =
          "Cushioned Oscillator Simulation Implementation.\n" +
          "Arguments: \n" +
          "* gen staticdat <N> <m> <r> <k> <gamma> <tf> : \n" +
          "     generates an output/static.dat file with the desired parameters.\n" +
          "* osc <path/to/static.dat> <type> <dt>\n" +
          "     runs the cushioned-oscillator simulation and saves snapshots of the system in <output.dat>.\n" +
          "     <type> can be 'analytic', 'verlet', 'beeman', 'gear'.\n" +
          "* toMars <dt> <ft> <days_to_take_off> <ship_take_off_v0> (<ship_take_off_angle_x> <ship_take_off_angle_y>)\n" +
          "     Simulation of a space ship taking off from Earth with Mars as destination." +
          "     <dt> is the delta time represented with each iteration, in seconds." +
          "     <ft> is the final time that the system will be simulated.\n" +
          "     <days_to_take_off> are the days to take off since the initial conditions.\n" +
          "     <ship_take_off_v0> initial velocity's module of the ship.\n" +
          "     Optional Arguments: \n" +
          "       <ship_take_off_angle_x> initial velocity's angle of the ship in x direction.\n" +
          "       <ship_take_off_angle_y> initial velocity's angle of the ship in y direction.\n" +
          "     Only The Sun, Earth, Mars and the spaceship are represented.\n" +
          "     **Note** A 'static.dat' file is generated automatically, although not needed.\n" +
          "* toEarth <dt> <ft> <days_to_take_off> <ship_take_off_v0> (<ship_take_off_angle_x> <ship_take_off_angle_y>)\n" +
          "     Simulation of a space ship taking off from Mars with Earth as destination." +
          "     <dt> is the delta time represented with each iteration, in seconds." +
          "     <ft> is the final time that the system will be simulated.\n" +
          "     <days_to_take_off> are the days to take off since the initial conditions.\n" +
          "     <ship_take_off_v0> initial velocity's module of the ship.\n" +
          "     Optional Arguments: \n" +
          "       <ship_take_off_angle_x> initial velocity's angle of the ship in x direction.\n" +
          "       <ship_take_off_angle_y> initial velocity's angle of the ship in y direction.\n" +
          "     Only The Sun, Earth, Mars and the spaceship are represented.\n" +
          "     **Note** A 'static.dat' file is generated automatically, although not needed.\n" +
          "* gen ovito <path/to/static.dat> <path/to/output.dat>: \n"+
          "     generates an output/graphics.xyz file (for Ovito) with the result of the simulation\n " +
          "     (<output.dat>) generated with the static file.\n";


  // Exit Codes
  enum EXIT_CODE {
    NO_ARGS(-1),
    NO_FILE(-2),
    BAD_N_ARGUMENTS(-3),
    BAD_ARGUMENT(-4),
    NOT_A_FILE(-5),
    UNEXPECTED_ERROR(-6),
    BAD_FILE_FORMAT(-7);

    private final int code;

    EXIT_CODE(final int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }
  }

  public static void main(final String[] args) {
    if(args.length == 0) {
      System.out.println("[FAIL] - No arguments passed. Try 'help' for more information.");
      exit(NO_ARGS);
    }

    switch (args[0]) {
      case "help":
        System.out.println(HELP_TEXT);
        break;
      case "gen":
        generateCase(args);
        break;
      case "osc":
        cushionedOscillator(args);
        break;
      case "toMars":
        toMars(args);
        break;
      case "toEarth":
        toEarth(args);
        break;
      case "min":
        minimumDistance();
        break;

      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;
    }

    System.out.println("[DONE]");
  }

  /**
   * Change the constants below to define the ranges you want for initial Speed, days to take off, and the initial angle.
   * After running, one file will be created for each time Mars was reached (if that was the case)
   * plus one file containing the travel that reached minimum distance to Mars.
   * This files contain the necessary information to run the desired simulation again with ss method.
   * NOTE: When running a single ss from console make sure to run for (ft + daysTakeOff) seconds.
   */
  private static void minimumDistance() {

    final double dt = 100;
    final double ft = 3600 * 24 * 365; // Max travel time for the spaceship (1 year)

    // Parameters range
    final double MIN_INITIAL_SPEED = 10000;
    final double STEP_INITIAL_SPEED = 1000;
    final double MAX_INITIAL_SPEED = 11000;

    final double MIN_DAYS_TAKE_OFF = 0;
    final double STEP_DAYS_TAKE_OFF = 100;
    final double MAX_DAYS_TAKE_OFF = 366;

    final double MIN_TAKE_OFF_DEGREE = 50;
    final double STEP_TAKE_OFF_DEGREE = 30;
    final double MAX_TAKE_OFF_DEGREE = 100;

    final int speedIter =  (int) Math.ceil(((MAX_INITIAL_SPEED - MIN_INITIAL_SPEED) / STEP_INITIAL_SPEED));
    final int takeOffIter = (int) Math.ceil(((MAX_DAYS_TAKE_OFF- MIN_DAYS_TAKE_OFF) / STEP_DAYS_TAKE_OFF));
    final int  degreeIter = (int) Math.ceil(((MAX_TAKE_OFF_DEGREE - MIN_TAKE_OFF_DEGREE ) / STEP_TAKE_OFF_DEGREE));

    final int totalIter = speedIter * takeOffIter * degreeIter;


    ArrayList<ReportFile> reports = new ArrayList<>();
    ReportFile minTravel = null;

    // Generate static.dat file for this system to be used to generate ovito file in a future
    generateStaticDatFile(SOLAR_SYSTEM_N, -1, -1, -1, -1, -1, SOLAR_SYSTEM_W, SOLAR_SYSTEM_L);

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    Vector2D takeOffAngle;
    double initialSpeed;
    double daysTakeOff;
    int currIter = 0;

    for(initialSpeed=MIN_INITIAL_SPEED; initialSpeed<MAX_INITIAL_SPEED; initialSpeed+=STEP_INITIAL_SPEED){
      for(daysTakeOff=MIN_DAYS_TAKE_OFF; daysTakeOff<MAX_DAYS_TAKE_OFF; daysTakeOff+=STEP_DAYS_TAKE_OFF){
        for(double angle=MIN_TAKE_OFF_DEGREE; angle<MAX_TAKE_OFF_DEGREE; angle+=STEP_TAKE_OFF_DEGREE){

          if(!deleteIfExists(pathToDatFile)) {
            return;
          }

          final SolarSystem solarSystem = new SolarSystem(dt);

          // Run the system until the ship is ready to take off
          long i = 0;
          double timeToTookOff = daysToSeconds(daysTakeOff);

          double currentTime = 0;
          while (currentTime < timeToTookOff) {
            evolve(solarSystem, i++);
            currentTime +=dt;
          }
          // takeOffAngle = null;
          takeOffAngle = calculateTakeOffAngle(angle, solarSystem.getEarthPosition(), solarSystem.getSunPosition());

          solarSystem.takeOff(initialSpeed, takeOffAngle);

          // Once the ship takes off, run until limit time is reached or ship crashes
          for (double systemTime = 0; systemTime < ft; systemTime += dt) {
            evolve(solarSystem, i);
            if (solarSystem.shipCrashed()) {
              System.out.println("[REACHED] - Ship landed on " + solarSystem.shipLandedTo());
              generateOutputDatFile(OutputType.SOLAR_SYSTEM, solarSystem.getParticles(), i);
              break;
            }
            i++;
          }

          // In case we find a new minimum or mars is reached, the system state is saved
          if(solarSystem.shipLandedTo().equals(ParticleType.MARS.toString())){
            ReportFile report = new ReportFile(dt, ft, takeOffAngle, initialSpeed, daysTakeOff, angle, solarSystem.getMinDistanceToMarsSSState());
            reports.add(report);
          }
          else if(minTravel == null ||
                  solarSystem.getMinDistanceToMarsSSState().getDistanceToMars() < minTravel.getDistanceToMars()){
            ReportFile report = new ReportFile(dt, ft, takeOffAngle, initialSpeed, daysTakeOff, angle, solarSystem.getMinDistanceToMarsSSState());
            minTravel = report;
          }
          currIter++;
          System.out.println("Progress: " + currIter + " / " + totalIter);

        }
      }
    }

    if (minTravel != null) {
      reports.add(minTravel);
      generateReportFiles(reports);
    }

  }

  /**
   *
   * @param angle the initial angle for the ship take off, measured as 0 when tangential to earth and
   * positive outwards
   * @return a vector representing the direction given by the degree
     */
  private static Vector2D calculateTakeOffAngle(double angle, Vector2D earth, Vector2D sun){
    double distance = sqrt(Math.pow(earth.x()-sun.x(), 2) +  Math.pow(earth.y()-sun.y(), 2));
    Vector2D normalVersor = new Vector2D(earth.x()- sun.x(), earth.y()-sun.y());
    normalVersor.div(distance); // Normalize Vector
    Vector2D tgVersor = new Vector2D(- normalVersor.y(), normalVersor.x());

    tgVersor.times(cos(angle));
    normalVersor.times(sin(angle));

    Vector2D versor = new Vector2D(tgVersor.x() + normalVersor.x(), tgVersor.y() + normalVersor.y());

    return versor;

  }

  private static void generateReportFiles(ArrayList<ReportFile> reports) {

    for(int i=0; i< reports.size(); i++){
      final Path pathToFile = Paths.get(DESTINATION_FOLDER, SS_MIN_DISTANCE_FILE + i + ".dat");

      if(!deleteIfExists(pathToFile)) {
        return;
      }

    /* write the new file */
      writeFile(pathToFile, reports.get(i).toString(), false);

    }

  }

  private static class ReportFile {
    double dt;
    double ft;
    Vector2D takeOffAngle;
    double initialSpeed;
    double daysTakeOff;
    double degree;
    SolarSystem.SolarSystemState state;

    public ReportFile(double dt, double ft, Vector2D takeOffAngle, double initialSpeed, double daysTakeOff, double degree, SolarSystem.SolarSystemState state ){
      this.dt = dt;
      this.ft = ft;
      this.takeOffAngle = takeOffAngle;
      this.initialSpeed = initialSpeed;
      this.daysTakeOff = daysTakeOff;
      this.degree = degree;
      this.state = state;
    }

    double getDistanceToMars(){
      return state.getDistanceToMars();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("dt: ").append(dt).append(System.lineSeparator());
      sb.append("ft: ").append(ft).append(System.lineSeparator());
      sb.append("takeOffAngleX: ").append(takeOffAngle.x()).append(System.lineSeparator());
      sb.append("takeOffAngleY: ").append(takeOffAngle.y()).append(System.lineSeparator());
      sb.append("daysTakeOff: ").append(daysTakeOff).append(System.lineSeparator());
      sb.append("degree: ").append(degree).append(System.lineSeparator());
      sb.append("initial speed: ").append(initialSpeed).append(System.lineSeparator());
      sb.append(state.toString());
      return sb.toString();
    }
  }


  private static void toEarth(final String[] args) {
    if (args.length != 7 && args.length != 5) {
      System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
      exit(BAD_N_ARGUMENTS);
    }

    final double dt = parseAsDouble(args[1], "<dt>");
    final double ft = parseAsDouble(args[2], "<ft>");
    final double daysToTakeOff = parseAsDouble(args[3], "<days_to_take_off>");
    final double shipTakeOffV0 = parseAsDouble(args[4], "<ship_take_off_v0>") * KM_TO_M;

    final Vector2D shipTakeOffAngle;

    if (args.length == 7) {
      final double shipTakeOffAngleX = parseAsDouble(args[5], "<ship_take_off_angle_x>");
      final double shipTakeOffAngleY = parseAsDouble(args[6], "<ship_take_off_angle_y>");
      shipTakeOffAngle = new Vector2D(shipTakeOffAngleX, shipTakeOffAngleY);
    } else {
      shipTakeOffAngle = null;
    }

    // Generate static.dat file for this system to be used to generate ovito file in a future
    generateStaticDatFile(SOLAR_SYSTEM_N, -1, -1, -1, -1, -1, SOLAR_SYSTEM_W, SOLAR_SYSTEM_L);

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    final SolarSystem solarSystem = new SolarSystem(dt);

    long i = 0;
    double timeToTookOff = daysToSeconds(daysToTakeOff);

    double currentTime = 0;
    while (currentTime < timeToTookOff) {
      evolve(solarSystem, i++);
      currentTime +=dt;
    }

    solarSystem.takeOffFromMars(shipTakeOffV0, shipTakeOffAngle);

    for (double systemTime = 0; systemTime < ft; systemTime += dt) {
      evolve(solarSystem, i);
      if (solarSystem.shipCrashedEarth()) {
        System.out.println("[REACHED] - Ship landed on " + solarSystem.shipLandedTo());
        generateOutputDatFile(OutputType.SOLAR_SYSTEM, solarSystem.getParticles(), i);
        break;
      }
      i++;
    }

    generateReportFile(solarSystem.getMinDistanceToMarsSSState());
  }

  private static void toMars(final String[] args) {
    if (args.length != 7 && args.length != 5) {
      System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
      exit(BAD_N_ARGUMENTS);
    }

    final double dt = parseAsDouble(args[1], "<dt>");
    final double ft = parseAsDouble(args[2], "<ft>");
    final double daysToTakeOff = parseAsDouble(args[3], "<days_to_take_off>");
    final double shipTakeOffV0 = parseAsDouble(args[4], "<ship_take_off_v0>") * KM_TO_M;

    final Vector2D shipTakeOffAngle;

    if (args.length == 7) {
      final double shipTakeOffAngleX = parseAsDouble(args[5], "<ship_take_off_angle_x>");
      final double shipTakeOffAngleY = parseAsDouble(args[6], "<ship_take_off_angle_y>");
      shipTakeOffAngle = new Vector2D(shipTakeOffAngleX, shipTakeOffAngleY);
    } else {
      shipTakeOffAngle = null;
    }

    // Generate static.dat file for this system to be used to generate ovito file in a future
    generateStaticDatFile(SOLAR_SYSTEM_N, -1, -1, -1, -1, -1, SOLAR_SYSTEM_W, SOLAR_SYSTEM_L);

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    final SolarSystem solarSystem = new SolarSystem(dt);

    long i = 0;
    double timeToTookOff = daysToSeconds(daysToTakeOff);

    double currentTime = 0;
    while (currentTime < timeToTookOff) {
      evolve(solarSystem, i++);
      currentTime +=dt;
    }

    solarSystem.takeOff(shipTakeOffV0, shipTakeOffAngle);

    for (double systemTime = 0; systemTime < ft; systemTime += dt) {
      evolve(solarSystem, i);
      if (solarSystem.shipCrashed()) {
        System.out.println("[REACHED] - Ship landed on " + solarSystem.shipLandedTo());
        generateOutputDatFile(OutputType.SOLAR_SYSTEM, solarSystem.getParticles(), i);
        break;
      }
      i++;
    }

    generateReportFile(solarSystem.getMinDistanceToMarsSSState());
  }

  /**
   * Parses as double the given string.
   * Exits if an error is encountered
   * @param s string to be parsed
   * @param varErrMsg variable name to be displayed if an error raise
   * @return the parsed double
   */
  private static double parseAsDouble(final String s, final String varErrMsg) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      LOGGER.warn("[FAIL] - " + varErrMsg + " must be a number. Caused by: ", e);
      System.out.println("[FAIL] - " + varErrMsg + " argument must be a number. Try 'help' for more information.");
      exit(BAD_ARGUMENT);
      // should not get here
      return -1;
    }
  }

  private static void evolve(final SolarSystem solarSystem, final long i) {
    if (i%10 == 0) { // print system after 10 dt units
      generateOutputDatFile(OutputType.SOLAR_SYSTEM, solarSystem.getParticles(), i);
    }
    solarSystem.evolveSystem();
  }

  private static double daysToSeconds(final double days) {
    return days * SECONDS_PER_DAY;
  }

  private static void generateReportFile(final SolarSystem.SolarSystemState minDistanceToMarsSSState) {
    /* delete previous file, if any */
    final Path pathToFile = Paths.get(DESTINATION_FOLDER, SS_REPORT_FILE);

    if(!deleteIfExists(pathToFile)) {
      return;
    }

    final String data = minDistanceToMarsSSState.toString();

    /* write the new file */
    writeFile(pathToFile, data, false);
  }

  private static void generateCase(final String[] args) {
    // another arg is needed
    if (args.length < 2) {
      System.out.println("[FAIL] - No file specified. Try 'help' for more information.");
      exit(NO_FILE);
    }

    switch (args[1]) {
      case "staticdat":
        if (args.length != 8) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        int N = 0;
        try {
          N = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <N> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <N> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double mass = 0;
        try {
          mass = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <m> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <m> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double r = 0;
        try {
          r = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <r> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <r> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double k = 0;
        try {
          k = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <k> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <k> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double gamma = 0;
        try {
          gamma = Double.parseDouble(args[6]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <gamma> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <gamma> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double tf = 0;
        try {
          tf = Double.parseDouble(args[7]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <tf> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <tf> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        generateStaticDatFile(N, mass, r, k, gamma, tf, 0, 7);

        break;

      case "ovito":
        // get particle id
        if (args.length != 4) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        final String staticFile = args[2];
        final String outputFile = args[3];

        generateOvitoFile(staticFile, outputFile);
        break;

      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;

    }
  }

  private static void generateStaticDatFile(final int N,
                                            final double mass,
                                            final double r,
                                            final double k,
                                            final double gamma,
                                            final double tf,
                                            final double W,
                                            final double L) {
    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, STATIC_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    final StringBuilder sb = new StringBuilder();
    sb      .append(N).append(System.lineSeparator())
            .append(mass).append(System.lineSeparator())
            .append(r).append(System.lineSeparator())
            .append(k).append(System.lineSeparator())
            .append(gamma).append(System.lineSeparator())
            .append(tf).append(System.lineSeparator())
            .append(W).append(System.lineSeparator())
            .append(L).append(System.lineSeparator());

    /* write the new static.dat file */
    writeFile(pathToDatFile, sb.toString(), false);
  }

  private static void cushionedOscillator(final String[] args) {
    if (args.length != 4) {
      System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
      exit(BAD_N_ARGUMENTS);
    }

    double dt = 0;
    try {
      dt = Double.parseDouble(args[3]);
    } catch (NumberFormatException e) {
      LOGGER.warn("[FAIL] - <dt> must be a number. Caused by: ", e);
      System.out.println("[FAIL] - <dt> argument must be a number. Try 'help' for more information.");
      exit(BAD_ARGUMENT);
    }


    final StaticData staticData = loadStaticFile(args[1]);

    if(staticData.N <= 0  || staticData.mass <= 0 || staticData.k < 0 || staticData.tf < 0) {
      System.out.println("[FAIL] - The following must not happen: N<0 or mass < 0 or k < 0 or tf < 0.\n" +
              "Please check the input files.");
      exit(BAD_ARGUMENT);
    }

    final Oscillator oscillator = pickOscilator(staticData, dt, args[2]);

    if (oscillator == null) {
      exit(UNEXPECTED_ERROR);
    }

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    List<Particle> particles;

    long i = 0;
    for(double systemTime = 0; systemTime < staticData.tf; systemTime += dt) {
      if(i%10 == 0){ // print system after 10 dt units
        particles = new ArrayList<>();
        particles.add(oscillator.getParticle());
        generateOutputDatFile(OutputType.COMMON, particles, i);
      }
      oscillator.evolveSystem();
      i++;
    }
  }

  private static Oscillator pickOscilator(final StaticData staticData, final double dt, final String arg) {
    switch (arg) {
      case "analytic":
        return new OscillatorAnalyticIntegration(
                staticData.mass,
                staticData.r,
                staticData.k,
                staticData.gamma,
                dt
        );
      case "verlet":
        return new OscillatorVerletIntegration(
                staticData.mass,
                staticData.r,
                staticData.k,
                staticData.gamma,
                dt
        );
      case "beeman":
        return new OscillatorBeemanIntegration(
                staticData.mass,
                staticData.r,
                staticData.k,
                staticData.gamma,
                dt
        );
      case "gear":
        return new OscillatorGearIntegration(
                staticData.mass,
                staticData.r,
                staticData.k,
                staticData.gamma,
                dt
        );
      default:
        LOGGER.warn("[FAIL] - <type> must be valid.");
        System.out.println("[FAIL] - <type> must be valid. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;
    }

    return null;
  }

  private static void generateOutputDatFile(final OutputType outputType,
                                            final List<Particle> particles,
                                            final long iteration) {
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    final String data;
    switch (outputType) {
      case SOLAR_SYSTEM:
        data = serializeSolarSystem(particles, iteration);
        break;
      default:
        data = serializeParticles(particles, iteration);
        break;
    }

    /* write the new output.dat file */
    writeFile(pathToDatFile, data, true);
  }

  private static String serializeSolarSystem(final List<Particle> particles, final long iteration) {
    final StringBuilder sb = new StringBuilder();
    final int N = particles.size();
    sb.append(N+4).append(System.lineSeparator());
    sb.append(iteration).append(System.lineSeparator());
    double vx, vy, r, g, b;
    for (Particle particle : particles) {
      vx = particle.vx();
      vy = particle.vy();
      switch (particle.type()) {
        case SUN:
          r = 255;
          g = 255;
          b = 0;
          break;
        case EARTH:
          r = 0;
          g = 0;
          b = 255;
          break;
        case MARS:
          r = 255;
          g = 0;
          b = 0;
          break;
        case SHIP:
          r = 0;
          g = 255;
          b = 0;
          break;
        default:
          r = 255;
          g = 255;
          b = 255;
          break;
      }
      sb      .append(particle.id()).append('\t')
              // position
              .append(particle.x()).append('\t').append(particle.y()).append('\t')
              // velocity
              .append(vx).append('\t').append(vy).append('\t')
              // R G B colors
              .append(r).append('\t')
              .append(g).append('\t')
              .append(b).append('\t')
              // radio
              .append(particle.radio()).append('\t')
              // type
              .append(particle.type()).append('\t')
              // age in days
              .append(particle.ageInDays()).append('\n');
    }

    // Edge particles
    final double W = SOLAR_SYSTEM_W;
    final double L = SOLAR_SYSTEM_L;
    sb      // id
            .append(N+1).append('\t')
            // position
            .append(-W/2).append('\t').append(-L/2).append('\t')
            // velocity
            .append(0).append('\t').append(0).append('\t')
            // color: black [ r, g, b ]
            .append(0).append('\t').append(0).append('\t').append(0).append('\t')
            // radio
            .append(0).append('\t')
            // type
            .append("EDGE").append('\t')
            // age in days
            .append(0).append('\n');

    sb      // id
            .append(N+2).append('\t')
            // position
            .append(W/2).append('\t').append(-L/2).append('\t')
            // velocity
            .append(0).append('\t').append(0).append('\t')
            // color: black [ r, g, b ]
            .append(0).append('\t').append(0).append('\t').append(0).append('\t')
            // radio
            .append(0).append('\t')
            // type
            .append("EDGE").append('\t')
            // age in days
            .append(0).append('\n');

    sb      // id
            .append(N+3).append('\t')
            // position
            .append(W/2).append('\t').append(L/2).append('\t')
            // velocity
            .append(0).append('\t').append(0).append('\t')
            // color: black [ r, g, b ]
            .append(0).append('\t').append(0).append('\t').append(0).append('\t')
            // radio
            .append(0).append('\t')
            // type
            .append("EDGE").append('\t')
            // age in days
            .append(0).append('\n');

    sb      // id
            .append(N+4).append('\t')
            // position
            .append(-W/2).append('\t').append(L/2).append('\t')
            // velocity
            .append(0).append('\t').append(0).append('\t')
            // color: black [ r, g, b ]
            .append(0).append('\t').append(0).append('\t').append(0).append('\t')
            // radio
            .append(0).append('\t')
            // type
            .append("EDGE").append('\t')
            // age in days
            .append(0).append('\n');

    return sb.toString();
  }

  private static void writeFile(final Path pathToFile, final String data, final boolean append) {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToFile.toFile(), append));
      writer.write(data);
    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        // close the writer regardless of what happens...
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ignored) {

      }
    }
  }

  // Used for building output.dat
  private static String serializeParticles(final List<Particle> pointsSet, long iteration) {
    final StringBuilder sb = new StringBuilder();
    sb.append(iteration).append('\n');
    double vx, vy, r, g, b;
    for (Particle point : pointsSet) {
      vx = point.vx();
      vy = point.vy();
      r = 255;
      g = 255;
      b = 255;
      sb.append(point.id()).append('\t')
              // position
              .append(point.x()).append('\t').append(point.y()).append('\t')
              // velocity
              .append(vx).append('\t').append(vy).append('\t')
              // R G B colors
              .append(r).append('\t')
              .append(g).append('\t')
              //.append(b).append('\n');
              .append(b).append('\t')
              .append(1).append('\n');
    }
    return sb.toString();
  }

  /**
   *  Generate a .XYZ file which contains the following information about a particle:
   *  - id
   *  - X Position
   *  - Y Position
   *  - X Speed
   *  - Y Speed
   *  - R color - vx
   *  - G color - vy
   *  - B color - vx + vy
   *  By default, the output file is 'graphics.xyz' which is stored in the 'data' folder.
   * @param staticFile -
   * @param outputFile -
   */
  private static void generateOvitoFile(final String staticFile, final String outputFile) {
    final Path pathToStaticDatFile = Paths.get(staticFile);
    final Path pathToOutputDatFile = Paths.get(outputFile);
    final Path pathToGraphicsFile = Paths.get(DESTINATION_FOLDER, OVITO_FILE);

    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    //noinspection ResultOfMethodCallIgnored
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    if(!deleteIfExists(pathToGraphicsFile)) {
      return;
    }

    Stream<String> staticDatStream = null;
    Stream<String> outputDatStream = null;

    try {
      staticDatStream = Files.lines(pathToStaticDatFile);
      outputDatStream = Files.lines(pathToOutputDatFile);
    } catch (IOException e) {
      LOGGER.warn("Could not read a file. Details: ", e);
      System.out.println("Could not read one of these files: '" + pathToStaticDatFile + "' or '"
              + pathToOutputDatFile + "'.\n" +
              "Check the logs for a detailed info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    }

    BufferedWriter writer = null;

    try {
      String stringN; // N as string
      String iterationNum, borderParticles;
      int N;
      final double L, W;
      final Iterator<String> outputDatIterator;
      final StringBuilder sb = new StringBuilder();

      final StaticData staticData = loadStaticFile(staticFile);

      writer = new BufferedWriter(new FileWriter(pathToGraphicsFile.toFile()));
      outputDatIterator = outputDatStream.iterator();

      // Write number of particles
      N = staticData.N;
      W = staticData.W;
      L = staticData.L;

      // Create virtual particles in the borders, in order for Ovito to show the whole board

      sb      // id
              .append(N+1).append('\t')
              // type
                //.append(N+1).append('\t')
              // position
              //.append(0).append('\t').append(0).append('\t')
              .append(-W/2).append('\t').append(-L/2).append('\t')
              // velocity
              .append(0).append('\t').append(0).append('\t')
              // color: black [ r, g, b ]
              .append(0).append('\t').append(0).append('\t').append(0).append('\t')
              // radio
              .append(0)
              .append('\n');

      sb      // id
              .append(N+2).append('\t')
              // type
                //.append(N+2).append('\t')
              // position
              //.append(W).append('\t').append(0).append('\t')
              .append(W/2).append('\t').append(-L/2).append('\t')
              // velocity
              .append(0).append('\t').append(0).append('\t')
              // color: black [ r, g, b ]
              .append(0).append('\t').append(0).append('\t').append(0).append('\t')
              // radio
              .append(0)
              .append('\n');

      sb      // id
              .append(N+3).append('\t')
              // type
                //.append(N+3).append('\t')
              // position
              //.append(W).append('\t').append(L).append('\t')
              .append(W/2).append('\t').append(L/2).append('\t')
              // velocity
              .append(0).append('\t').append(0).append('\t')
              // color: black [ r, g, b ]
              .append(0).append('\t').append(0).append('\t').append(0).append('\t')
              // radio
              .append(0)
              .append('\n');

      sb      // id
              .append(N+4).append('\t')
              // type
                //.append(N+4).append('\t')
              // position
              //.append(0).append('\t').append(L).append('\t')
              .append(-W/2).append('\t').append(L/2).append('\t')
              // velocity
              .append(0).append('\t').append(0).append('\t')
              // color: black [ r, g, b ]
              .append(0).append('\t').append(0).append('\t').append(0).append('\t')
              // radio
              .append(0)
              .append('\n');

      stringN = String.valueOf(N+4);

      borderParticles = sb.toString();

      while(outputDatIterator.hasNext()){
        // Write amount of particles (N)
        writer.write(stringN);
        writer.newLine();

        // Write iteration number
        iterationNum = outputDatIterator.next();
        writer.write(iterationNum);
        writer.newLine();

                /*
                  Write particle information in this order
                  Particle_Id     X_Pos	Y_Pos   X_Vel   Y_Vel R G B
                */
        for(int i=0; i<N; i++){
          writer.write(outputDatIterator.next() + "\n");
        }

        // Write border particles
        writer.write(borderParticles);


      }
    } catch(final IOException e) {
      LOGGER.warn("Could not write to '{}'. Caused by: ", pathToGraphicsFile, e);
      System.out.println("Could not write to '" + pathToGraphicsFile + "'." +
              "\nCheck the logs for a detailed info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        if(writer != null) {
          writer.close();
        }
        staticDatStream.close();
        outputDatStream.close();
      } catch (final IOException ignored) {

      }
    }
  }




  /**
   * Try to delete a file, whether it exists or not
   * @param pathToFile the file path that refers to the file that will be deleted
   * @return true if there were not errors when trying to delete the file;
   * 		   false in other case;
   */
  private static boolean deleteIfExists(final Path pathToFile) {
    try {
      Files.deleteIfExists(pathToFile);
    } catch(IOException e) {
      LOGGER.warn("Could not delete previous file: '{}'. Caused by: ", pathToFile, e);
      System.out.println("Could not delete previous file: '" + pathToFile + "'.\n");
      return false;
    }
    return true;
  }

  private static class StaticData {
    private int N;
    private double mass;
    private double r;
    private double k;
    private double gamma;
    private double tf;
    private double W;
    private double L;
  }

  private static StaticData loadStaticFile(final String filePath) {
    final StaticData staticData = new StaticData();

    final File staticFile = new File(filePath);
    if (!staticFile.isFile()) {
      System.out.println("[FAIL] - File '" + filePath + "' is not a normal file. Aborting...");
      exit(NOT_A_FILE);
    }

    try (final Stream<String> staticStream = Files.lines(staticFile.toPath())) {
      final Iterator<String> staticFileLines = staticStream.iterator();

      staticData.N = Integer.valueOf(staticFileLines.next());

      // get mass
      staticData.mass = Double.valueOf(staticFileLines.next());

      staticData.r = Double.valueOf(staticFileLines.next());

      staticData.k = Double.valueOf(staticFileLines.next());

      staticData.gamma = Double.valueOf(staticFileLines.next());

      staticData.tf = Double.valueOf(staticFileLines.next());

      staticData.W = Double.valueOf(staticFileLines.next());

      staticData.L = Double.valueOf(staticFileLines.next());


    } catch (final IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while reading the file {}. Caused by: ", staticFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while reading the file '" + staticFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } catch (final NumberFormatException e) {
      LOGGER.warn("[FAIL] - Number expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + staticFile + "'.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    }

    return staticData;
  }

  private static void exit(final EXIT_CODE exitCode) {
    System.exit(exitCode.getCode());
  }

}
