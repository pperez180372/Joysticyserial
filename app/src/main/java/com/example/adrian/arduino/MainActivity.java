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
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    //Addressing

    InetAddress IPL=null;
    // int puerto_de_escucha = 54545;
    int IPPORT=6070;
    ServerSocket sk;

    //LOG
    String LOG="";

    // connect
    boolean connect=false;

    public View Vista;

    UsbDevice device;
    UsbDeviceConnection connection;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    PendingIntent pendingIntent;
    int channel1 =127, channel2 =127, channel3=127, channel4 =127, channel5=127;

    ReentrantLock joy1 = new ReentrantLock();
    ReentrantLock joy2 = new ReentrantLock();
    ReentrantLock send = new ReentrantLock();

    //byte[] joystickData= new byte[5];
    byte joystickData[] = new byte[6];
    boolean messagesAvailableFromSerial=false;
    byte[] dataFromSerial;
    byte[] dataFromClient= new byte[5];


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
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);

                            setSLconnectCheckBox(true);

                            /*                        Toast toast2 = Toast.makeText(MainActivity.this, "Serial port opened", Toast.LENGTH_SHORT);
                            toast2.show();
*/

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

        Vista=this.findViewById(android.R.id.content).getRootView();

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        TextView ll = (TextView) findViewById(R.id.textView_LOG);
        if (ll != null) {
            ll.setGravity(Gravity.LEFT);

        }

        ScrollView ll1 = (ScrollView) findViewById(R.id.scrollView);
        if (ll1 != null) {
            ll1.fullScroll(View.FOCUS_DOWN);
        }


        setContentView(R.layout.activity_main);



