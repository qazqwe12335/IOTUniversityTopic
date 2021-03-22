package com.thematic.protection_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    MqttAndroidClient client;

    //訂閱的主題
    String Sub_wifi_topic = "monitor_vulru/4w/61998/sensor/Power/thematic";
    String Sub_fingerprint_topic = "monitor_vulru/4w/61998/sensor/Detect/thematic";
    String MqttHost = "tcp://test.mosquitto.org:1883";

    String dialog_msg;

    //
    String[] Sub_topic = new String[]{Sub_wifi_topic, Sub_fingerprint_topic};
    int Sub_qos[] = {1, 1};

    private int nextDrawableId = R.drawable.fingerprint_on;
    String fingerprint = "off";

    CircleImageView finger_print_img_btn;
    Switch wifi_status_switch;
    TextView status_txv;

    int a = 1;

    private int rotation = 180;

    AlertDialog.Builder alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    //基本介面配置
    private void init() {
        status_txv = findViewById(R.id.status_txv);

        wifi_status_switch = findViewById(R.id.wifi_status);
        wifi_status_switch.setOnCheckedChangeListener(this);
        wifi_status_switch.setEnabled(false);

        finger_print_img_btn = findViewById(R.id.finger_img_btn);
        finger_print_img_btn.setOnClickListener(this);

        mqtt_connect_to();

        //接收回傳的值
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                int arrived_msg = Integer.valueOf(String.valueOf(message));
                a = 0;

                Log.e("mqtt_arrived", topic + String.valueOf(arrived_msg));

                //先判斷收到的訊息在判斷主題
                if (arrived_msg == 1) {
                    if (topic.equals(Sub_wifi_topic)) {

                        wifi_status_switch.setChecked(true);
                        wifi_status_switch.setEnabled(true);
                        status_txv.setText(R.string.rpi_wifi_status_message_on);

                    } else if (topic.equals(Sub_fingerprint_topic)) {

                        finger_print_img_btn.setImageResource(R.drawable.fingerprint_on);
                        finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_on_background));
                        fingerprint = "on";
                        //finger_print_img_btn.setEnabled(false);

                    }
                } else if (arrived_msg == 0) {
                    if (topic.equals(Sub_wifi_topic)) {

                        wifi_status_switch.setChecked(false);
                        wifi_status_switch.setEnabled(false);
                        status_txv.setText(R.string.rpi_wifi_status_message_off);

                    } else if (topic.equals(Sub_fingerprint_topic)) {

                        finger_print_img_btn.setImageResource(R.drawable.fingerprint_off);
                        finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_off_background));
                        fingerprint = "off";
                        finger_print_img_btn.setEnabled(true);

                    }
                }
                a = 1;
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    //MQTT連線 ，會在 init 被呼叫
    private void mqtt_connect_to() {

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MqttHost, clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("Mqtt_connect", "onSuccess");
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("Mqtt_connect", "onFailure");

                }
            });
        } catch (MqttException e) {
            Log.e("MQTT_CON", e.toString());
        } catch (NullPointerException n) {
            n.printStackTrace();
        }
    }

    //訂閱  MQTT連現成功時呼叫
    private void sub() {
        try {
            //IMqttToken subToken = client.subscribe(Sub_topic, qos);
            IMqttToken subToken = client.subscribe(Sub_topic, Sub_qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("mqtt_sub", "success");
                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //錄影機的點擊事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.finger_img_btn:
                check_fingerprint_status();
                touch_animation();
                finger_print_img_btn.setEnabled(true);
                break;
        }
    }
    //錄影機點擊事件判斷狀態(圖片顏色等等)
    private void check_fingerprint_status() {
        a = 1;
        if (a == 1) {
            if (fingerprint.equals("on")) {
                nextDrawableId = R.drawable.fingerprint_off;
                finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_off_background));
                fingerprint = "off";
                dialog_msg = getResources().getString(R.string.dialog_msg_fingerprint_off);
                btn_dialog_msg(dialog_msg, true);
            } else {
                nextDrawableId = R.drawable.fingerprint_on;
                finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_on_background));
                fingerprint = "on";
                dialog_msg = getResources().getString(R.string.dialog_msg_fingerprint);
                btn_dialog_msg(dialog_msg, true);
            }
        }
    }

    //錄影機按鈕動畫及改變顏色
    private void touch_animation() {
        finger_print_img_btn.setEnabled(false);
        finger_print_img_btn.setElevation(3);
        finger_print_img_btn.animate()
                //.rotationBy(rotation)        // rest 180 covered by "shrink" animation
                .setDuration(75)
                .scaleX(1.1f)           //Scaling to 110%
                .scaleY(1.1f)           //Scaling to 110%
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        //Chaning the icon by the end of animation
                        finger_print_img_btn.setImageResource(nextDrawableId);
                        finger_print_img_btn.setElevation(8);
                        finger_print_img_btn.animate()
                                //.rotationBy(rotation)   //Complete the rest of the rotation
                                .setDuration(75)
                                .scaleX(1)              //Scaling back to what it was
                                .scaleY(1)
                                .start();

                        rotation = -rotation;
                    }
                })
                .start();
    }
    //switch開關
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.wifi_status:
                if (!b) {
                    dialog_msg = getResources().getString(R.string.dialog_msg_power);
                    btn_dialog_msg(dialog_msg, false);
                } else {
                    pub(Sub_wifi_topic, "1");
                }
                break;
        }
    }
    //發布函式
    private void pub(String topic, String send_msg) {
        try {
            client.publish(topic, send_msg.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //dialog通知
    private void btn_dialog_msg(String msg, final boolean sensor) {
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("確認")
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (sensor) {
                            if (fingerprint.equals("on")) {
                                pub(Sub_fingerprint_topic, "1");
                            } else {
                                pub(Sub_fingerprint_topic, "0");
                            }
                        } else {
                            pub(Sub_wifi_topic, "0");
                        }
                    }
                })
                .setNegativeButton("Cannel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (sensor) {
                            if (fingerprint.equals("on")) {
                                nextDrawableId = R.drawable.fingerprint_off;
                                finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_off_background));
                                finger_print_img_btn.setImageResource(nextDrawableId);
                                fingerprint = "off";
                            }else{
                                nextDrawableId = R.drawable.fingerprint_on;
                                finger_print_img_btn.setBorderColor(getResources().getColor(R.color.circle_on_background));
                                finger_print_img_btn.setImageResource(nextDrawableId);
                                fingerprint = "on";
                            }
                        } else {
                            wifi_status_switch.setChecked(true);
                        }
                    }
                })
                .show();
    }
}