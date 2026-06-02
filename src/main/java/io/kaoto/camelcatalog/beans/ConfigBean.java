package io.kaoto.camelcatalog.beans;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.kaoto.camelcatalog.model.CatalogCliArgument;

public class ConfigBean {
    private File outputFolder;
    private String catalogsName;
    private Set<CatalogCliArgument> catalogVersionSet = new LinkedHashSet<>();
    private String kameletsVersion;
    private boolean verbose = false;
    private List<String> repositories = new ArrayList<>();

    public ConfigBean() {
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = new File(outputFolder);
    }

    public String getCatalogsName() {
        return catalogsName;
    }

    public void setCatalogsName(String catalogsName) {
        this.catalogsName = catalogsName;
    }

    public void addCatalogVersion(CatalogCliArgument catalogCliArg) {
        this.catalogVersionSet.add(catalogCliArg);
    }

    public Set<CatalogCliArgument> getCatalogVersionSet() {
        return catalogVersionSet;
    }

    public String getKameletsVersion() {
        return kameletsVersion;
    }

    public void setKameletsVersion(String kameletsVersion) {
        this.kameletsVersion = kameletsVersion;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : repositories;
    }
}
