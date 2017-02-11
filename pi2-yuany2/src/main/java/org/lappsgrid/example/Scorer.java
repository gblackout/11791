package org.lappsgrid.example;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.example.Preprocessor.Stats;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import java.util.Map;
import java.util.Random;

/**
 * Fourth step in PI2. Takes the ngram view and compute score for each answer, which is added to the
 * feature in first view
 * 
 * @author yuany
 *
 */
public class Scorer implements ProcessingService {

  /**
   * storing the metadata
   */
  private String metadata;

  private Random rand;

  public Scorer() {
    metadata = generateMetadata();
    rand = new Random();
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
    metadata.setDescription("Score each answer using ngram features");
    metadata.setVersion("1.0.0-SNAPSHOT");
    metadata.setVendor("http://www.lappsgrid.org");
    metadata.setLicense(Uri.APACHE2);

    // JSON for input information
    IOSpecification requires = new IOSpecification();
    requires.addFormat(Uri.LIF); // LIF (form)
    requires.addAnnotation(Uri.TOKEN);
    requires.addLanguage("en"); // Source language
    requires.setEncoding("UTF-8");

    // JSON for output information
    IOSpecification produces = new IOSpecification();
    produces.addFormat(Uri.LAPPS); // LIF (form) synonymous to LIF
    produces.addAnnotation(Uri.SENTENCE);
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
   * Take LIF which contains 3 views from previous steps and add score to the first view for each
   * annotation
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
    if (discriminator.equals(Uri.LAPPS)) {
      container = new Container((Map) data.getPayload());
    } else {
      // This is a format we don't accept.
      String message = String.format("Unsupported discriminator type: %s", discriminator);
      return new Data<String>(Uri.ERROR, message).asJson();
    }

    // get the ngram view
    View ngramView = container.getView(2);
    // get the qa view
    View qaView = container.getView(0);

    for (Annotation ann : qaView.getAnnotations()) {
      ann.addFeature(Stats.STATS2, this.getScore(ann.getId(), ngramView) + "");
    }

    // Step #7: Create a DataContainer with the result.
    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }

  /**
   * function that returns score of an answer given ngram; currently it's just a place holder
   * need to distinguish if it's question; if it is then return negative score
   * 
   * @param id
   *          String id of the answer to be scored
   * @param ngramView
   *          view produced by NGramMaker
   * @return the score of the answer
   */
  private double getScore(String id, View ngramView) {
    return this.rand.nextDouble();
  }
}