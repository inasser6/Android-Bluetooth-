package com.example.mybluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {
    private static final int DELIMITER = '\n';
    final String ON = "1";
    final String OFF = "0";
    final String ReadVolts = "3";
    private InputStream inStream;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Handler readHandler;

    BluetoothSPP bluetooth;
    private BluetoothSocket socket;
    private String rx_buffer = "";

    Button connect;
    Button on;
    Button off;
    Button readVolt;

    public MainActivity() {
        readHandler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetooth = new BluetoothSPP(this);
        // Buffer used to parse messages
         String rx_buffer = "";
        connect = (Button) findViewById(R.id.connect);
        on = (Button) findViewById(R.id.on);
        off = (Button) findViewById(R.id.off);
        readVolt= (Button) findViewById(R.id.volt);
        if (!bluetooth.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                connect.setText("Connected to " + name);
                try {
                    connectSocket();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            public void onDeviceDisconnected() {
                connect.setText("Connection lost");
            }

            public void onDeviceConnectionFailed() {
                connect.setText("Unable to connect");
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bluetooth.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });

        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.send(ON, true);
            }
        });
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.send(OFF, true);
            }
        });

        readVolt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.send(ReadVolts, true);
                String value = readVolt();
                Toast.makeText(getApplicationContext(), "Volts:"+value, Toast.LENGTH_SHORT).show();
/*
                    Handler mHandler = new Handler(){

                        public void handleMessage(android.os.Message msg){
                            Toast.makeText(getApplicationContext(), "Volts: "+msg, Toast.LENGTH_SHORT).show();

                            if(msg.what == BluetoothState.MESSAGE_READ){
                                byte[] readBuf = (byte[]) msg.obj;
                                String readMessage = new String(readBuf);
                                Log.d(readMessage,"hjhhjjhjh");
                                try {
                                    readMessage = new String((byte[]) msg.obj, "UTF-8");
                                    readVolt.setText("Volt:"+readMessage);
                                    Toast.makeText(getApplicationContext(), "Volts: "+readMessage, Toast.LENGTH_SHORT).show();

                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "Bluetooth is not available"+e, Toast.LENGTH_SHORT).show();
                                    connect.setText("Unable to connect");

                                }
                            }


                        }
                    };
*/
                    }
        });
    }
    private void connectSocket () throws Exception {
// Find the remote device
        BluetoothAdapter adapter = bluetooth.getBluetoothAdapter();
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(adapter.getAddress().toUpperCase());

        // Create a socket with the remote device using this protocol
        socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);

        // Make sure Bluetooth adapter is not in discovery mode
        adapter.cancelDiscovery();
        // Connect to the socket
        socket.connect();
        wait(200);

        inStream = socket.getInputStream();


        // Get input and output streams from the socket
        // outStream = socket.getOutputStream();
        //

    }
    private String readVolt() {

        String s = "0";
       // run();
        try {
            // Check if there are bytes available
            if (inStream.available() > 0) {

                // Read bytes into a buffer
                byte[] inBuffer = new byte[1024];
                int bytesRead = inStream.read(inBuffer);

                // Convert read bytes into a string
                s = new String(inBuffer, "ASCII");
                s = s.substring(0, bytesRead);
            }

        } catch (Exception e) {
            Log.d("", "Read failed!", e);
        }

        return s;
    }


    private String read() {

        String s = "";

        try {
            // Check if there are bytes available
            if (inStream.available() > 0) {

                // Read bytes into a buffer
                byte[] inBuffer = new byte[1024];
                int bytesRead = inStream.read(inBuffer);

                // Convert read bytes into a string
                s = new String(inBuffer, "ASCII");
                s = s.substring(0, bytesRead);
                parseMessages();

            }

        } catch (Exception e) {
            Log.e("", "Read failed!", e);
        }

        return s;
    }



    /**
     * Send complete messages from the rx_buffer to the read handler.
     */
    private void parseMessages() {

        // Find the first delimiter in the buffer
        int inx = rx_buffer.indexOf(DELIMITER);

        // If there is none, exit
        if (inx == -1)
            return;

        // Get the complete message
        String s = rx_buffer.substring(0, inx);

        // Remove the message from the buffer
        rx_buffer = rx_buffer.substring(inx + 1);

        // Send to read handler
        sendToReadHandler(s);

        // Look for more complete messages
        parseMessages();
    }

    /**
     * Entry point when thread.start() is called.
     */
    private void sendToReadHandler(String s) {

        Message msg = Message.obtain();
        msg.obj = s;
        readHandler.sendMessage(msg);
        Log.i("", "[RECV] " + s);
    }

    public void run() {

        // Attempt to connect and exit the thread if it failed
        try {
            connectSocket();
            sendToReadHandler("CONNECTED");
        } catch (Exception e) {
            Log.e("", "Failed to connect!", e);
            sendToReadHandler("CONNECTION FAILED");
            return;
        }

        // Loop continuously, reading data, until thread.interrupt() is called
        while (!this.isFinishing()) {

            // Make sure things haven't gone wrong
            if (inStream == null){
                Log.e("", "Lost bluetooth connection!");
                break;
            }

            // Read data and add it to the buffer
            String s = read();
            if (s.length() > 0)
                rx_buffer += s;

            // Look for complete messages
            parseMessages();
        }

        // If thread is interrupted, close connections
        //disconnect();
        sendToReadHandler("DISCONNECTED");
    }

    public void onStart() {
        super.onStart();
        if (!bluetooth.isBluetoothEnabled()) {
            bluetooth.enable();
        } else {
            if (!bluetooth.isServiceAvailable()) {
                bluetooth.setupService();
                bluetooth.startService(BluetoothState.DEVICE_OTHER);
            }
        }
    }


    public void onDestroy() {
        super.onDestroy();
        bluetooth.stopService();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bluetooth.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetooth.setupService();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}