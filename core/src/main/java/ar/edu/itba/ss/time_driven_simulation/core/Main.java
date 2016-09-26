package ar.edu.itba.ss.time_driven_simulation.core;

import ar.edu.itba.ss.time_driven_simulation.models.Particle;
import ar.edu.itba.ss.time_driven_simulation.services.*;
import ar.edu.itba.ss.time_driven_simulation.services.OscillatorGearIntegration;
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

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private static final String DESTINATION_FOLDER = "output";
  private static final String STATIC_FILE = "static.dat";
  private static final String DYNAMIC_FILE = "dynamic.dat";
  private static final String OUTPUT_FILE = "output.dat";
  private static final String DATA_FOR_GRAPHICS_FILE = "i_t_fp_pre_temp.csv";
  private static final String OVITO_FILE = "graphics.xyz";
  private static final String TIME_TO_EQUILIBRIUM_FILE = "time_to_eq.csv";
  private static final long MAX_TIME_AFTER_EQUILIBRIUM = 100;
  private static final int SYSTEM_PARTICLES_INDEX = 0;
  private static final int KINETIC_ENERGY_INDEX = 1;
  private static final String HELP_TEXT =
          "Cushioned Oscillator Simulation Implementation.\n" +
                  "Arguments: \n" +
                  "* gen staticdat <N> <m> <r> <k> <gamma> <tf> : \n" +
                  "\t generates an output/static.dat file with the desired parameters.\n" +
                  "* osc <path/to/static.dat> <dt>\n" +
                  "\t runs the cushioned-oscillator simulation and saves snapshots of the system in <output.dat>.\n" +
                  "* gen ovito <path/to/static.dat> <path/to/output.dat> <W> <L>: \n"+
                  "\t generates an output/graphics.xyz file (for Ovito) with the result of the simulation\n " +
                  "\t (<output.dat>) generated with the static file.\n";


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
      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;
    }

    System.out.println("[DONE]");
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

        generateStaticDatFile(N, mass, r, k, gamma, tf);

        break;

      case "ovito":
        // get particle id
        if (args.length != 6) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        final String staticFile = args[2];
        final String outputFile = args[3];

        double L = 0;
        try {
          L = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <L> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <L> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double W = 0;
        try {
          W = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <W> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <W> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        generateOvitoFile(staticFile, outputFile, L, W);
        break;

      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;

    }
  }

  private static void generateStaticDatFile(final int N, final double mass, final double r, final double k, final double gamma,
                                            final double tf) {
    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, STATIC_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    /* write the new static.dat file */
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToDatFile.toFile()));
      writer.write(String.valueOf(N));
      writer.write("\n");
      writer.write(String.valueOf(mass));
      writer.write("\n");
      writer.write(String.valueOf(r));
      writer.write("\n");
      writer.write(String.valueOf(k));
      writer.write("\n");
      writer.write(String.valueOf(gamma));
      writer.write("\n");
      writer.write(String.valueOf(tf));
      writer.write("\n");

    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToDatFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToDatFile + "'. \n" +
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

  private static void cushionedOscillator(final String[] args) {
    if (args.length != 3) {
      System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
      exit(BAD_N_ARGUMENTS);
    }

    double dt = 0;
    try {
      dt = Double.parseDouble(args[2]);
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

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);
    final Path pathToGraphicsFile = Paths.get(DESTINATION_FOLDER, DATA_FOR_GRAPHICS_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }
    if(!deleteIfExists(pathToGraphicsFile)){
      return;
    }

    //TODO: To run solarSystem, comment 1), 2) && 3) and uncomment the line under each one of these.
    // To run a different oscillator, simply change the instance 1)

    // 1)
    final OscillatorBeemanIntegration oscillator = new OscillatorBeemanIntegration(
            staticData.mass,
            staticData.r,
            staticData.k,
            staticData.gamma,
            dt
    );
    //final SolarSystem solarSystem = new SolarSystem(dt);

    List<Particle> particles;

    long i = 0;
    for(double systemTime = 0; systemTime < staticData.tf; systemTime += dt) {
      if(i%10 == 0){ // print system after 10 dt units
        particles = new ArrayList<>();
        // 2)
        particles.add(oscillator.getParticle());
        //particles.addAll(solarSystem.getParticles());
        generateOutputDatFile(particles, i);
      }
      // 3)
      oscillator.evolveSystem();
      //solarSystem.evolveSystem();
      i++;
    }
  }

  private static void generateOutputDatFile(final List<Particle> updatedParticles, final long iteration) {
//		/* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);

    /* write the new output.dat file */
    final String data = pointsToString(updatedParticles, iteration);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToDatFile.toFile(), true));
      writer.write(data);
    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToDatFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToDatFile + "'. \n" +
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
  private static String pointsToString(final List<Particle> pointsSet, long iteration) {
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
  private static void generateOvitoFile(final String staticFile, final String outputFile, double W, double L) {
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
      //final double L, W;
      final Iterator<String> staticDatIterator;
      final Iterator<String> outputDatIterator;
      final StringBuilder sb = new StringBuilder();

      final StaticData staticData = loadStaticFile(staticFile);

      writer = new BufferedWriter(new FileWriter(pathToGraphicsFile.toFile()));
      staticDatIterator = staticDatStream.iterator();
      outputDatIterator = outputDatStream.iterator();

      // Write number of particles
      N = staticData.N;

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
    private double mass;
    private double r;
    private double k;
    private double gamma;
    private double tf;
    private int N;
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
