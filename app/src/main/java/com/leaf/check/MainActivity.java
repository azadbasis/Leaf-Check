package com.leaf.check;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.leaf.check.databinding.ActivityMainBinding;
import com.leaf.check.ml.ModelUnquant;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private Boolean mPermissionsChecked = false;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1235;
    private Preview preview;
    private ImageCapture imageCapture;

    private ActivityMainBinding binding;

    private Bitmap bitmap;
    // Registers a photo picker activity launcher in single-select mode.
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);

                    // Convert URI to Bitmap
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imgCamera.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    private void initialCameraX() {
        // Set up the ImageCapture use case
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = new Preview.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .build();
        }

        // Initialize ImageCapture
        imageCapture = new ImageCapture.Builder().build();
        // Bind preview and capture use case (optional)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialCameraX();
        setupListener();
    }

    private void setupListener() {
        binding.btnFindImage.setOnClickListener(v -> {
            mPermissionsChecked = checkPermissions();
            if (!mPermissionsChecked) {
                setPicture();
            } else {
                checkPermissions();
            }

        });
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

                    // Optional: preprocess tensorImage for normalization, etc. This might involve:
                    // tensorImage = TensorImage.fromBitmap(bitmap);
                    // normalize the image if your model requires normalization

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
                    // TODO Handle the exception
                }
            }
        });
    }


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

    private void setPicture() {
        final String[] photoOptions = {getResources().getString(R.string.take_photo), getResources().getString(R.string.choose_gallery)};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getResources().getString(R.string.choose_photo));
        // TODO: The setItems function is used to create a list of content
        builder.setItems(photoOptions, (dialog, which) -> {
            String photoOption = photoOptions[which];
            if (photoOption.equals(getResources().getString(R.string.take_photo))) {
//                CameraxSheetDialog cameraxDialog = new CameraxSheetDialog();
//                cameraxDialog.show(((FragmentActivity) requireActivity()).getSupportFragmentManager(), "CameraSheetDialog");
            }
            if (photoOption.equals(getResources().getString(R.string.choose_gallery))) { //"Choose from Gallery"
                openGallery();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void openGallery() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private boolean checkPermissions() {
        final boolean cameraGranted =
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED;

        List<String> perms = new ArrayList<>();
        if (!cameraGranted) {
            perms.add(android.Manifest.permission.CAMERA);
        }

        // Check for storage access on Android 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Option 1: Request specific media access (recommended)
            if (!hasPermission(android.Manifest.permission.READ_MEDIA_VIDEO) && !hasPermission(android.Manifest.permission.READ_MEDIA_IMAGES)) {
                perms.add(android.Manifest.permission.READ_MEDIA_VIDEO);
                perms.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Option 2: Request broader storage access (use with caution)
            if (!(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!perms.isEmpty()) {
            requestPermissions(perms.toArray(new String[0]), VERIFY_PERMISSIONS_REQUEST);
            return false;
        } else {
            return true;
        }
    }

    // Helper method to check permission (can be reused)
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == VERIFY_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions are granted
                mPermissionsChecked = true;
                setPicture();
            } else {
                // Permissions are denied
                mPermissionsChecked = false;
            }
        }
    }
}