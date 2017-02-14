import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.example.Evaluator;
import org.lappsgrid.example.NGramMaker;
import org.lappsgrid.example.Preprocessor;
import org.lappsgrid.example.Preprocessor.Stats;
import org.lappsgrid.example.Scorer;
import org.lappsgrid.example.Tokenizer;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;

/**
 * A simple test on PI3
 * 
 * @author yuany
 *
 */
public class Testall {

  /**
   * entrance of the test
   * 
   * @param args nothing needs to be input
   */
  public static void main(String[] args) {
    String path = "src/main/java/org/lappsgrid/example/q001.txt";
    Preprocessor pp = new Preprocessor();
    String output = pp.execute(new Data<>(Uri.TEXT, path).asPrettyJson());

    Container container;
    List<Annotation> anns;
    String text = null;

    // // test of preprocessor
    // System.out.println(output);
    container = new Container((Map) Serializer.parse(output, Data.class).getPayload());
    // anns = container.getView(0).getAnnotations();
    text = container.getText();
    // for (Annotation ann:anns) {
    // System.out.println(text.substring(ann.getStart().intValue(), ann.getEnd().intValue()));
    // }

    Tokenizer tk = new Tokenizer();
    output = tk.execute(output);

    // // test of tokenizer
    // System.out.println(output);
    // container = new Container((Map) Serializer.parse(output, Data.class).getPayload());
    // anns = container.getView(1).getAnnotations();
    // for (Annotation ann:anns) {
    // System.out.println(text.substring(ann.getStart().intValue(), ann.getEnd().intValue())+"
    // id:"+ann.getId()+" which:"+ann.getFeature(Stats.STATS1));
    // }

    NGramMaker ng = new NGramMaker(3);
    output = ng.execute(output);

    // // test of NGramMaker
    // System.out.println(output);
    // container = new Container((Map) Serializer.parse(output, Data.class).getPayload());
    // anns = container.getView(2).getAnnotations();
    // for (Annotation ann:anns) {
    // System.out.println(ann.getStart().intValue()+" "+ann.getEnd().intValue()+" "+"
    // id:"+ann.getId()+" which:"+ann.getFeature(Stats.STATS1)+"
    // ngram:"+ann.getFeature(Stats.STATS2));
    // }

    Scorer sc = new Scorer(3);
    output = sc.execute(output);

    // // test for Scorer
    // System.out.println(output);
    // container = new Container((Map) Serializer.parse(output, Data.class).getPayload());
    // anns = container.getView(3).getAnnotations();
    // for (Annotation ann:anns) {
    // System.out.println(ann.getFeature(Stats.STATS2));
    // }

    Evaluator ev = new Evaluator();
    output = ev.execute(output);

    // container = new Container((Map) Serializer.parse(output, Data.class).getPayload());
    // anns = container.getView(4).getAnnotations();
    // for (Annotation ann:anns) {
    // System.out.println("id:"+ann.getId()+" isCorrect:"+ann.getFeature(Stats.STATS1)+"
    // scores:"+ann.getFeature(Stats.STATS2));
    // }
  }
}
