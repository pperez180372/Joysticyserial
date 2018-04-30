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
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ScrollView;
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
                                            line=entrada.readLine();
                                            sb.append(line + "\n");
                                            //hacking

                                            System.out.println("S" + sb);
                                        }
                                        while (entrada.ready());
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                                cliente.close();
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

                            socket = new Socket(IPL, IPPORT);

                            while(socket.isConnected()) {

                                if (connect)
                                {
                                        imprimirln("Cerrando el socket por cliente");
                                        socket.close();
                                        continue;
                                }
                                    imprimirln("socketabierto");
                                    PrintWriter out=new PrintWriter(new BufferedWriter(
                                        new OutputStreamWriter(socket.getOutputStream())), true);
                                        imprimirln("C GET");
                                    out.print("GET \r\n");
                                        out.flush();
                                        out.close();

                                try {
                                    Thread.sleep(3000);
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
/*
Socket socket;
    String res;
    try {



        final String EndPoint="/iot/d?"+"k="+apikey+"&i="+device+"&d="+Attribute+"|"+Value;



        InetAddress serverAddr = InetAddress.getByName(ServerName);
        socket = new Socket(serverAddr, port);
        if (socket.isConnected()) {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),true);
            out.print("GET "+EndPoint+" HTTP/1.1\r\n");
            out.print("Host: "+IP+"\r\n");
            out.print("Connection: keep-alive\r\n");

            out.print("\r\n\r\n");
            out.flush();
            out.close();
            socket.close();

        }
        else res="Imposible abrir socket";


    } catch (UnknownHostException e1) {
        e1.printStackTrace();
    } catch (IOException e1) {
        e1.printStackTrace();
    }
    return "";

*/

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



/*
        Button toggle = (Button) findViewById(R.id.button_Connect);
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

*/


/*

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
*/

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
                if (ll != null) ll.setText(cad);
            }
        });
    }

    synchronized void imprimir(final String cad) {
        final String g = cad;

        LOG=LOG+cad;
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
}



