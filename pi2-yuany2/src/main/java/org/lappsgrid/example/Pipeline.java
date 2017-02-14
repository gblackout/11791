package org.lappsgrid.example;

import java.util.ArrayList;
import java.util.List;

import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;

/**
 * defines how to pipeline webservices
 * 
 * @author yuany
 *
 */
public abstract class Pipeline {

  /**
   * list to store all components
   */
  private List<WebService> Stages;

  private Data input;

  private String output;

  /**
   * init by indicating the length of the pipeline
   * 
   * @param n
   *          length of the pipeline
   */
  public Pipeline(int n) {
    this.Stages = new ArrayList<>(n);
  }

  /**
   * @return return stages
   */
  public List<WebService> getPipelineStages() {
    return Stages;
  }

  /**
   * @param newService
   *          the new service to be added in
   */
  public void addService(WebService newService) {
    Stages.add(newService);
  }

  /**
   * @param docText
   *          text to be set as input
   */
  public void setPipelineInput(String docText) {
    input = new Data<>(Uri.TEXT, docText);
  }

  /**
   * @return jason format of input
   */
  public String getPipelineInput() {
    return input.asJson();
  }

  /**
   * @param jsonText
   *          string to be set as output
   */
  protected void setOutput(String jsonText) {
    output = jsonText;
  }

  /**
   * @return output string
   */
  public String getOutput() {
    return output;
  }

  /**
   * read text line given filepath
   * 
   * @param filePath
   *          path to the file
   * @return line read from the file
   */
  abstract String readInput(String filePath);

  /**
   * write jason string to the filepath with certain format
   * 
   * @param filePath
   *          path where the output file will be created
   * @param outputJson
   *          content to be written to
   */
  abstract void writeOutput(String filePath, String outputJson);

  /**
   * run the pipeline stage by stage
   */
  abstract void runPipeline();

}
