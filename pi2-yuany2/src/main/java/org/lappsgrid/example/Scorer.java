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

import groovyjarjarasm.asm.tree.IntInsnNode;

// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

  private int ngrams;

  public Scorer(int ngrams) {
    metadata = generateMetadata();
    this.ngrams = ngrams;
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
    // get the token view
    View tokenView = container.getView(1);
    // get the qa view
    View qaView = container.getView(0);
    String text = container.getText();

    for (Annotation ann : qaView.getAnnotations()) {
      ann.addFeature(Stats.STATS2, this.getScore(ann.getId(), ngramView, tokenView, text) + "");
      ann.addFeature(Stats.NAME, this.getClass().getName());
      ann.addFeature(Stats.CONFSCORE, "1");
    }

    // a hack by copying the view 0 to a new view, since in-place modification is somehow not
    // working
    container.addView(qaView);

    // Step #7: Create a DataContainer with the result.
    data = new DataContainer(container);
    // Step #8: Serialize the data object and return the JSON.
    return data.asPrettyJson();
  }

  /**
   * function that returns score of an answer given ngram; currently it's just a place holder need
   * to distinguish if it's question; if it is then return negative score
   * 
   * @param id
   *          String id of the answer to be scored
   * @param ngramView
   *          view produced by NGramMaker
   * @return the score of the answer
   */
  private double getScore(String id, View ngramView, View tokenView, String text) {

    if (id.equals("q"))
      return -1.0;

    List<Annotation> ngramanns = ngramView.getAnnotations();
    List<Annotation> tokenanns = tokenView.getAnnotations();

    HashSet<String> target = new HashSet<>();
    HashSet<String> answer = new HashSet<>();
    HashSet<String> union = new HashSet<>();

    target = prepNGram("q", ngramanns, tokenanns, text);
    answer = prepNGram(id, ngramanns, tokenanns, text);
    union.addAll(target);
    union.addAll(answer);

    Iterator iter = target.iterator();
    double n = 0;
    while (iter.hasNext()) {
      String gram = (String) iter.next();
      if (answer.contains(gram))
        n++;
    }

    return n / union.size();
  }

  /**
   * This prepares the ngram hashset for a given id which can be -1 (indicating the question) and
   * 0-n (which is the answer id)
   * 
   * @param id
   *          the id of the question/answer from which we collect the ngram
   * @param anns
   *          list of annotations in ngram view
   * @param tokenanns
   *          list of annotations in token view
   * @param text
   *          original text
   * @return hashset of the ngrams
   */
  private HashSet<String> prepNGram(String id, List<Annotation> anns, List<Annotation> tokenanns,
          String text) {
    HashSet<String> set = new HashSet<>();

    for (Annotation ann : anns) {
      String gram_id = ann.getFeature(Stats.STATS1);

      if (gram_id.equals("-1"))
        gram_id = "q";
      else
        gram_id = "a" + gram_id;

      if (gram_id.equals(id)) {
        String tmp = "";
        for (int i = ann.getStart().intValue(); i <= ann.getEnd(); i++) {
          Annotation tokenann = tokenanns.get(i);
          if (tmp.length() > 0)
            tmp = tmp + " ";
          tmp = tmp + text.substring(tokenann.getStart().intValue(), tokenann.getEnd().intValue())
                  .replaceAll("\\W", "");
        }
        set.add(tmp);
      }
    }

    return set;
  }

}