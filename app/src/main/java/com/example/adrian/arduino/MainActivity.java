package com.example.adrian.arduino;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//imports sockets
import java.io.*;
import java.net.*;


import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    UsbDevice device;
    UsbDeviceConnection connection;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    PendingIntent pendingIntent;
    int channel1 =127, channel2 =127, channel3=127, channel4 =127;
    ReentrantLock joy1 = new ReentrantLock();
    ReentrantLock joy2 = new ReentrantLock();
    ReentrantLock send = new ReentrantLock();

    //byte[] joystickData= new byte[5];
    byte joystickData[] = new byte[5];
    boolean messagesAvailableFromSerial=false;
    byte[] dataFromSerial;
    byte[] dataFromClient= new byte[5];

    //variables sockets
    private final int PORT = 8080; //Puerto para la conexión
    private final String HOST = "127.0.0.1"; //localhost

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                Toast toast = Toast.makeText(MainActivity.this, "Permission", Toast.LENGTH_SHORT);
                toast.show();
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) {
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Toast toast2 = Toast.makeText(MainActivity.this, "Serial port opened", Toast.LENGTH_SHORT);
                            toast2.show();


                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERMISSION NOT GRANTED");
                }
            }
        };
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        joystickData[0]=0x2A;

        JoystickView joystick = (JoystickView) findViewById(R.id.joystick);

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {


                double per= strength/100.0;
                double var= per*127.5;

                joy1.lock();

                double value= (var*Math.sin(angle*(Math.PI/180)))+127.5;
                channel1= (int) value;
                value= (int)(var*Math.cos(angle*(Math.PI/180)))+127.5;
                channel2= (int) value;
                joy1.unlock();

            }
        });

        JoystickView joystick2 = (JoystickView) findViewById(R.id.joystick2);

        joystick2.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // do whatever you want
                double per= strength/100.0;
                double var= per*127.5;
                //System.out.println(strength+"  " + per+ " "+var);

                joy2.lock();
                double value= (var* Math.sin(angle*(Math.PI/180)))+127.5;
                channel3= (int) value;
                value= (var*Math.cos(angle*(Math.PI/180)))+127.5;
                channel4= (int) value;
                joy2.unlock();

            }
        });



        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(broadcastReceiver, filter);


        final Handler handler=new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                joy1.lock();
                joy2.lock();

                joystickData[1]=(byte)channel1;
                joystickData[2]=(byte)channel2;
                joystickData[3]=(byte)channel3;
                joystickData[4]=(byte)channel4;
                joy1.unlock();
                joy2.unlock();
                //write(data);
                handler.postDelayed(this, 50);
            }
        },50);









        ToggleButton toggle = (ToggleButton) findViewById(R.id.buttonCS);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Thread server= new Thread(new Runnable() {
                        DataOutputStream output; //Flujo de datos de salida
                        Socket clientsocket;
                        ServerSocket serversocket;

                        DataInputStream input;

                        public void sendToClient(){
                            try {
                                output.write( dataFromSerial);
                                messagesAvailableFromSerial = false;
                                send.unlock();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        @Override
                        public void run() {
                            try  {

                                serversocket = new ServerSocket();//Se crea el socket para el servidor
                                serversocket.bind(new InetSocketAddress(HOST, PORT)); //se asigna el socket a localhost y puerto 8080

                                //Socket para el cliente
                                clientsocket = serversocket.accept(); //Accept comienza el socket y espera una conexión desde un cliente

                                //Se obtiene el flujo entrante desde el cliente
                                input = new DataInputStream(clientsocket.getInputStream());
                                output = new DataOutputStream(clientsocket.getOutputStream());

                                while (true) {

                                    if (( input.read(dataFromClient, 0, 5)) == 5 ) //Mientras haya mensajes desde el cliente
                                    {
                                        writeToSerial(dataFromClient);
                                    }

                                    if (messagesAvailableFromSerial == true) sendToClient();

                                }

                            } catch (IOException e1) {
                            }
                        }
                    });
                    server.start();
                    
                } else {
                    Thread cliente = new Thread(new Runnable() {
                        private DataOutputStream output; //Flujo de datos de salida
                        private Socket cs;
                        private Scanner input;
                        @Override
                        public void run() {
                            try  {
                                final TextView tv = findViewById(R.id.IP);
                                Thread.sleep(1000);

                                cs = new Socket(HOST,PORT); //se conecta el cliente a localhost y puerto 8080

                                //Flujo de entrada al cliente
                                input = new Scanner(cs.getInputStream());

                                //Flujo de datos hacia el servidor
                                output = new DataOutputStream(cs.getOutputStream());

                                new Thread( new Runnable() {
                                    @Override
                                    public void run() {
                                        while(true){
                                            if(input.hasNextLine()){
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        tv.append(input.nextLine()+"\n");
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }).start();

                                while (true) {

                                    //Se escribe en el servidor usando su flujo de datos
                                    output.write(joystickData);
                                    Thread.sleep(20); //Pequeño delay opcional
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    cliente.start();

                }
            }
        });


    }

    public static String printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public void writeToSerial(byte[] data)  {
        try {

           if(serialPort!=null) serialPort.write(data);
           else{

               System.out.println(printHex(data));
           }
        }
        catch(Exception e){}
    }


    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] arg0)
        {

            send.lock();
            dataFromSerial=arg0;
            messagesAvailableFromSerial= true;

        }

    };



    public void start(View view)
     {
         if (serialPort == null) {
             usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

             HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
             if (!usbDevices.isEmpty()) {
                 boolean keep = true;
                 for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                     device = entry.getValue();
                         usbManager.requestPermission(device, pendingIntent);
                         keep = false;

                     if (!keep)
                         break;
                 }
             }
         }


     }

     public void end(View view)
     {

         serialPort.close();



     }



}



