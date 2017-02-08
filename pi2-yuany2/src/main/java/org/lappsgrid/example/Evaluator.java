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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Last step in PI2. Takes the first view and sort the annotations w.r.t their scores and compute
 * the P(at)N
 * 
 * @author yuany
 *
 */
public class Evaluator implements ProcessingService {

  /**
   * storing the metadata
   */
  private String metadata;

  public Evaluator() {
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
    metadata.setDescription("Sort answers with thier scores and compute the precision@N");
    metadata.setVersion("1.0.0-SNAPSHOT");
    metadata.setVendor("http://www.lappsgrid.org");
    metadata.setLicense(Uri.APACHE2);

    // JSON for input information
    IOSpecification requires = new IOSpecification();
    requires.addFormat(Uri.LIF); // LIF (form)
    requires.addAnnotation(Uri.SENTENCE);
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
   * Take a LIF which contains 4 views from previous step, and sort the first view w.r.t the score
   * it contains
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

    View qaView = container.getView(0);
    int N = 0;
    int n = 0;
    for (Annotation ann : qaView.getAnnotations()) {
      N = ann.getFeature(Stats.STATS1).equals("1") ? N + 1 : N;
    }
    List<Annotation> annotations = qaView.getAnnotations();
    annotations.sort(new Comparator<Annotation>() {
      @Override
      public int compare(Annotation o1, Annotation o2) {
        double left = Double.parseDouble(o1.getFeature(Stats.STATS2));
        double right = Double.parseDouble(o2.getFeature(Stats.STATS2));
        return left < right ? 1 : left == right ? 0 : -1;
      }

    });

    // previous pipeline give question minimum score so it should appear at the end
    // here we store precision@N in stats2 feature
    for (int i = 0; i < N; i++) {
      Annotation ann = annotations.get(i);
      n = ann.getFeature(Stats.STATS1).equals("1") ? n + 1 : n;
    }
    annotations.get(annotations.size() - 1).addFeature(Stats.STATS2, (((double) n) / N) + "");

    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }
}