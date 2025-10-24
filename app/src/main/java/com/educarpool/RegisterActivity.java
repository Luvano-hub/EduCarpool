package com.educarpool;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etStudentId, etEmail, etPassword, etPhone, etHomeAddress;
    private RadioGroup rgRole;
    private RadioButton rbDriver, rbPassenger;
    private LinearLayout llDriverFields;
    private Spinner spDetourRange;
    private Button btnRegister, btnLicense, btnCarRegistration;
    private TextView tvLogin;
    private ImageView ivLicensePreview, ivCarRegPreview;

    private AuthRepository authRepository;

    private String licenseBase64;
    private String carRegBase64;
    private String currentPhotoPath;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_LICENSE_IMAGE = 100;
    private static final int REQUEST_CAR_REG_IMAGE = 101;

    private int currentImageRequestCode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();
        initializeViews();
        setupSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        etName = findViewById(R.id.et_name);
        etStudentId = findViewById(R.id.et_student_id);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPhone = findViewById(R.id.et_phone);
        etHomeAddress = findViewById(R.id.et_home_address);

        rgRole = findViewById(R.id.rg_role);
        rbDriver = findViewById(R.id.rb_driver);
        rbPassenger = findViewById(R.id.rb_passenger);

        llDriverFields = findViewById(R.id.ll_driver_fields);
        spDetourRange = findViewById(R.id.sp_detour_range);

        btnRegister = findViewById(R.id.btn_register);
        btnLicense = findViewById(R.id.btn_license);
        btnCarRegistration = findViewById(R.id.btn_car_registration);

        ivLicensePreview = findViewById(R.id.iv_license_preview);
        ivCarRegPreview = findViewById(R.id.iv_car_reg_preview);

        tvLogin = findViewById(R.id.tv_login);
    }

    private void setupSpinner() {
        String[] detourRanges = new String[]{"5", "10", "15", "20", "25"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, detourRanges);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDetourRange.setAdapter(adapter);
    }

    private void setupClickListeners() {
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_driver) {
                llDriverFields.setVisibility(LinearLayout.VISIBLE);
            } else {
                llDriverFields.setVisibility(LinearLayout.GONE);
                licenseBase64 = null;
                carRegBase64 = null;
                ivLicensePreview.setImageDrawable(null);
                ivCarRegPreview.setImageDrawable(null);
                btnLicense.setText("Upload License");
                btnCarRegistration.setText("Upload Car Registration");
            }
        });

        btnLicense.setOnClickListener(v -> {
            currentImageRequestCode = REQUEST_LICENSE_IMAGE;
            showImageSourceDialog();
        });

        btnCarRegistration.setOnClickListener(v -> {
            currentImageRequestCode = REQUEST_CAR_REG_IMAGE;
            showImageSourceDialog();
        });

        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take Photo
                dispatchTakePictureIntent();
            } else if (which == 1) {
                // Choose from Gallery
                dispatchPickPictureIntent();
            }
            // Cancel is handled by default
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchPickPictureIntent() {
        Intent pickPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPictureIntent.setType("image/*");
        startActivityForResult(pickPictureIntent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            File imageFile = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Image captured from camera
                imageFile = new File(currentPhotoPath);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Image picked from gallery
                Uri imageUri = data.getData();
                try {
                    imageFile = getFileFromUri(imageUri);
                } catch (IOException e) {
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (imageFile != null) {
                try {
                    // Convert image to Base64
                    String base64Image = encodeImageToBase64(imageFile);

                    if (currentImageRequestCode == REQUEST_LICENSE_IMAGE) {
                        licenseBase64 = base64Image;
                        ivLicensePreview.setImageBitmap(decodeBase64ToBitmap(base64Image));
                        btnLicense.setText("License Selected ✓");
                        Toast.makeText(this, "License image selected", Toast.LENGTH_SHORT).show();
                    } else if (currentImageRequestCode == REQUEST_CAR_REG_IMAGE) {
                        carRegBase64 = base64Image;
                        ivCarRegPreview.setImageBitmap(decodeBase64ToBitmap(base64Image));
                        btnCarRegistration.setText("Car Registration Selected ✓");
                        Toast.makeText(this, "Car registration image selected", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Convert image file to Base64 string
    private String encodeImageToBase64(File imageFile) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(imageFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        fileInputStream.close();
        byteArrayOutputStream.close();

        return base64Image;
    }

    // Convert Base64 string to Bitmap for preview
    private Bitmap decodeBase64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    // Helper method to get a File from Uri (works for both file and content URIs)
    private File getFileFromUri(Uri uri) throws IOException {
        if (uri == null) {
            return null;
        }

        String fileName = getFileName(uri);
        File file = new File(getCacheDir(), fileName);

        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {

            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        return file;
    }

    // Helper method to get file name from Uri
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        // Ensure the file has a proper extension
        if (!result.toLowerCase().endsWith(".jpg") && !result.toLowerCase().endsWith(".jpeg") && !result.toLowerCase().endsWith(".png")) {
            result += ".jpg";
        }

        return result;
    }

    private boolean validateInputs() {
        String name = etName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etHomeAddress.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return false;
        }

        if (studentId.isEmpty()) {
            etStudentId.setError("Student ID is required");
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }

        if (!email.endsWith("@vossie.net")) {
            etEmail.setError("Please use your @vossie.net email");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            return false;
        }

        if (address.isEmpty()) {
            etHomeAddress.setError("Home address is required");
            return false;
        }

        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Driver-specific validations
        if (rbDriver.isChecked()) {
            if (licenseBase64 == null) {
                Toast.makeText(this, "Please upload driver's license", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (carRegBase64 == null) {
                Toast.makeText(this, "Please upload car registration", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etHomeAddress.getText().toString().trim();
        String role = rbDriver.isChecked() ? "driver" : "passenger";
        Integer detourRange = rbDriver.isChecked() ?
                Integer.parseInt(spDetourRange.getSelectedItem().toString()) : null;

        // Create user object
        User user = new User(email, password, name, studentId, phone, address, role);
        if (detourRange != null) {
            user.setDetourRange(detourRange);
        }

        // Add Base64 images for drivers
        if (rbDriver.isChecked()) {
            user.setLicenseUrl(licenseBase64);
            user.setCarRegUrl(carRegBase64);
        }

        // Show loading
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        // Register user directly in database
        registerUserInDatabase(user);
    }

    private void registerUserInDatabase(User user) {
        Log.d("RegisterActivity", "Registering user in database: " + user.getEmail());

        authRepository.registerUser(user, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Toast.makeText(RegisterActivity.this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show();

                    // Redirect to login page
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}