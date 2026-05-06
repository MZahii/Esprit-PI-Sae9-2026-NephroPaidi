package tn.esprit.spring.mlpredictionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tn.esprit.spring.mlpredictionservice.config.MlInferenceProperties;
import tn.esprit.spring.mlpredictionservice.dto.PredictionRequest;
import tn.esprit.spring.mlpredictionservice.dto.PredictionResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class MlPredictionEngine {

    private final ObjectMapper objectMapper;
    private final MlInferenceProperties properties;
    private final Path bundledScriptPath;
    private final Path bundledPreOpModelPath;
    private final Path bundledPostOpModelPath;

    public MlPredictionEngine(ObjectMapper objectMapper, MlInferenceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.bundledScriptPath = extractResource("scripts/predict_with_joblib.py", "predict-with-joblib", ".py");
        this.bundledPreOpModelPath = extractResource("models/surgery_preop_best_model.joblib", "preop-model", ".joblib");
        this.bundledPostOpModelPath = extractResource("models/surgery_postop_best_model.joblib", "postop-model", ".joblib");
    }

    public PredictionResponse predictPreOp(PredictionRequest request) {
        return runPrediction("PRE_OP", request);
    }

    public PredictionResponse predictPostOp(PredictionRequest request) {
        return runPrediction("POST_OP", request);
    }

    private PredictionResponse runPrediction(String phase, PredictionRequest request) {
        Path requestFile = null;
        try {
            requestFile = Files.createTempFile("ml-request-", ".json");
            objectMapper.writeValue(requestFile.toFile(), request);

            Process process = new ProcessBuilder(buildCommand(phase, requestFile)).start();
            boolean finished = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out waiting for Python ML inference after "
                        + properties.getTimeoutSeconds() + "s");
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Python ML inference failed: " + stderr);
            }
            if (stdout == null || stdout.isBlank()) {
                throw new IllegalStateException("Python ML inference returned an empty payload");
            }
            return objectMapper.readValue(stdout, PredictionResponse.class);
        } catch (IOException e) {
            if (shouldRetryWithPython3(e)) {
                return retryWithPython3(phase, request);
            }
            throw new IllegalStateException("Failed to execute ML inference process", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ML inference", e);
        } finally {
            if (requestFile != null) {
                try {
                    Files.deleteIfExists(requestFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private PredictionResponse retryWithPython3(String phase, PredictionRequest request) {
        String original = properties.getPythonCommand();
        properties.setPythonCommand("python3");
        try {
            return runPrediction(phase, request);
        } finally {
            properties.setPythonCommand(original);
        }
    }

    private boolean shouldRetryWithPython3(IOException exception) {
        String message = exception.getMessage();
        return message != null
                && properties.getPythonCommand() != null
                && properties.getPythonCommand().equalsIgnoreCase("python")
                && message.toLowerCase(Locale.ROOT).contains("cannot run program");
    }

    private List<String> buildCommand(String phase, Path requestFile) {
        List<String> command = new ArrayList<>();
        command.add(properties.getPythonCommand());
        command.add(bundledScriptPath.toString());
        command.add(phase);
        command.add(requestFile.toString());
        command.add(resolvePreOpModelPath().toString());
        command.add(resolvePostOpModelPath().toString());
        return command;
    }

    private Path resolvePreOpModelPath() {
        if (properties.getPreOpModelPath() != null && !properties.getPreOpModelPath().isBlank()) {
            return Path.of(properties.getPreOpModelPath());
        }
        return bundledPreOpModelPath;
    }

    private Path resolvePostOpModelPath() {
        if (properties.getPostOpModelPath() != null && !properties.getPostOpModelPath().isBlank()) {
            return Path.of(properties.getPostOpModelPath());
        }
        return bundledPostOpModelPath;
    }

    private Path extractResource(String classpathLocation, String prefix, String suffix) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            Path tempFile = Files.createTempFile(prefix + "-", suffix);
            tempFile.toFile().deleteOnExit();
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract bundled resource: " + classpathLocation, e);
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
}
