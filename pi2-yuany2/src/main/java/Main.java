import java.io.File;

import org.lappsgrid.example.Evaluator;
import org.lappsgrid.example.NGramMaker;
import org.lappsgrid.example.Preprocessor;
import org.lappsgrid.example.QAPipeline;
import org.lappsgrid.example.Scorer;
import org.lappsgrid.example.Tokenizer;

/**
 * the class containing the main entrance of PI3
 * 
 * @author yuany
 *
 */
public class Main {

  /**
   * main entrance of PI3
   * 
   * @param args
   *          ngram input_file_path output_dir
   */
  public static void main(String[] args) {
    // TODO Every annotation must record the following: (1) the name of a component that produces
    // the annotation, and (2) the component's confidence score assigned to the annotation.
    int ngrams = Integer.parseInt(args[0]);
    String inputPath = args[1];
    String outputPath = args[2];
    if (!new File(outputPath).exists())
      new File(outputPath).mkdirs();

    QAPipeline pipe = new QAPipeline(5);
    pipe.addService(new Preprocessor());
    pipe.addService(new Tokenizer());
    pipe.addService(new NGramMaker(ngrams));
    pipe.addService(new Scorer(ngrams));
    pipe.addService(new Evaluator());

    for (String filename : new File(inputPath).list()) {
      if (!filename.matches("q\\d\\d\\d.txt"))
        continue;
      pipe.setPipelineInput(pipe.readInput(inputPath + "/" + filename));
      pipe.runPipeline();
      pipe.writeOutput(outputPath + "/a" + filename.substring(1), pipe.getOutput());
    }

  }

}
