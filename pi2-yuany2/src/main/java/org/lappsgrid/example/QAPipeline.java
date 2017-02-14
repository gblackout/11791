package org.lappsgrid.example;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.example.Preprocessor.Stats;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import com.fasterxml.jackson.core.sym.Name;

/**
 * defines how to pipeline webservices with implmented in- and output methods
 * 
 * @author yuany
 *
 */
public class QAPipeline extends Pipeline {

  /**
   * init by indicating the length of the pipeline
   * 
   * @param n
   *          length of the pipeline
   */
  public QAPipeline(int n) {
    super(n);
  }

  @Override
  public
  /**
   * Read a file path and return the first line of text in that file
   * 
   * @param filePath
   *          path to the file
   * @return one-line text read from the file
   */
  String readInput(String filePath) {
    Scanner scanner;
    String line = null;

    try {
      scanner = new Scanner(new FileReader(filePath));
      if (scanner.hasNext()) { // only read one line
        line = scanner.nextLine();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return line;
  }

  @Override
  public void writeOutput(String filePath, String outputJson) {
    try {
      PrintWriter writer = new PrintWriter(filePath);

      Data data = Serializer.parse(outputJson, Data.class);
      final String discriminator = data.getDiscriminator();
      Container container = null;

      // This is a format we don't accept.
      if (!discriminator.equals(Uri.LAPPS)) {
        String message = String.format("Unsupported discriminator type: %s", discriminator);
        writer.close();
        return;
      }

      container = new Container((Map) data.getPayload());
      View qaView = container.getView(4); // get the last view
      List<Annotation> anns = qaView.getAnnotations();

      // print Precision@N
      writer.println(anns.get(anns.size() - 1).getFeature(Stats.STATS2));

      // print answer and score
      for (int i = 0; i < anns.size() - 1; i++) {
        Annotation ann = anns.get(i);
        // make name from a0 -> A1
        String name = ann.getId().substring(0, 1).toUpperCase()
                + Integer.parseInt(ann.getId().substring(1, 2));
        writer.println(name + " " + ann.getFeature(Stats.STATS2));
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void runPipeline() {

    String stageInput = getPipelineInput();
    String intermediateOutput = "";

    for (WebService service : this.getPipelineStages()) {
      intermediateOutput = service.execute(stageInput);
      stageInput = intermediateOutput;
    }

    setOutput(intermediateOutput);
  }

}