// thread para almacenar la IP asignada
        new Thread(new Runnable() {
            public void run() {

                int IPOK=0;
                int i=0;

                while(true)
                {
                    IPL = getLocalAddress();
                    try {
                        if (IPL==null)
                        {
                            IPOK=0;
                        }
                        else
                        {
                            if (IPOK==0)
                            {
                                IPOK=1;
                                imprimirln("IP Local "+IPL.getHostAddress());
                                TextView tv;
                                tv = (TextView) findViewById(R.id.textView_IP);
                                tv.setText(""+IPL.getHostAddress());
                            }
                        }

                        i++;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        ).start();

// thread servidor



        new Thread(new Runnable() {
            public void run() {

                while (true) {
                    try {
                        while (true) {
                            InetAddress g1 = getLocalAddress();
                            sk = new ServerSocket(IPPORT, 0, g1);
                            StringBuilder sb = null, sbj = null;

                            while (!sk.isClosed()) {
                                sbj = null;
                                sb = null;
                                imprimirln("LISTEN ON "+IPPORT);
                                setIPconnectCheckBox(false);

                                Socket cliente = sk.accept();

                                imprimirln("new conection");

                                setIPconnectCheckBox(true);

                                BufferedReader entrada = new BufferedReader(
                                        new InputStreamReader(cliente.getInputStream()));

                                PrintWriter salida = new PrintWriter(
                                        new OutputStreamWriter(cliente.getOutputStream()), true);

                                while(cliente.isConnected()) {

                                    String line=null;
                                    sb=new StringBuilder();
                                    try {
                                        do {
                                          //  imprimirln("1");
                                            line=entrada.readLine();

                                            imprimirln("  "+line);

                                            final String finalLine = line;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    JoystickView tv = (JoystickView) findViewById(R.id.joystick);
                                                    tv.putButtonPosition(scanHex(finalLine)[2],scanHex(finalLine)[1]);
                                                    tv = (JoystickView) findViewById(R.id.joystick2);
                                                    tv.putButtonPosition(scanHex(finalLine)[4],scanHex(finalLine)[3]); }

                                            });

                                            SeekBar tv1 = (SeekBar) findViewById(R.id.seekBar_flightmode);

                                            int incx;
                                                    byte valx;

                                            valx=scanHex(finalLine)[5];
                                            incx=0;
                                            if (valx<0)  incx=256+(int)valx;
                                            else
                                                incx = (int)valx;


                                            tv1.setProgress((int) (incx/255.0*6.0+0.5));

                                            //joystickData scanHex(line);
                                            //imprimirln("g "+printHex(scanHex(line)));
                                            writeToSerial(scanHex(line));

                                        }
                                        while (entrada.ready());
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }

                            }
                            Thread.sleep(1000);

                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
            }

        }).start();


// thread cliente
        new Thread(new Runnable() {
            Socket socket;
            String res;

            public void run() {

                while (true) {

                    while(connect) {

                        try {
                            EditText et = (EditText) findViewById(R.id.textView_IP);
                            InetAddress lo=InetAddress.getByName(et.getText().toString());
                            imprimirln(lo.getHostName());// int puerto_de_escucha
                            socket = new Socket(lo, IPPORT);

                            PrintWriter out=new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())), true);

                            while(socket.isConnected()) {
                                setIPconnectCheckBox(true);

                                if (!connect)
                                {
                                        setIPconnectCheckBox(false);

                                        imprimirln("Cerrando el socket por cliente");
                                        socket.close();
                                        continue;
                                }



                                    //imprimirln("socketabierto");

                                        out.println(printHex(joystickData));
                                        out.flush();


                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                /*
                               */
                            }
                            if (socket.isConnected()==false) {
                                imprimirln("Imposible abrir socket");
                            }
                            else
                            {
                                imprimirln("Cerrando el socket por servidor");
                                socket.close();

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }



                    }


                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
            }

        }).start();

        // thread apertura serial

        new Thread(new Runnable() {
            public void run() {

                while (true) {

                               start(Vista);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    }
                    }

        }).start();


        SeekBar sk=(SeekBar) findViewById(R.id.seekBar_flightmode);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                float max=2100;
                float min=900;
                float step=(float) ((2100.0-900.0)/255.0);
                float f2=(330+65)/step;
                //00 es 900
                //FF es 2100
                //0xFF es 1200 + 900 fijos
                // 0xFF*130/1200 130 es ca escalon
                //1230-900= 0xFF*330/1200
                //0-1230 flight mode 1 --> 00;
                //-1360 fight mode 2 --> (0x00+1200/FF*) escogemos 1295 la mitad del rango
                channel5= (int) (progress*255.0/6.0);



            }
        });


        Button BotonConnect = (Button) findViewById(R.id.button_Connect);
        BotonConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                    connect=!connect;
                    if (connect)  {
                        Button BotonConnect = (Button) findViewById(R.id.button_Connect);
                        BotonConnect.setText("Disconnect");
                    }
                    else
                    {
                        Button BotonConnect = (Button) findViewById(R.id.button_Connect);
                        BotonConnect.setText("Connect");
                    }
                // cuando termine de ejecutarse la clase será destruida si ya no tiene referencias, por ejemplo la primera de dos ejecuciones.
            }

            ;
        });

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
        joystick2.setFixedCenter(true);
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

        joystick2.setAutoReCenterButton(false);

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
                joystickData[5]=(byte)channel5;
                joy1.unlock();
                joy2.unlock();
                //write(data);
                handler.postDelayed(this, 50);
            }
        },50);






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
    public static byte[]  scanHex(String cad) {

        byte[] v = new byte[6];
        for (int t=0;t<6;t++) {
            String g=cad.substring(t*5+4,(t+1)*5+1);
            //System.out.println(g);
            v[t]=(byte) Integer.parseInt(g,16);

        }
        return v;
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

          String st=new String(arg0);
          imprimirln(st);

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

                     if (!keep) break;
                 }
             }
         }

     }

     public void end(View view)
     {

         serialPort.close();



     }


