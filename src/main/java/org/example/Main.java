package org.example;


import be.ugent.rml.Executor;
import be.ugent.rml.Utils;
import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.functions.lib.IDLabFunctions;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.NamedNode;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Main {

    private static final String RML_OUTPUT_PATH = "rml_output";

    public static void main(String[] args) throws Exception {

        rmlMapper();

        var repository = new HTTPRepository("http://localhost:7200/repositories/testRepo1");
        var connection = repository.getConnection();

        connection.clear();
        connection.begin();

        try {
            connection.add(
                    Main.class.getClassLoader().getResourceAsStream("data/imdb_ontology.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);

            connection.add(
                    Main.class.getClassLoader().getResourceAsStream(RML_OUTPUT_PATH + "/actors_output.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);

            connection.add(
                    Main.class.getClassLoader().getResourceAsStream(RML_OUTPUT_PATH + "/movies_output.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);

            connection.add(
                    Main.class.getClassLoader().getResourceAsStream(RML_OUTPUT_PATH + "/ratings_output.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        connection.commit();
        repository.shutDown();
    }

    private static void rmlMapper() throws Exception {
        var rootFolder = "./src/main/resources/";

        String[][] files = {{"data/actors.mappings.ttl", RML_OUTPUT_PATH + "/actors_output.ttl"},
                            {"data/movies.mappings.ttl", RML_OUTPUT_PATH + "/movies_output.ttl"},
                            {"data/ratings.mappings.ttl", RML_OUTPUT_PATH + "/ratings_output.ttl"}};

        for (var pair : files) {
            var mapPath = rootFolder + pair[0];
            var mappingFile = new File(mapPath);

            var outPath = rootFolder + pair[1];

            try (var output = new FileWriter(outPath)) {
                var mappingStream = new FileInputStream(mappingFile);
                var rmlStore = QuadStoreFactory.read(mappingStream);

                var factory = new RecordsFactory(mappingFile.getParent());

                var libraryMap = new HashMap<String, Class>();
                libraryMap.put("IDLabFunctions", IDLabFunctions.class);

                var functionLoader = new FunctionLoader(null, libraryMap);
                var outputStore = new RDF4JStore();

                var executor = new Executor(rmlStore, factory, functionLoader, outputStore, Utils.getBaseDirectiveTurtle(mappingStream));
                var result = executor.executeV5(null).get(new NamedNode("rmlmapper://default.store"));
                result.write(output, "turtle");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}