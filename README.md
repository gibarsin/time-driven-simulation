# Cushioned Punctual Oscillator
Java Implementation of the Cushioned Punctual Oscillator Simulation
## Build
To build the project, it is necessary to have Maven and Java 1.8 installed.
Then, run

    $ mvn clean package
    
## Execution
To run the program, from the root folder

    $ java -jar core/target/time-driven-simulation.jar <arguments>

## Simulation
`help` argument is a highly detailed help menu that show possible usages of the current program.
So, we highly recommend that for using this jar, you may run

    $ java -jar core/target/time-driven-simulation.jar help

### Usage examples

Reach Mars from Earth #1 - without initial angle

    $ java -jar core/target/time-driven-simulation.jar toMars 100 31536000 578.732 8

Reach Mars from Earth #2 - with initial angle
    
    $ java -jar core/target/time-driven-simulation.jar toMars 100 31536000 603.49 7 -1 -2

Reach Earth from Mars

    $ java -jar core/target/time-driven-simulation.jar toEarth 1000 31536000 755 15 -1 -.6873