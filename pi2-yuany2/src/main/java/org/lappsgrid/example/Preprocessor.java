package org.lappsgrid.example;

import org.lappsgrid.api.ProcessingService;
import static org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;
// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * First step in PI2 pipeline: preprocess the raw text into question/answer fields with their
 * attributes
 * 
 * @author yuany
 *
 */
public class Preprocessor implements ProcessingService {

  /**
   * storing the metadata
   */
  private String metadata;

  public Preprocessor() {
    metadata = generateMetadata();
  }

  /**
   * Generate metadata
   * 
   * @return a newly created metadata object
   */
  private String generateMetadata() {
    // Create and populate the metadata object
    ServiceMetadata metadata = new ServiceMetadata();

    // Populate metadata using setX() methods
    metadata.setName(this.getClass().getName());
    metadata.setDescription("Preprocess the raw text input into question and answer fields");
    metadata.setVersion("1.0.0-SNAPSHOT");
    metadata.setVendor("http://www.lappsgrid.org");
    metadata.setLicense(Uri.APACHE2);

    // JSON for input information
    IOSpecification requires = new IOSpecification();
    requires.addFormat(Uri.TEXT); // Plain text (form)
    requires.addLanguage("en"); // Source language
    requires.setEncoding("UTF-8");

    // JSON for output information
    IOSpecification produces = new IOSpecification();
    produces.addFormat(Uri.LAPPS); // LIF (form) synonymous to LIF
    produces.addAnnotation(Uri.SENTENCE); // each q and a are sentences
    requires.addLanguage("en"); // Target language
    produces.setEncoding("UTF-8");

    // Embed I/O metadata JSON objects
    metadata.setRequires(requires);
    metadata.setProduces(produces);

    // Serialize the metadata to a string and return
    Data<ServiceMetadata> data = new Data<>(Uri.META, metadata);

    return data.asPrettyJson();
  }

  @Override
  /**
   * @return return the stored metadata
   */
  public String getMetadata() {
    return metadata;
  }

  @Override
  /**
   * Take a raw text as input and output a LIF with annotations on different fields
   * 
   * @param input
   *          LIF JSON string
   * 
   * @return LIF JSOn string
   */
  public String execute(String input) {
    // Step #1: Parse the input.
    Data data = Serializer.parse(input, Data.class);
    // Step #2: Check the discriminator
    final String discriminator = data.getDiscriminator();
    if (discriminator.equals(Uri.ERROR)) {
      // Return the input unchanged.
      return input;
    }
    // Step #3: Extract the text.
    Container container = null;
    if (discriminator.equals(Uri.TEXT)) {
      container = new Container();
      container.setText(data.getPayload().toString());
    } else {
      // This is a format we don't accept.
      String message = String.format("Unsupported discriminator type: %s", discriminator);
      return new Data<String>(Uri.ERROR, message).asJson();
    }

    // Step #4: Create a new View
    View view = container.newView();

    // Step #5: Identify the question and answer
    String filepath = container.getText(); // read the filepath
    Scanner scanner;
    ArrayList<ArrayList<Integer>> triples = null;
    try {
      scanner = new Scanner(new FileReader(filepath));
      if (scanner.hasNext()) { // only read one line
        String line = scanner.nextLine();
        triples = new ArrayList<ArrayList<Integer>>();
        // TODO scan through line and create triples <start, end, isCorrect> for question and each
        // answer (question appears at the first and isCorrect field is left empty)
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    if (triples != null && triples.size() > 1) {
      int id = 0;
      for (ArrayList<Integer> triple : triples) {
        Annotation a = view.newAnnotation(id == 0 ? "q" : "a" + id, Uri.SENTENCE, triple.get(0),
                triple.get(1));
        a.addFeature(Stats.STATS1, triple.get(2) + "");
        id++;
      }

    }

    // Step #6: Update the view's metadata. Each view contains metadata about the
    // annotations it contains, in particular the name of the tool that produced the
    // annotations.
    view.addContains(Uri.SENTENCE, this.getClass().getName(), "qa_sentences");
    // Step #7: Create a DataContainer with the result.
    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }

  /**
   * A class extends the Features class in LAPPS, which is used to store various value such as score
   * and whether it is correct answer
   * 
   * @author yuany
   *
   */
  public static class Stats extends Features.Markable {
    public static final String STATS1 = "pi2stats1";

    public static final String STATS2 = "pi2stats2";
  }
}