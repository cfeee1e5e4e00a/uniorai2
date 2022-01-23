package nerlihmax.uniorai2;

import android.bluetooth.*;
import android.content.Intent;
import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.EventDispatcher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import nerlihmax.uniorai2.SensorProcessors;

class UniorGattCallback extends BluetoothGattCallback {
    private final UUID dataServiceUUID = UUID.fromString("e54eeef0-1980-11ea-836a-2e728ce88125");
    private final UUID startCharacteristicUUID = UUID.fromString("e54e0002-1980-11ea-836a-2e728ce88125");
    private final UUID dataCharacteristicUUID = UUID.fromString("e54eeef1-1980-11ea-836a-2e728ce88125");
    private final UUID dataCharacteristicDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final byte[] startCommand = { 0x53, 0x54, 0x41, 0x52, 0x54, 0x00 };

    SensorProcessors.EEG processor = new SensorProcessors.EEG();

    UniorDevice component;

    public UniorGattCallback(UniorDevice component) {
        this.component = component;
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        BluetoothGattService gattService = gatt.getService(dataServiceUUID);

        BluetoothGattCharacteristic dataCharacteristic = gattService.getCharacteristic(dataCharacteristicUUID);
        Log.d("unior", gatt.setCharacteristicNotification(dataCharacteristic, true) ? "Sucksess" : "Fack");

        for (BluetoothGattDescriptor descriptor : dataCharacteristic.getDescriptors()) {
            Log.d("unior", descriptor.getUuid().toString());
        }

        BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(dataCharacteristicDescriptorUUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        /* ПУКАТЬ ТУТ */
        byte[] packet = characteristic.getValue();

        float[] values = new float[27];
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(values);
        values = Arrays.copyOfRange(values, 2, 27);

        for (float value : values) {
            Log.d("unior", "onCharacteristicChanged");
            component.OnDataReceived(0, processor.process(value));
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d("unior", "gatt callback - onServicesDiscovered");

        BluetoothGattService gattService = gatt.getService(dataServiceUUID);

        BluetoothGattCharacteristic startCharacteristic = gattService.getCharacteristic(startCharacteristicUUID);
        startCharacteristic.setValue(startCommand);
        gatt.writeCharacteristic(startCharacteristic);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d("unior", "gatt callback - onConnectionStateChange - connected");

            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d("unior", "gatt callback - disconnected");
        }
    }
}

@DesignerComponent(
    version = YaVersion.LABEL_COMPONENT_VERSION,
    category = ComponentCategory.CONNECTIVITY,
    description = "ПАК-Юниор устройство",
    nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
    permissionNames =
        "android.permission.BLUETOOTH," +
        "android.permission.BLUETOOTH_ADMIN," +
        "android.permission.ACCESS_FINE_LOCATION"
)
public class UniorDevice extends AndroidNonvisibleComponent implements Component {
    private final BluetoothAdapter bluetoothAdapter;
    private String macAddress;

    public UniorDevice(Form form) {
        super(form);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            form.startActivityForResult(enableBluetoothIntent, 1);
        }
    }

    @SimpleFunction(description = "Подключает к устройству")
    public void Connect() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        device.connectGatt(form, false, new UniorGattCallback(this));
        Log.d("unior", "Connect");
    }

    @SimpleEvent(description = "Вызывается при получении данных")
    public void OnDataReceived(float timestamp, float value) {
        Log.d("unior", "OnDataReceived " + value);
        EventDispatcher.dispatchEvent(this, "OnDataReceived", timestamp, value);
    }

    @SimpleProperty(description = "MAC-адрес устройтства")
    @DesignerProperty()
    public void MacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @SimpleProperty(description = "MAC-адрес устройтства")
    public String MacAddress() {
        return this.macAddress;
    }
}
