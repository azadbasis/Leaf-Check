
```markdown
# Plant Disease Detection Application

This project is an Android application that uses Machine Learning to detect plant diseases from images of plant leaves. The application is built using Java and TensorFlow Lite.

## Features

- **Image Classification**: Detects various plant diseases from leaf images.
- **User-Friendly Interface**: Easy-to-use interface for capturing or uploading leaf images.
- **Real-Time Predictions**: Provides instant disease detection results.
- **Offline Functionality**: Works without an internet connection once the model is downloaded.

## Getting Started

### Prerequisites

- Android Studio
- Java Development Kit (JDK)
- TensorFlow Lite Model
- Plant Disease Dataset (e.g., PlantVillage)

### Installation

1. **Clone the Repository**:
    ```sh
    git clone https://github.com/yourusername/plant-disease-detection.git
    cd plant-disease-detection
    ```

2. **Open in Android Studio**:
    - Open Android Studio and select `Open an existing Android Studio project`.
    - Navigate to the cloned repository and open it.

3. **Add TensorFlow Lite Model**:
    - Download the TensorFlow Lite model and place it in the `assets` directory of your project.
    - Ensure the model file is named correctly (e.g., `model.tflite`).

4. **Add Labels File**:
    - Create a `labels.txt` file in the `assets` directory.
    - Add the labels corresponding to the classes your model can predict, one per line.

### Usage

1. **Capture or Upload Image**:
    - Use the app to capture a photo of a plant leaf or upload an existing image from your gallery.

2. **Run Prediction**:
    - Click the `Predict` button to run the model and get the disease prediction.

3. **View Results**:
    - The app will display the predicted disease along with the confidence score.

### Code Overview

- **MainActivity.java**: The main activity that handles image capture/upload and runs the prediction.
- **ModelUnquant.java**: The TensorFlow Lite model class for running inference.
- **Utils.java**: Utility functions for image processing and loading labels.

### Example Code

```java
binding.btnPredict.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        try {
            ModelUnquant model = ModelUnquant.newInstance(MainActivity.this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

            Bitmap scaledBitmap  = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(scaledBitmap);

            inputFeature0.loadBuffer(tensorImage.getBuffer());

            // Runs model inference and gets result.
            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Load labels from the labels file
            List<String> labels = loadLabels("labels.txt");

            // Get the predicted label
            int maxIndex = getMaxIndex(outputFeature0.getFloatArray());
            String predictedLabel = labels.get(maxIndex);

            // Display the predicted label
            Toast.makeText(MainActivity.this, "Predicted: " + predictedLabel, Toast.LENGTH_SHORT).show();

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }
});

// Helper method to load labels from a file
private List<String> loadLabels(String fileName) throws IOException {
    List<String> labels = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
    String line;
    while ((line = reader.readLine()) != null) {
        labels.add(line);
    }
    reader.close();
    return labels;
}

// Helper method to find the index of the maximum value in the output array
private int getMaxIndex(float[] probabilities) {
    int maxIndex = 0;
    float maxProb = probabilities[0];
    for (int i = 1; i < probabilities.length; i++) {
        if (probabilities[i] > maxProb) {
            maxProb = probabilities[i];
            maxIndex = i;
        }
    }
    return maxIndex;
}
```

### Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Acknowledgments

- Custom Dataset
- TensorFlow Lite
- Android Studio