// Funciones utiles



    // + * Returns a valid InetAddress to use for RMI communication. + * If the
    // system property java.rmi.server.hostname is set it is used. + * Secondly
    // InetAddress.getLocalHost is used. + * If neither of these are
    // non-loopback all network interfaces + * are enumerated and the first
    // non-loopback ipv4 + * address found is returned. If that also fails null
    // is returned.

    private static InetAddress getLocalAddress() {
        InetAddress inetAddr = null;

        //
        // 1) If the property java.rmi.server.hostname is set and valid, use it
        //

        try {
            //System.out.println("Attempting to resolve java.rmi.server.hostname");
            String hostname = System.getProperty("java.rmi.server.hostname");
            if (hostname != null) {
                inetAddr = InetAddress.getByName(hostname);
                if (!inetAddr.isLoopbackAddress()) {
                    return inetAddr;
                } else {
                    //System.out                     .println("java.rmi.server.hostname is a loopback interface.");
                }

            }
        } catch (SecurityException e) {
            System.out                    .println("Caught SecurityException when trying to resolve java.rmi.server.hostname");
        } catch (UnknownHostException e) {
            System.out
                    .println("Caught UnknownHostException when trying to resolve java.rmi.server.hostname");
        }

        // 2) Try to use InetAddress.getLocalHost
        try {
            //System.out                    .println("Attempting to resolve InetADdress.getLocalHost");
            InetAddress localHost = null;
            localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost;
            } else {
                //System.out                        .println("InetAddress.getLocalHost() is a loopback interface.");
            }

        } catch (UnknownHostException e1) {
            System.out                    .println("Caught UnknownHostException for InetAddress.getLocalHost()");
        }

        // 3) Enumerate all interfaces looking for a candidate
        Enumeration ifs = null;
        try {
            //System.out                    .println("Attempting to enumerate all network interfaces");
            ifs = NetworkInterface.getNetworkInterfaces();

            // Iterate all interfaces
            while (ifs.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) ifs.nextElement();

                // Fetch all IP addresses on this interface
                Enumeration ips = iface.getInetAddresses();

                // Iterate the IP addresses
                while (ips.hasMoreElements()) {
                    InetAddress ip = (InetAddress) ips.nextElement();
                    if ((ip instanceof Inet4Address) && !ip.isLoopbackAddress()) {
                        return (InetAddress) ip;
                    }
                }
            }
        } catch (SocketException se) {
            System.out.println("Could not enumerate network interfaces");
        }

        // 4) Epic fail
        //System.out                .println("Failed to resolve a non-loopback ip address for this host.");
        return null;
    }

    synchronized void ponTextoTextView(final String cad, final int id) {
        final String g = cad;

        runOnUiThread(new Runnable() {
            public void run() {

                TextView ll = (TextView) findViewById(id);
                if (ll != null) {ll.setGravity(Gravity.LEFT);
                                 ll.setText(cad);}
                ScrollView ll1 = (ScrollView) findViewById(R.id.scrollView);
                if (ll1 != null) {
                    ll1.fullScroll(View.FOCUS_DOWN);

                }



            }
        });
    }

    synchronized void imprimir(final String cad) {
        final String g = cad;

        LOG=LOG+cad;
        if (LOG.length()>500)
            LOG=LOG.substring(100,LOG.length());
        ponTextoTextView(LOG,R.id.textView_LOG);

    }

    public synchronized  String getLOG() {return LOG;};


    public void imprimirln(final String cad) {
        imprimir(cad + "\r\n");
    }

    public void setIPconnectCheckBox(final boolean status)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                CheckBox cb=(CheckBox) findViewById(R.id.checkBox_IP);
                cb.setChecked(status);
            }

        });

    }

    public void setSLconnectCheckBox(final boolean status) {
        runOnUiThread(new Runnable() {
            public void run() {
                CheckBox cb=(CheckBox) findViewById(R.id.checkBox_SL);
                cb.setChecked(status);
            }

        });

    }
}



