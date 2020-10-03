package com.agusj.myprinter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintDocument;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

public class MyPrintService extends PrintService {
    PrinterInfo mThermalPrinter;

    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;
    /******************************************************************************************************/
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECTION_LOST = 6;
    public static final int MESSAGE_UNABLE_CONNECT = 7;
    /*******************************************************************************************************/
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CHOSE_BMP = 3;
    private static final int REQUEST_CAMER = 4;

    //QRcode
    private static final int QR_WIDTH = 350;
    private static final int QR_HEIGHT = 350;
    /*******************************************************************************************************/
    private static final String CHINESE = "GBK";
    private static final String THAI = "CP874";
    private static final String KOREAN = "EUC-KR";
    private static final String BIG5 = "BIG5";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the services
    private BluetoothService mService = null;

    @Override
    public void onCreate() {
        mThermalPrinter = new PrinterInfo.Builder(generatePrinterId("USB"),
                "USB Printer", PrinterInfo.STATUS_IDLE).build();
        appendLog("printservice create");
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();

        }
        if (!mBluetoothAdapter.isEnabled()) {
            // Otherwise, setup the session
        } else {
            if (mService == null) {
                KeyListenerInit();//监听
                appendLog("Check bt addr...");
                String address = "66:22:52:BB:7E:C2";//data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                if (BluetoothAdapter.checkBluetoothAddress(address)) {
                    BluetoothDevice device = mBluetoothAdapter
                            .getRemoteDevice(address);
                    // Attempt to connect to the device
                    appendLog("Connecting bt...");
                    mService.connect(device);
                }
            }

        }
    }
    private void SendDataString(String data) {

        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (data.length() > 0) {
            try {
                appendLog("Printing bt...");
                mService.write(data.getBytes("GBK"));
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     *SendDataByte
     */
    private void SendDataByte(byte[] data) {

        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        appendLog("send data...");
        mService.write(data);
    }

    private void KeyListenerInit() {
        //update status
        Toast.makeText(getApplicationContext(), "KeyListenerInit",
                Toast.LENGTH_SHORT).show();
        mService = new BluetoothService(this, mHandler);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (DEBUG)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "STATE_CONNECTED",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(),
                                    R.string.title_connecting,
                                    Toast.LENGTH_SHORT).show();
                            //mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Toast.makeText(getApplicationContext(),
                                    R.string.title_not_connected,
                                    Toast.LENGTH_SHORT).show();
                            //mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    break;
                case MESSAGE_READ:

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + mConnectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    appendLog("Connected to " + mConnectedDeviceName);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_CONNECTION_LOST:    //蓝牙已断开连接
                    Toast.makeText(getApplicationContext(), "Device connection was lost",
                            Toast.LENGTH_SHORT).show();
                    appendLog("dev bt lost...");
                    break;
                case MESSAGE_UNABLE_CONNECT:     //无法连接设备
                    Toast.makeText(getApplicationContext(), "Unable to connect device",
                            Toast.LENGTH_SHORT).show();
                    appendLog("unable to connect bt...");
                    break;
            }
        }
    };
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services
        if (mService != null)
            mService.stop();
        if (DEBUG)
            Log.e(TAG, "--- ON DESTROY ---");
    }

   /* @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:{
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = "";//data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    if (BluetoothAdapter.checkBluetoothAddress(address)) {
                        BluetoothDevice device = mBluetoothAdapter
                                .getRemoteDevice(address);
                        // Attempt to connect to the device
                        mService.connect(device);
                    }
                }
                break;
            }
            case REQUEST_ENABLE_BT:{
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a session
                    KeyListenerInit();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    //finish();
                }
                break;
            }
            case REQUEST_CHOSE_BMP:{
                if (resultCode == Activity.RESULT_OK){

                }else{
                    Toast.makeText(this, getString(R.string.msg_statev1), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_CAMER:{
                if (resultCode == Activity.RESULT_OK){
                    // handleSmallCameraPhoto(data);
                }else{
                    Toast.makeText(this, getText(R.string.camer), Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }*/

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        Log.d("myprinter", "MyPrintService#onCreatePrinterDiscoverySession() called");
        appendLog("discovery");
        return new ThermalPrinterDiscoverySession(mThermalPrinter);
        /*return new PrinterDiscoverySession() {
            @Override
            public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
                Log.d("myprinter", "PrinterDiscoverySession#onStartPrinterDiscovery(priorityList: " + priorityList + ") called");

                if (!priorityList.isEmpty()) {
                    return;
                }

                final List<PrinterInfo> printers = new ArrayList<>();
                final PrinterId printerId = generatePrinterId("aaa");
                final PrinterInfo.Builder builder = new PrinterInfo.Builder(printerId, "dummy printer", PrinterInfo.STATUS_IDLE);
                PrinterCapabilitiesInfo.Builder capBuilder = new PrinterCapabilitiesInfo.Builder(printerId);
                capBuilder.addMediaSize(PrintAttributes.MediaSize.ISO_A4, true);
                capBuilder.addMediaSize(PrintAttributes.MediaSize.ISO_A3, false);
                capBuilder.addResolution(new PrintAttributes.Resolution("resolutionId", "default resolution", 600, 600), true);
                capBuilder.setColorModes(PrintAttributes.COLOR_MODE_COLOR | PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR);
                builder.setCapabilities(capBuilder.build());
                printers.add(builder.build());
                addPrinters(printers);
            }

            @Override
            public void onStopPrinterDiscovery() {
                Log.d("myprinter", "MyPrintService#onStopPrinterDiscovery() called");
            }

            @Override
            public void onValidatePrinters(List<PrinterId> printerIds) {
                Log.d("myprinter", "MyPrintService#onValidatePrinters(printerIds: " + printerIds + ") called");
            }

            @Override
            public void onStartPrinterStateTracking(PrinterId printerId) {
                Log.d("myprinter", "MyPrintService#onStartPrinterStateTracking(printerId: " + printerId + ") called");
            }

            @Override
            public void onStopPrinterStateTracking(PrinterId printerId) {
                Log.d("myprinter", "MyPrintService#onStopPrinterStateTracking(printerId: " + printerId + ") called");
            }

            @Override
            public void onDestroy() {
                Log.d("myprinter", "MyPrintService#onDestroy() called");
            }
        };*/
    }
    public void toBitmap(File filepdf) throws IOException
    {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(filepdf, ParcelFileDescriptor.MODE_READ_ONLY);
        if(pfd != null)
        {
            PdfRenderer pdfr = new PdfRenderer(pfd);

            PdfRenderer.Page page = pdfr.openPage(0);
            Bitmap bmp0 = Bitmap.createBitmap(page.getWidth()*3, page.getHeight()*3, Bitmap.Config.ARGB_8888);

            page.render(bmp0, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            Bitmap bmp = Bitmap.createBitmap(bmp0,90,90,3*page.getWidth()-240,3*page.getHeight()-240);
            File sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dest = new File(sdDir, "small.png");
            try {
                FileOutputStream out = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                //SendDataByte(PrinterCommand.POS_Print_Text("Print after generate bitmap", CHINESE, 0, 0, 0, 0));
                //SendDataByte(Command.LF);
                int nMode = 0;
                int nPaperWidth = 384;
                out.flush(); out.close();


                if(bmp != null)
                {
                    appendLog("Print bmp");
                    byte[] data = PrintPicture.POS_PrintBMP(bmp, nPaperWidth, nMode);
                    SendDataByte(Command.ESC_Init);
                    SendDataByte(Command.LF);
                    SendDataByte(data);
                    SendDataByte(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
                    SendDataByte(PrinterCommand.POS_Set_Cut(1));
                    SendDataByte(PrinterCommand.POS_Set_PrtInit());
                }

            } catch (Exception e) { e.printStackTrace(); }

            pdfr.close();
            pfd.close();
        }
    }
    public void copyFile(FileInputStream inStream, File bfile)
    {

        //InputStream inStream = null;
        OutputStream outStream = null;

        try{

            //inStream = new FileInputStream(afile);
            outStream = new FileOutputStream(bfile);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0){

                outStream.write(buffer, 0, length);

            }

            inStream.close();
            outStream.close();
            toBitmap(bfile);
            System.out.println("File is copied successful!");

        }catch(IOException e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onPrintJobQueued(PrintJob printJob) {
        Log.d("myprinter", "queued: " + printJob.getId().toString());
        appendLog("queued: " + printJob.getId().toString());
        printJob.start();

        final PrintDocument document = printJob.getDocument();
        final PrintDocumentInfo info = document.getInfo();

        final FileInputStream in = new FileInputStream(document.getData().getFileDescriptor());
        appendLog("Info "+info.getName()+" "+info.getContentType()+ " "+ info.getDataSize());
        try {
            File sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            //File mFolder = new File(sdDir, "print");
            copyFile(in, new File(sdDir, "small.pdf"));
            //SendDataString("Print after generate bitmap");
            final byte[] buffer = new byte[4];
            @SuppressWarnings("unused")
            final int read = in.read(buffer);
            //appendLog("first " + buffer.length + "bytes of content: " + toString(buffer));

            //Log.d("myprinter", "first " + buffer.length + "bytes of content: " + toString(buffer));
        } catch (IOException e) {
            Log.d("myprinter", "", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                assert true;
            }
        }
        appendLog("printjob complete");
        printJob.complete();
    }

    private static String toString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Byte.toString(b)).append(',');
        }
        if (sb.length() != 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        Log.d("myprinter", "canceled: " + printJob.getId().toString());
        appendLog("Cancel");
        printJob.cancel();
    }

    public static void appendLog(String text)
    {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String filename = sdDir.getPath() + File.separator + "print.txt";
        File logFile = new File(filename);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = dateFormat.format(new Date());

            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(date+" "+text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class ThermalPrinterDiscoverySession extends PrinterDiscoverySession {

    private PrinterInfo printerInfo;


    ThermalPrinterDiscoverySession(PrinterInfo printerInfo) {
        PrinterCapabilitiesInfo capabilities =
                new PrinterCapabilitiesInfo.Builder(printerInfo.getId())
                        .addMediaSize(PrintAttributes.MediaSize.ISO_A7, true)
                        //.addMediaSize(new PrintAttributes.MediaSize("na_58mm_roll", "58mm Roll", 2300,7000), true)
                        .addResolution(new PrintAttributes.Resolution("1234","Default",200,200), true)
                        .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_MONOCHROME)
                        .build();
        this.printerInfo = new PrinterInfo.Builder(printerInfo)
                .setCapabilities(capabilities)
                .build();
    }

    @Override
    public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
        printers.add(printerInfo);
        addPrinters(printers);
        MyPrintService.appendLog("add printer");
    }

    @Override
    public void onStopPrinterDiscovery() {

    }

    @Override
    public void onValidatePrinters(List<PrinterId> printerIds) {

    }

    @Override
    public void onStartPrinterStateTracking(PrinterId printerId) {

    }

    @Override
    public void onStopPrinterStateTracking(PrinterId printerId) {

    }

    @Override
    public void onDestroy() {

    }
}
