package io.kaoto.camelcatalog;

import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.commands.GenerateCommand;
import io.kaoto.camelcatalog.commands.GenerateCommandOptions;

public class Main {
    private static final Logger LOGGER;
    static final int EXIT_CODE_SUCCESS = 0;
    static final int EXIT_CODE_FAILURE = 1;

    static {
        // Load logging configuration from classpath
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logging configuration: " + e.getMessage());
        }
        LOGGER = Logger.getLogger(Main.class.getName());
    }

    public static void main(String[] args) {
        ConfigBean configBean = new ConfigBean();
        GenerateCommandOptions generateCommandOptions = new GenerateCommandOptions(configBean);
        int exitCode = EXIT_CODE_SUCCESS;

        try {
            generateCommandOptions.configure(args);
        } catch (Exception e) {
            LOGGER.severe("Error: " + e.getMessage());
            generateCommandOptions.printHelp();
            exitCode = EXIT_CODE_FAILURE;
        }

        GenerateCommand generateCommand = new GenerateCommand(configBean);
        generateCommand.run();

        exit(exitCode);
    }

    static void exit(int status) {
        System.exit(status);
    }
}
