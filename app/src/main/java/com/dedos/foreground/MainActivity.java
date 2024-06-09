package com.dedos.foreground;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    private static final String FILE_NAME = "user_data.json";
    private static final String KEY_EMULATOR_ID = "emulatorId"; // Nuevo valor para el ID del emulador

    private TextInputEditText usernameEditText;
    private TextInputEditText userIdEditText;
    private TextView emulatorId;

    private Switch autoConnectSwitch;
    private Button connectButton;
    private Button disconnectButton;
    private ProgressBar progressBar;
    private TextView estadoText;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if(!RootUtils.executeRootCommand("service call notification 1")){
            Toast.makeText(this, "Necesitas root para usar esta app", Toast.LENGTH_SHORT).show();
            finish();
        }



        try {
            usernameEditText = findViewById(R.id.textInputEditText);
            userIdEditText = findViewById(R.id.textInputEditText3);
            autoConnectSwitch = findViewById(R.id.switch2);
            connectButton = findViewById(R.id.button3);
            disconnectButton = findViewById(R.id.button4);
            progressBar = findViewById(R.id.progressBar);
            estadoText = findViewById(R.id.textView5);
            emulatorId = findViewById(R.id.textView4);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Alguna de las variables no se encontraron en el layout", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        requestIgnoreBatteryOptimizations();
        while (!isIgnoringBatteryOptimizations(this, getPackageName())){
            checkBatteryOptimizationPermission();
            SystemClock.sleep(5000);
        }
        showProgressBarWithDelay(3000);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    saveDataToSharedPreferences();
                    startForegroundService();
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopForegroundService();
            }
        });

        autoConnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (validateInputs()) {
                    saveDataToSharedPreferences();
                    startForegroundService();
                } else {
                    autoConnectSwitch.setChecked(false);
                }
            } else {
                saveDataToSharedPreferences();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadDataFromSharedPreferences();
        checkAutoConnectAndStartService();
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showProgressBarWithDelay(int delayMillis) {
        progressBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
        }, delayMillis);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkAutoConnectAndStartService() {
        if (autoConnectSwitch.isChecked()) {
            if (validateInputs()) {
                startForegroundService();
            } else {
                autoConnectSwitch.setChecked(false);
            }
        } else {
            setStatustext(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService() {
        saveDataToSharedPreferences();
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        startForegroundService(serviceIntent);
        Toast.makeText(this, "Servicio Iniciado", Toast.LENGTH_SHORT).show();
        setStatustext(true);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Servicio Detenido", Toast.LENGTH_SHORT).show();
        setStatustext(false);
    }

    private boolean validateInputs() {
        try {
            String username = usernameEditText.getText().toString();
            String userId = userIdEditText.getText().toString();

            if (username.isEmpty() || userId.isEmpty()) {
                Toast.makeText(this, "Username o User Id estan vacios", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!isNumeric(userId)) {
                Toast.makeText(this, "User Id debe ser un numero", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: EditText es nulo", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveDataToSharedPreferences() {
        String username = usernameEditText.getText().toString();
        String userId = userIdEditText.getText().toString();
        boolean autoConnect = autoConnectSwitch.isChecked();

        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("userId", userId);
        editor.putBoolean("autoConnect", autoConnect);
        if(!sharedPreferences.contains("emularoId")){
            String emulidlol = generateEmulatorId();
            editor.putString("emulatorId", emulidlol);
            emulatorId.setText(emulidlol);
        }

        editor.apply();
    }

    private void loadDataFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        String userId = sharedPreferences.getString("userId", "");
        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
        String emulatorIdValue = sharedPreferences.getString("emulatorId", "");

        usernameEditText.setText(username);
        userIdEditText.setText(userId);
        autoConnectSwitch.setChecked(autoConnect);
        emulatorId.setText(emulatorIdValue);

        Toast.makeText(this, "Datos cargados", Toast.LENGTH_SHORT).show();
    }

    public void setStatustext(boolean estado) {
        if (estado) {
            estadoText.setText("Estado: Activado");
        } else {
            estadoText.setText("Estado: Desactivado");
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            if (!isIgnoringBatteryOptimizations(this, packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }


    private boolean isIgnoringBatteryOptimizations(Context context, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return false;
    }



    private void checkEmulatorId() {
        if (emulatorId.getText().toString().isEmpty()) {
            String emulatorIdValue = generateEmulatorId();
            emulatorId.setText(emulatorIdValue);
        }
    }

    private void checkBatteryOptimizationPermission() {
        if (!isIgnoringBatteryOptimizations(this, getPackageName())) {
            Toast.makeText(this, "Por favor, habilite los permisos de optimización de batería para usar esta aplicación", Toast.LENGTH_LONG).show();
        }
    }




    private String generateEmulatorId() {
        return UUID.randomUUID().toString();
    }
}
