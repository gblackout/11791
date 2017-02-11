package org.lappsgrid.example;

import org.apache.axis.transport.jms.InvokeTimeoutException;
import org.codehaus.groovy.classgen.ReturnAdder;
import org.codehaus.groovy.transform.PackageScopeASTTransformation;
import org.lappsgrid.api.ProcessingService;
import static org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import ch.qos.logback.core.subst.Token;
import jp.go.nict.langrid.commons.transformer.ArrayToArrayTransformer;

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

    View view = container.newView();
    String filepath = container.getText(); // read the filepath
    Scanner scanner;
    String line=null;
    
    try {
      scanner = new Scanner(new FileReader(filepath));
      if (scanner.hasNext()) { // only read one line
        line = scanner.nextLine();        
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    ArrayList<ArrayList<Integer>> triples = find_triples(line);
    
    if (triples != null && triples.size() > 1) {
      int id = 0;
      for (ArrayList<Integer> triple : triples) {
        Annotation a = view.newAnnotation(id == 0 ? "q" : "a" + id, Uri.SENTENCE, triple.get(0),
                triple.get(1));
        a.addFeature(Stats.STATS1, triple.get(2) + "");
        id++;
      }

    }

    view.addContains(Uri.SENTENCE, this.getClass().getName(), "qa_sentences");
    data = new DataContainer(container);
    
    return data.asPrettyJson();
  }

  /**
   * scan through line and create triples <start, end, isCorrect> for question and each; answer
   * (question appears at the first and isCorrect field is left empty)
   * 
   * @param line input string
   * @return the list of the triples found
   */
  private ArrayList<ArrayList<Integer>> find_triples(String line) {
    
    if (line == null) return null;
    
    ArrayList<ArrayList<Integer>> triples = new ArrayList<ArrayList<Integer>>();    
    int ptr = 0;
    int len = line.length();
    ArrayList<Integer> triple = new ArrayList<>();

    assert line.substring(0, 1).equals("Q");
    ptr += 1; // move to first char in Q
    triple.add(ptr); // add start
    while (!line.substring(ptr + 1, next_token(line, ptr)).matches("A\\d")) {
      ptr = next_token(line, ptr);
    }
    triple.add(ptr); // add end
    triple.add(-1); // placeholder
    triples.add(triple);

    // now ptr points to the space before A1
    while (ptr < len) {
      triple = new ArrayList<>();
      ptr = next_token(line, ptr);
      triple.add(ptr + 1);
      int isCorrect = Integer.parseInt(line.substring(ptr + 1, next_token(line, ptr)));
      while (!(ptr + 1 >= line.length())
              && !line.substring(ptr + 1, next_token(line, ptr)).matches("A\\d")) {
        ptr = next_token(line, ptr);
      }
      triple.add(ptr);
      triple.add(isCorrect);
      triples.add(triple);
    }

    return triples;
  }

  private int next_token(String line, int ptr) {

    if (ptr + 1 == line.length())
      return ptr + 1;

    if (line.substring(ptr, ptr + 1).equals(" ")) {
      ptr++;
      if (ptr + 1 == line.length())
        return ptr + 1;
    }

    while (!line.substring(ptr, ptr + 1).equals(" ")) {
      ptr++;
      if (ptr + 1 == line.length())
        return ptr + 1;
    }

    return ptr;
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