package tn.esprit.spring.mlpredictionservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ml.inference")
public class MlInferenceProperties {

    private String pythonCommand = "python";
    private String preOpModelPath;
    private String postOpModelPath;
    private int timeoutSeconds = 60;

    public String getPythonCommand() {
        return pythonCommand;
    }

    public void setPythonCommand(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    public String getPreOpModelPath() {
        return preOpModelPath;
    }

    public void setPreOpModelPath(String preOpModelPath) {
        this.preOpModelPath = preOpModelPath;
    }

    public String getPostOpModelPath() {
        return postOpModelPath;
    }

    public void setPostOpModelPath(String postOpModelPath) {
        this.postOpModelPath = postOpModelPath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
