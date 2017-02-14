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
import org.lappsgrid.vocabulary.Features;
// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;

import java.util.List;
import java.util.Map;

/**
 * Second step in PI2. Takes the first view and tokenize the word in each question/answer
 * 
 * @author yuany
 *
 */
public class Tokenizer implements ProcessingService {

  /**
   * storing the metadata
   */
  private String metadata;

  public Tokenizer() {
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
    metadata.setDescription("Tokenize question and answer into tokens");
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
    produces.addAnnotation(Uri.TOKEN); // Tokens (contents)
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
   * Take LIF which contains the view from preprocessor, and add new view annotating the token
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
    View view = container.newView();
    List<Annotation> annotations = container.getView(0).getAnnotations(); // annotations of the 1st
                                                                          // view
    String text = container.getText();

    int id = 0;
    int start = 0;
    for (Annotation ann : annotations) {
      boolean isAnswer = ann.getId().contains("a");
      int answerId = isAnswer ? Integer.parseInt(ann.getId().substring(1)) : -1;
      String[] words = text.substring(ann.getStart().intValue(), ann.getEnd().intValue()).trim()
              .split("\\s+");
      for (String word : words) {
        start = text.indexOf(word, start);
        if (start < 0) {
          return new Data<String>(Uri.ERROR, "Unable to match word: " + word).asJson();
        }
        int end = start + word.length();
        Annotation a = view.newAnnotation((isAnswer ? "atok" : "qtok") + id, Uri.TOKEN, start, end);
        a.addFeature(Features.Token.WORD, word);
        a.addFeature(Stats.STATS1, answerId + ""); // add feature indicating which answer/question
                                                   // it's in
        a.addFeature(Stats.NAME, this.getClass().getName());
        a.addFeature(Stats.CONFSCORE, "1");
        id++;
      }
    }

    // Step #6: Update the view's metadata. Each view contains metadata about the
    // annotations it contains, in particular the name of the tool that produced the
    // annotations.
    view.addContains(Uri.TOKEN, this.getClass().getName(), "whitespace");
    // Step #7: Create a DataContainer with the result.
    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }
}