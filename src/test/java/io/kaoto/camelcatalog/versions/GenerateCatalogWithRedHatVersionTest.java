package io.kaoto.camelcatalog.versions;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.commands.GenerateCommand;
import io.kaoto.camelcatalog.model.CatalogCliArgument;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCatalogWithRedHatVersionTest {

    @TempDir
    File tempDir;

    @Test
    void testOK() {
        CatalogCliArgument catalogCliArg = new CatalogCliArgument();
        catalogCliArg.setRuntime(CatalogRuntime.Main);
        catalogCliArg.setCatalogVersion("4.4.0.redhat-00045");

        ConfigBean configBean = new ConfigBean();
        configBean.setOutputFolder(tempDir.toString());
        configBean.setCatalogsName("test-camel-catalog");
        configBean.addCatalogVersion(catalogCliArg);
        configBean.setKameletsVersion("1.0.0");

        GenerateCommand generateCommand = new GenerateCommand(configBean);

        generateCommand.run();

        File catalogDir = new File(tempDir, "camel-main/4.4.0.redhat-00045");

        assertTrue(catalogDir.exists(), "The folder for the catalog wasn't created");
        assertEquals(18, Objects.requireNonNull(catalogDir.listFiles()).length,
                "The folder for the catalog doesn't contain the correct number of files");
    }

}
