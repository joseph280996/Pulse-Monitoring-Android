package com.tonydicola.bletest.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class LandingPage extends Activity {
    private ILineDataSet set1;
    private WebSocketClient mWebSocketClient;
    private final String LOG_TAG = "myApp";
    //Data Output
    private String output;

    private int dataSetIdx = 0;
    private int totalValueReceived = 0;
    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // UI elements
    private Button getSensorValBtn;
    private Button scanForDeviceBtn;
    private SeekBar seekBar;
    private Button stopBtn;
    private LineChart graph;


    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    private CountDownTimer countdownTimer = new CountDownTimer(15000, 1000) {

        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            gatt.disconnect();
            Log.v(LOG_TAG, "Sum of values received: "+ totalValueReceived);
            totalValueReceived = 0;
            Log.v(LOG_TAG, "Sum of values received: "+ totalValueReceived);
        }
    };


    private final long pollingInterval = 50;

    private Timer myTimer = new Timer();
    private TimerTask myTimerTask;


    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {

                Log.v(LOG_TAG,"Connected!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        graph.setVisibility(View.VISIBLE);
                        stopBtn.setVisibility(View.VISIBLE);
                        getSensorValBtn.setVisibility(View.VISIBLE);
                        scanForDeviceBtn.setVisibility(View.GONE);
                    }
                });

                // Discover services.
                if (!gatt.discoverServices()) {
                    Log.v(LOG_TAG,"Failed to start discovering services!");
                }

//                countdownTimer.start();
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.v(LOG_TAG,"Disconnected!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        graph.setVisibility(View.GONE);
                        stopBtn.setVisibility(View.GONE);
                        getSensorValBtn.setVisibility(View.GONE);
                        scanForDeviceBtn.setVisibility(View.VISIBLE);
                    }
                });
                gatt.close();
            }
            else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        graph.setVisibility(View.GONE);
                        stopBtn.setVisibility(View.GONE);
                        getSensorValBtn.setVisibility(View.GONE);
                        scanForDeviceBtn.setVisibility(View.VISIBLE);
                    }
                });

                Log.v(LOG_TAG,"Connection state changed.  New state: " + newState);
            }
        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(LOG_TAG,"Service discovery completed!");
            }
            else {
                Log.v(LOG_TAG,"Service discovery failed with status: " + status);
            }

            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

            Log.v("BLE", "tx:" + tx);
            Log.v("BLE", "rx:" + rx.getDescriptor(CLIENT_UUID) );

            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                Log.v(LOG_TAG,"Couldn't set notifications for RX characteristic!");
            }

            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    Log.v(LOG_TAG,"Couldn't write RX client descriptor value!");
                }
            }
            else {
                Log.v(LOG_TAG,"Couldn't get RX client descriptor!");
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            addEntry(characteristic.getStringValue(0));
            String value = "sensor:" + characteristic.getStringValue(0);
            mWebSocketClient.send(value);
//            Log.v(LOG_TAG, "Value Received: "+ Integer.parseInt(characteristic.getStringValue(0)));
//            totalValueReceived += Integer.parseInt(characteristic.getStringValue(0));
        }
    };

    // BTLE device scanning callback.
    private LeScanCallback scanCallback = new LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            Log.v(LOG_TAG,"Found device: " + bluetoothDevice.getAddress());
            // Check if the device has the UART service.
