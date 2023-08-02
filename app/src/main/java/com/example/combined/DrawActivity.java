
package com.example.combined;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DrawActivity extends Activity {

    private DrawOnImageView drawOnImageView;
    private boolean isDrawingEnabled = false;
    private int currentColor = Color.BLACK; // Default color is black

    private static final int PIC_CROP = 2;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CODE_SAVE_IMAGE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        String imageUriString = getIntent().getStringExtra("imageUri");
        Uri imageUri = Uri.parse(imageUriString);
        Bitmap imageBitmap = BitmapFactory.decodeFile(imageUri.getPath());

        drawOnImageView = findViewById(R.id.drawOnImageView);
        drawOnImageView.setImageBitmap(imageBitmap);
        



        // Set fixed dimensions for the captured image (353dp width, 590dp height)
        int desiredWidth = dpToPx(375);
        int desiredHeight = dpToPx(599);

        // Scale the captured image to match the desired dimensions
        Bitmap scaledImageBitmap = Bitmap.createScaledBitmap(imageBitmap, desiredWidth, desiredHeight, true);
        drawOnImageView.setImageBitmap(scaledImageBitmap);

        ImageButton buttonDraw = findViewById(R.id.buttonDraw);
        buttonDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDrawingEnabled = !isDrawingEnabled; // Toggle the drawing state
                if (isDrawingEnabled) {
                    drawOnImageView.enableDrawing(); // Enable drawing
//                    buttonDraw.setText("Disable Drawing"); // Update button text
                } else {
                    drawOnImageView.disableDrawing(); // Disable drawing
//                    buttonDraw.setText("Enable Drawing"); // Update button text
                }
            }
        });

        ImageButton buttonColor = findViewById(R.id.buttonColor);
        buttonColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorOptions();
            }
        });

        ImageButton buttonCrop = findViewById(R.id.buttonCrop);
        buttonCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImage();
            }
        });

        ImageButton buttonUndo = findViewById(R.id.buttonUndo);
        buttonUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawOnImageView.undo(); // Call the undo method in DrawOnImageView
            }
        });
        ImageButton buttonRedo = findViewById(R.id.buttonRedo);
        buttonRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawOnImageView.redo(); // Call the undo method in DrawOnImageView
            }
        });


    }

    private void showColorOptions() {
        // Inflate the custom color options layout
        View colorOptionsView = getLayoutInflater().inflate(R.layout.custom_colour_selector, null);

        // Find color option ImageViews and set click listeners
        ImageView colorBlack = colorOptionsView.findViewById(R.id.black);
        ImageView colorRed = colorOptionsView.findViewById(R.id.red);
        ImageView colorBlue = colorOptionsView.findViewById(R.id.blue);

        colorBlack.setOnClickListener(v -> changeDrawingColor(Color.BLACK));
        colorRed.setOnClickListener(v -> changeDrawingColor(Color.RED));
        colorBlue.setOnClickListener(v -> changeDrawingColor(Color.BLUE));

        // Create and show a custom AlertDialog with the color options layout
        AlertDialog colorOptionsDialog = new AlertDialog.Builder(this)
                .setView(colorOptionsView)
                .create();

        colorOptionsDialog.show();
    }
    private void changeDrawingColor(int color) {
        currentColor = color;
        drawOnImageView.setDrawingColor(currentColor);
    }



    private void cropImage() {
        // Check for the WRITE_EXTERNAL_STORAGE permission before cropping
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if it's not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            // If the permission is already granted, proceed with cropping
            // Get the current drawn image bitmap from the drawOnImageView
            Bitmap currentBitmap = drawOnImageView.getBitmap();

            // Get the dimensions of the DrawOnImageView
            int desiredWidth = drawOnImageView.getWidth();
            int desiredHeight = drawOnImageView.getHeight();
            Bitmap mergedBitmap = drawOnImageView.getMergedBitmap();


            // Create an intent to perform image cropping
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(getImageUri(mergedBitmap), "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", desiredWidth);
            cropIntent.putExtra("aspectY", desiredHeight);
            cropIntent.putExtra("outputX", desiredWidth); // Set the desired output width
            cropIntent.putExtra("outputY", desiredHeight); // Set the desired output height
            cropIntent.putExtra("scale", true);
            cropIntent.putExtra("return-data", false); // Change to "true" to get the cropped bitmap in onActivityResult

            // Grant temporary read and write permission to the content URI for the crop operation
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // Start the cropping activity
            startActivityForResult(cropIntent, PIC_CROP);
        }
    }

    // Save the bitmap to a temporary file and return its Uri using FileProvider
    private Uri getImageUri(Bitmap bitmap) {
        String title = "Image"; // Set a valid title without spaces
        String description = "Image with Drawings"; // Set a description (optional)
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                title,
                description
        );

        return Uri.parse(savedImageURL);
    }


    // Utility method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with cropping
                cropImage();
            } else {
                // Permission denied, show a toast or handle accordingly
                Toast.makeText(this, "Permission denied. Cannot crop image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PIC_CROP && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri croppedUri = data.getData();
                try {
                    Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);

                    // Set the cropped bitmap as the new image in the DrawOnImageView
                    drawOnImageView.setImageBitmap(croppedBitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CODE_SAVE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageWithDrawings() {
        Bitmap editedBitmap = drawOnImageView.getBitmap(); // Get the edited image with drawings

        // Implement your save logic here using the editedBitmap
        // For example, you can save it to the device's gallery or a custom file directory.
        // You may use the imageUri obtained after saving the image for further actions.
        Uri imageUri = saveBitmapToFile(editedBitmap, this);

        if (imageUri != null) {
            // Show the edited image with drawings in the gallery or an image viewer app
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(imageUri, "image/*");
            startActivity(intent);
        } else {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri saveBitmapToFile(Bitmap bitmap, Context context) {
        try {
            File tempFile = File.createTempFile("temp_image", ".jpg", context.getCacheDir());
            FileOutputStream outStream = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}


