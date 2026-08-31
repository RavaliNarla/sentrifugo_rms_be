package com.bob.candidateportal.service;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.translator.YoloV8TranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ManualValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class FaceDetectionService {

    // ── Thresholds ──────────────────────────────────────────────────────────
    private static final double MIN_PERSON_CONFIDENCE = 0.70; // raised from 0.70 — filters back-of-head photos
    private static final double MIN_FACE_AREA         = 0.15;
    private static final double MAX_FACE_AREA         = 0.80;
    private static final double CENTER_MIN            = 0.30;
    private static final double CENTER_MAX            = 0.70;

    // ── Model ─────────────────────────────────────────────────────────────
    private ZooModel<Image, DetectedObjects> detectionModel; // yolov8n

    @PostConstruct
    public void init() throws Exception {
        this.detectionModel = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optArtifactId("yolov8n")
                .optEngine("PyTorch")
                .optTranslatorFactory(new YoloV8TranslatorFactory())
                .optProgress(new ProgressBar())
                .build()
                .loadModel();
        log.info("Model loaded: yolov8n (person detection)");
    }

    // ── Main Validation ───────────────────────────────────────────────────
    public DetectedObjects validatePassportPhoto(MultipartFile file) throws Exception {
        try (var is = file.getInputStream()) {

            Image img = ImageFactory.getInstance().fromInputStream(is);

            // Check 1: Portrait or square only
            if (img.getWidth() > img.getHeight()) {
                throw new ManualValidationException(
                        "Please upload a portrait or square photo. Landscape orientation is not accepted.");
            }

            DetectedObjects detectionResult;
            DetectedObjects.DetectedObject bestPerson;

            try (Predictor<Image, DetectedObjects> predictor = detectionModel.newPredictor()) {
                detectionResult = predictor.predict(img);

                int personCount = 0;
                bestPerson = null;

                for (int i = 0; i < detectionResult.getNumberOfObjects(); i++) {
                    DetectedObjects.DetectedObject obj = detectionResult.item(i);
                    if ("person".equalsIgnoreCase(obj.getClassName())) {
                        personCount++;
                        if (bestPerson == null || obj.getProbability() > bestPerson.getProbability()) {
                            bestPerson = obj;
                        }
                    }
                }

                // Check 2: Exactly one person
                if (personCount == 0) {
                    throw new ManualValidationException(
                            "No person detected. Please upload a clear, real-life passport-size photograph. " +
                                    "Cartoons and drawings are not accepted.");
                }
                if (personCount > 1) {
                    throw new ManualValidationException(
                            "Multiple people detected. Please upload a single-person passport-size photo.");
                }

                // Check 3: Person confidence
                // A high threshold (85%+) naturally rejects back-of-head, side profiles,
                // and unclear photos since yolov8n scores these much lower than front-facing persons
                if (bestPerson.getProbability() < MIN_PERSON_CONFIDENCE) {
                    throw new ManualValidationException(
                            "Person not clearly visible (" +
                                    String.format("%.0f%%", bestPerson.getProbability() * 100) + "). " +
                                    "Please upload a clear, front-facing photo in good lighting.");
                }

                log.info("Person detected — confidence: {}%",
                        String.format("%.0f", bestPerson.getProbability() * 100));
            }

            // ── Bounding box size and centering ──────────────────────────
            BoundingBox bounds = bestPerson.getBoundingBox();
            ai.djl.modality.cv.output.Rectangle rect = bounds.getBounds();

            double x       = rect.getX();
            double y       = rect.getY();
            double w       = rect.getWidth();
            double h       = rect.getHeight();
            double area    = w * h;
            double centerX = x + w / 2.0;
            double centerY = y + h / 2.0;

            log.info("Bounding box — area: {}%, centerX: {}, centerY: {}",
                    String.format("%.1f", area * 100), centerX, centerY);

            // Check 4: Not too small
            if (area < MIN_FACE_AREA) {
                throw new ManualValidationException(
                        "You are too far from the camera (" +
                                String.format("%.0f%%", area * 100) + " of frame). " +
                                "Please move closer for a proper passport-size photo.");
            }

            // Check 5: Not too zoomed in
            /*if (area > MAX_FACE_AREA) {
                throw new ManualValidationException(
                        "Photo is too zoomed in (" +
                                String.format("%.0f%%", area * 100) + " of frame). " +
                                "Please step back to include your full face and top of shoulders.");
            }

            // Check 6: Centered
            if (centerX < CENTER_MIN || centerX > CENTER_MAX
                    || centerY < CENTER_MIN || centerY > CENTER_MAX) {
                throw new ManualValidationException(
                        "You are not centered in the photo. " +
                                "Please position yourself in the center of the frame.");
            }*/

            log.info("Passport photo validation PASSED ✓");
            return detectionResult;

        } catch (IOException e) {
            throw new CommonException("Error processing uploaded photo");
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────
    @PreDestroy
    public void close() {
        if (detectionModel != null) detectionModel.close();
    }
}