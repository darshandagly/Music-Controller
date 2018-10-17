package com.bellighting.musiccontroller.musiccontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity  {


    TextView text;
    LinearLayout connection;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    final String TAG="DAM5";
    private MenuItem menuconnect;
    Long execTime=System.currentTimeMillis()/1000;
    volatile boolean repeat=true,flag=false;
    String lastCmd="0";
    String currCmd="0";
    Thread limiterThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView)findViewById(R.id.text);
        connection =(LinearLayout)findViewById(R.id.layout1);

        final ImageButton play = (ImageButton) findViewById(R.id.play);
        ImageButton prev = (ImageButton) findViewById(R.id.prev);
        ImageButton next = (ImageButton) findViewById(R.id.next);



        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag==false) {
                    Intent i = new Intent("com.android.music.musicservicecommand");
                    i.putExtra("command", "play");
                    sendBroadcast(i);
                    play.setImageResource(R.drawable.pause);
                    flag=true;
                }
                else
                {
                    Intent i = new Intent("com.android.music.musicservicecommand");
                    i.putExtra("command", "pause");
                    sendBroadcast(i);
                    flag=false;
                    play.setImageResource(R.drawable.play);
                }
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent("com.android.music.musicservicecommand");
                i.putExtra("command", "previous");
                sendBroadcast(i);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent("com.android.music.musicservicecommand");
                i.putExtra("command", "next");
                sendBroadcast(i);
            }
        });



        // Button openButton = (Button)findViewById(R.id.open);




        //Open Button
      /*  openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });       */

        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        SeekBar volControl = (SeekBar)findViewById(R.id.volume);
        volControl.setMax(maxVolume);
        volControl.setProgress(curVolume);
        volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
            }
        });




        //Setting up filter to check if device has disconnected.
        IntentFilter disconFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, disconFilter);
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                Log.d(TAG, "Lost connection to the wearable");

                //trial code to restart app
                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                //end of trial code

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        menuconnect=menu.getItem(0);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.connect:
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) {
                    Log.d(TAG, "",  ex);
                }
        }
        return false;
    }





    void findBT()
    {
        //Toast.makeText(getApplicationContext(),"Finding Bluetooth",Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Entered findBT");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "No Bluetooth Adapter Found", Toast.LENGTH_LONG).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetooth);
        }

        Log.d(TAG,"Bluetooth Enabled");

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        if(mmDevice==null)
        {
            Log.d(TAG,"Device Not Found. Please Pair First");
            Toast.makeText(getApplicationContext(), "Device Not Found. Please Pair First", Toast.LENGTH_LONG).show();

            System.exit(0);
        }
        Toast.makeText(this, "Bluetooth Device Found", Toast.LENGTH_LONG).show();
    }

    void openBT() throws IOException
    {
        Toast.makeText(getApplicationContext(), "Connecting to your Wearable", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"Entered openBT");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        connection.setBackgroundColor(Color.parseColor("#00C853"));
        text.setText("Connected");
        Toast.makeText(getApplicationContext(), "Connected to your Wearable", Toast.LENGTH_SHORT).show();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();


        beginListenForData();

        Toast.makeText(this, "No Bluetooth Adapter Found", Toast.LENGTH_LONG).show();
    }

    void beginListenForData()
    {
        Log.d(TAG,"Entered beginListenForData");
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {

                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");

                                    currCmd=data;
                                    Log.d(TAG,"->Data Received"+ data);
                                    readBufferPosition = 0;
                                    if(repeat) {
                                        Log.d(TAG, "-->Performed the action");
                                        try {
                                            goMusic(data);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        repeat=false;
                                        lastCmd=currCmd;
                                        execTime=System.currentTimeMillis()/1000;
                                        Log.d(TAG, "-->currCommand ="+ currCmd);
                                        Log.d(TAG, "-->execTime =" + execTime);

                                    }
                                    else{
                                        Log.d(TAG, "-->Did not perform action");
                                        Thread.yield();
                                    }
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }

            private void goMusic(String data) throws InterruptedException {
                Log.d(TAG, "Entered goServer()");
                Log.d(TAG, "Data received is:" + data);


                data= data.substring( (data.indexOf("*") +1), data.lastIndexOf("#") );
                int ch = Integer.parseInt(data);

                switch (ch)
                {
                    case 1:
                        Intent i1 = new Intent("com.android.music.musicservicecommand");
                        i1.putExtra("command", "play");
                        sendBroadcast(i1);
                        break;
                    case 2:
                        Intent i2 = new Intent("com.android.music.musicservicecommand");
                        i2.putExtra("command", "previous");
                        sendBroadcast(i2);
                        break;
                    case 3:
                        Intent i3 = new Intent("com.android.music.musicservicecommand");
                        i3.putExtra("command", "next");
                        sendBroadcast(i3);
                        break;
                    case 4:
                        Intent i4 = new Intent("com.android.music.musicservicecommand");
                        i4.putExtra("command", "pause");
                        sendBroadcast(i4);
                        break;

                    default:
                        break;
                }

            }


        });
        limiterThread = new Thread(new Runnable(){
            public void run(){
                Log.d(TAG, "->Entered limiterThread: Repeat= "+ repeat);
                while (!stopWorker) {
                    if(!repeat) {
                        Long time = System.currentTimeMillis() / 1000;

                        if ((time - execTime) >= 3000) {
                            repeat = true;
                            Log.d(TAG, "-->Repeat set to true: Timed-out");
                        } else if (!(lastCmd.equalsIgnoreCase(currCmd))) {
                            repeat = true;
                            Log.d(TAG, "-->Repeat set to true: New Command");
                        }

                        Thread.yield();

                        Log.d(TAG, "->Repeat unchanged: Repeat= " + repeat);
                    }
                    else{
                        Thread.yield();
                    }
                }

            }
        });
        limiterThread.start();
        workerThread.start();

        Log.d(TAG, "Exiting beginListenForData()");
    }
}

