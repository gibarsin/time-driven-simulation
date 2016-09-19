package ar.edu.itba.ss.cushioned_oscilator.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ar.edu.itba.ss.cushioned_oscilator.interfaces.Interface;
import ar.edu.itba.ss.cushioned_oscilator.models.Model;

public class Service implements Interface {
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
    private final Model model;

    public Service() {
        LOGGER.debug("Creating model...");
        model = new Model();
    }

    @Override
    public void printToScreen() {
        LOGGER.debug("Printing model to screen...");
        System.out.println(model);
    }
}
