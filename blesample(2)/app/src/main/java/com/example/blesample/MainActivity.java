package com.example.blesample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Button chuyen;
    Button readDataButton;
    Button stopReadButton;
    TextView status;

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    EditText deviceIndexInput;
    Button connectToDevice;
    Button disconnectDevice;
    BluetoothGatt bluetoothGatt;

    private UUID serviceUUID;
    private UUID characteristicUUID;
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<>();

    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;

    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private Handler readHandler = new Handler();
    private Runnable readRunnable;
    private boolean isReading = false;

    @SuppressLint({"NewApi", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chuyen = findViewById(R.id.chuyen);
        chuyen.setOnClickListener(view -> {
            Intent myintent = new Intent(MainActivity.this, ChildActivity.class);
            startActivity(myintent);
        });

        status = findViewById(R.id.status);
        peripheralTextView = findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        deviceIndexInput = findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        connectToDevice = findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener(v -> connectToDeviceSelected());

        disconnectDevice = findViewById(R.id.DisconnectButton);
        disconnectDevice.setVisibility(View.INVISIBLE);
        disconnectDevice.setOnClickListener(v -> disconnectDeviceSelected());

        startScanningButton = findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> openAppSettings());
                builder.show();
            } else {
                startScanning();
            }
        });

        stopScanningButton = findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(v -> stopScanning());
        stopScanningButton.setVisibility(View.INVISIBLE);

        readDataButton = findViewById(R.id.ReadButton);
        stopReadButton = findViewById(R.id.StopReadButton);
        stopReadButton.setVisibility(View.INVISIBLE);

        readDataButton.setOnClickListener(v -> startReadingData());
        stopReadButton.setOnClickListener(v -> stopReadingData());

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @SuppressLint("NewApi")
    private ScanCallback leScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            peripheralTextView.append("Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            if (scrollAmount > 0) {
                peripheralTextView.scrollTo(0, scrollAmount);
            }
        }
    };

    @SuppressLint("NewApi")
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            String value = new String(data);
            MainActivity.this.runOnUiThread(() -> peripheralTextView.append("Received data: " + value + "\n"));
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            switch (newState) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    MainActivity.this.runOnUiThread(() -> {
                        peripheralTextView.append("device disconnected\n");
                        connectToDevice.setVisibility(View.VISIBLE);
                        disconnectDevice.setVisibility(View.INVISIBLE);
                    });
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    MainActivity.this.runOnUiThread(() -> {
                        peripheralTextView.append("device connected\n");
                        connectToDevice.setVisibility(View.INVISIBLE);
                        disconnectDevice.setVisibility(View.VISIBLE);
                    });
                    bluetoothGatt.discoverServices();
                    break;
                default:
                    MainActivity.this.runOnUiThread(() -> peripheralTextView.append("we encountered an unknown state, uh oh\n"));
                    break;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainActivity.this.runOnUiThread(() -> peripheralTextView.append("device services have been discovered\n"));
                displayGattServices(bluetoothGatt.getServices());

                for (BluetoothGattService service : bluetoothGatt.getServices()) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            serviceUUID = service.getUuid();
                            characteristicUUID = characteristic.getUuid();
                            bluetoothGatt.readCharacteristic(characteristic);
                        }
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            bluetoothGatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String value = new String(data);
                MainActivity.this.runOnUiThread(() -> peripheralTextView.append("Received data: " + value + "\n"));
            }
        }
    };

    @SuppressLint({"MissingPermission", "NewApi"})
    public void connectToDeviceSelected() {
        peripheralTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    public void disconnectDeviceSelected() {
        peripheralTextView.append("Disconnecting from device\n");
        bluetoothGatt.disconnect();
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            @SuppressLint({"NewApi", "LocalSuppress"}) final String uuid = gattService.getUuid().toString();
            MainActivity.this.runOnUiThread(() -> peripheralTextView.append("Service discovered: " + uuid + "\n"));
            @SuppressLint({"NewApi", "LocalSuppress"}) List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                @SuppressLint({"NewApi", "LocalSuppress"}) final String charUuid = gattCharacteristic.getUuid().toString();
                MainActivity.this.runOnUiThread(() -> peripheralTextView.append("Characteristic discovered for service: " + charUuid + "\n"));
            }
        }
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    public void startScanning() {
        status.setText("START");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        peripheralTextView.setText("");
        peripheralTextView.append("Started Scanning\n");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> btScanner.startScan(leScanCallback));
        mHandler.postDelayed(this::stopScanning, SCAN_PERIOD);
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    public void stopScanning() {
        peripheralTextView.append("Stopped Scanning\n");
        btScanning = false;
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(() -> btScanner.stopScan(leScanCallback));
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void startReadingData() {
        if (bluetoothGatt == null || isReading || serviceUUID == null || characteristicUUID == null) return;

        isReading = true;
        readDataButton.setVisibility(View.INVISIBLE);
        stopReadButton.setVisibility(View.VISIBLE);

        readRunnable = new Runnable() {
            @SuppressLint({"MissingPermission", "NewApi"})
            @Override
            public void run() {
                BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
                    if (characteristic != null) {
                        bluetoothGatt.readCharacteristic(characteristic);
                    }
                }
                readHandler.postDelayed(this, 1000); // Đọc lại sau 1 giây
            }
        };
        readHandler.post(readRunnable);
    }

    private void stopReadingData() {
        isReading = false;
        readHandler.removeCallbacks(readRunnable);
        readDataButton.setVisibility(View.VISIBLE);
        stopReadButton.setVisibility(View.INVISIBLE);
        peripheralTextView.append("Stopped reading data\n");
    }
}
