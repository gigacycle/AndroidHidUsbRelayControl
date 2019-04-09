package gigacycle.usbrelaycontrol;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    UsbDevice[] UsbRelayModules = null;
    static EditText et;
    Spinner spRelayModules;
    Switch swRelay1, swRelay2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swRelay1 = (Switch) findViewById(R.id.swRelay1);
        swRelay2 = (Switch) findViewById(R.id.swRelay2);
        spRelayModules = (Spinner) findViewById(R.id.spRelays);
        Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
        et = (EditText) findViewById(R.id.etLog);
        et.setFocusable(false);

        refreshUsbPorts(spRelayModules);

        swRelay1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnOffRelay(UsbRelayModules[spRelayModules.getSelectedItemPosition()], 1, swRelay1.isChecked());
            }
        });
        swRelay2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnOffRelay(UsbRelayModules[spRelayModules.getSelectedItemPosition()], 2, swRelay2.isChecked());
            }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et.setText("");
                refreshUsbPorts(spRelayModules);
            }
        });
    }

    private static void log(String msg){
        et.setText(et.getText() + "\n" + msg);
    }

    private void refreshUsbPorts(Spinner spRelayModules){
        if (UsbRelayModules != null)
            UsbRelayModules = null;
        et.setText("");
        try {
            UsbRelayModules = getUsbRelaysArray();
            if ((UsbRelayModules != null))
            {
                List<String> relaysList = new ArrayList<>();
                for (UsbDevice usbHidDev:UsbRelayModules) {
                    relaysList.add(String.valueOf(usbHidDev.getDeviceId()));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, relaysList);
                spRelayModules.setAdapter(adapter);
                if (adapter.getCount() > 0) {
                    spRelayModules.setSelection(0);
                    swRelay1.setEnabled(true);
                    swRelay2.setEnabled(true);
                }
            }
        } catch (Exception e) {
            log(e.getStackTrace().toString());
        }

        if (UsbRelayModules == null || UsbRelayModules.length == 0)
        {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"None"});
            spRelayModules.setAdapter(adapter);
            swRelay1.setEnabled(false);
            swRelay2.setEnabled(false);
        }
    }

    private void turnOnOffRelay(UsbDevice device, int relayNum, boolean onOff) {
        try {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection connection = manager.openDevice(device);
            if (connection == null || !connection.claimInterface(device.getInterface(0), true)) {
                log("No connection to this device!");
                return;
            }
            byte[] data = new byte[8];
            Arrays.fill(data, (byte) 0);
            data[0] = onOff ? (byte) (255) : (byte) (253);
            data[1] = (byte) relayNum;
            sendCommand(data, 0, data.length, 20, connection, device.getInterface(0).getEndpoint(0));
        }
        catch (Exception e){
            log("Error >> " + e.getMessage());
        }
    }

    public void sendCommand(byte[] data, int offset, int size, int timeout, UsbDeviceConnection connection, UsbEndpoint endPoint) {
        if (offset != 0) {
            data = Arrays.copyOfRange(data, offset, size);
        }
        if (endPoint == null) {
            log("command not executed!");
        } else {
            connection.controlTransfer(0x21, 0x09, 0x0300, 0x00, data, size, timeout);
        }
    }

    private UsbDevice[] getUsbRelaysArray() {
        List<UsbDevice> relays = null;
        UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
        if (manager == null)
            return null;
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (relays == null)
                relays = new ArrayList<>();
            if (device.getVendorId() == 0x16C0 && device.getProductId() == 0x05DF)
                relays.add(device);
        }

        if (relays == null || relays.size() == 0)
            return null;
        else
            return relays.toArray(new UsbDevice[relays.size()]);
    }
}