//            && bluetoothDevice.getName().equals("tung")
            if (parseUUIDs(bytes).contains(UART_UUID) ) {
                // Found a device, stop the scan.
                adapter.stopLeScan(scanCallback);
                Log.v(LOG_TAG,"Found UART service!");
                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.
                gatt = bluetoothDevice.connectGatt(getApplicationContext(), false, callback);
            }
        }
    };

    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Establish WebSocket connection
        createWebSocket();

        // Grab references to UI elements.
        adapter = BluetoothAdapter.getDefaultAdapter();
        stopBtn = (Button) findViewById(R.id.disconnectBtn);
        scanForDeviceBtn = (Button) findViewById(R.id.startScan);
        getSensorValBtn = (Button) findViewById(R.id.getSensorValBtn);
        graph = (LineChart) findViewById(R.id.brain_chart);

        graph.setVisibility(View.GONE);
        stopBtn.setVisibility(View.GONE);
        getSensorValBtn.setVisibility(View.GONE);
        scanForDeviceBtn.setVisibility(View.VISIBLE);

        // Chart listener
        graph.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

            }

            @Override
            public void onNothingSelected() {

            }
        });

        // Enable description text
        graph.getDescription().setEnabled(true);

        // Enable touch gestures
        graph.setTouchEnabled(true);

        // Enable scaling and dragging
        graph.setDragEnabled(true);
        graph.setScaleEnabled(true);
        graph.setDrawGridBackground(false);

        // If disabled, scaling can be done on x- and y-axis separately
        graph.setPinchZoom(true);

        // Set an alternative background color
        graph.setBackgroundColor(Color.BLACK);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // Add empty data
        graph.setData(data);

        graph.getDescription().setText("Pulse data chart");
        graph.getDescription().setTextColor(Color.WHITE);

        // Get the legend
        Legend l = graph.getLegend();

        // Set the Legend for the graph
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = graph.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = graph.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(40f);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = graph.getAxisRight();
        rightAxis.setEnabled(false);

        // Start Scanning for Devices Button
        scanForDeviceBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v) {
                Log.v(LOG_TAG,"Scanning for devices...");
                adapter.startLeScan(scanCallback);
            }
        });

        // Disconnect button
        stopBtn.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick (View v) {
                myTimerTask.cancel();
            }
        });

        // Get sensor value
        getSensorValBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myTimer = new Timer();
                myTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        String setSensorMessage = "/pulse /";
                        tx.setValue(setSensorMessage.getBytes(Charset.forName("UTF-8")));
                        if (gatt.writeCharacteristic(tx)) {
                            Log.v(LOG_TAG, "Sent: "+ setSensorMessage);
                        } else {
                            Log.v(LOG_TAG, "Couldn't write TX characteristic!");
                        }
                    }
                };
                myTimer.scheduleAtFixedRate(myTimerTask, 0, pollingInterval);
            }
        });
    }

    private void createWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://165.227.254.178/socket");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handShakeData) {
                Log.v(LOG_TAG, "WebSocket successfully opened!");
            }

            @Override
            public void onMessage(String message) {
                Log.v(LOG_TAG, "Message Received: "+ message);
                tx.setValue(message.getBytes(Charset.forName("UTF-8")));
                if(gatt.writeCharacteristic(tx)){
                    Log.v(LOG_TAG, "Sent: "+ message);
                } else {
                    Log.v(LOG_TAG, "Couldn't write TX characteristic!");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote){
                Log.v(LOG_TAG, "WebSocket closing");
                Log.v(LOG_TAG, "Reason: "+reason);
                Log.v(LOG_TAG, "Code: " + code);
            }

            @Override
            public void onError(Exception ex){
                Log.e(LOG_TAG, "Error encountered!");
                ex.printStackTrace();
            }
        };
        mWebSocketClient.connect();
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Pulse waves");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GREEN);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setFillColor(Color.GREEN);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }


    private void addEntry(String value) {

        LineData data = graph.getData();
        if (data != null) {

            set1 = data.getDataSetByIndex(0);
            if (set1 == null) {
                set1 = createSet();
                data.addDataSet(set1);
            }
            data.addEntry(new Entry(set1.getEntryCount(), Float.parseFloat(value)), dataSetIdx);
            data.notifyDataChanged();

            // let the graph know it's data has changed
            graph.notifyDataSetChanged();

            // limit the number of visible entries
            graph.setVisibleXRangeMaximum(10);

            // move to the latest entry
            graph.moveViewToX(data.getEntryCount());

        }
    }

    // OnResume, called right before UI is displayed.  Start the BTLE connection.
    @Override
    protected void onResume() {
        super.onResume();
        // Scan for all BTLE devices.
        // The first one with the UART service will be chosen--see the code in the scanCallback.

    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        if (gatt != null) {
            // For better reliability be careful to disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }

    // Boilerplate code from the activity creation:

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
