package org.lappsgrid.example;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.example.Preprocessor.Stats;

import static org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import java.util.List;
import java.util.Map;

/**
 * Third step in PI2. Takes the view of tokenizer and add a new view annotating the 1-, 2- and
 * 3-gram
 * 
 * @author yuany
 *
 */
public class NGramMaker implements ProcessingService {

  /**
   * storing the metadata
   */
  private String metadata;

  public NGramMaker() {
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
    metadata.setDescription("Produce 1-, 2- and 3-grams of tokens");
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
    produces.addAnnotation(Uri.TOKEN);
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
   * Take the LIF which contains views from previous two steps and annotate the ngrams
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

    // Step #4: Create a new View
    View ngramView = container.newView();
    List<View> views = container.getViews();
    List<Annotation> annotations = views.get(1).getAnnotations(); // annotations of the 2nd view
    int id = 0;
    int length = annotations.size();
    for (int i = 1; i < 4; i++) {
      for (int j = 0; j < length; j++) {
        if (j + i - 1 >= length) {
          continue;
        }
        Annotation head = annotations.get(j);
        Annotation tail = annotations.get(j + i - 1);
        if (head.getFeature(Stats.STATS1).equals(tail.getFeature(Stats.STATS1))) {
          // here start and end denote the indices in 2nd view
          Annotation ann = ngramView.newAnnotation(i + "gram" + id, Uri.TOKEN, j, j + i - 1);
          ann.addFeature(Stats.STATS1, head.getFeature(Stats.STATS1)); // add feature indicating the
                                                                       // answer/question id
          ann.addFeature(Stats.STATS2, i + ""); // add feature indicating which gram it is
          id++;
        }
      }
    }

    // Step #7: Create a DataContainer with the result.
    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }
}