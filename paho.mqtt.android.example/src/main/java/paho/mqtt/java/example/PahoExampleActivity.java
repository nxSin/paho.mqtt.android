/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package paho.mqtt.java.example;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class PahoExampleActivity extends AppCompatActivity {
    private static final String TAG = PahoExampleActivity.class.getSimpleName();
    private HistoryAdapter mAdapter;

    MqttAndroidClient mqttAndroidClient;

    //    final String serverUri = "tcp://iot.eclipse.org:1883";
    final String serverUri = "tcp://120.79.77.146:61613";

    String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "123_345";
    final String publishTopic = "publish_1";
    final String publishMessage = "Hello World!";
    private static String userName = "admin";
    private static String passWord = "123654";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage();
            }
        });


        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        //客户端ID，这里示例为了是的每次都是不一样的客户端去连接所以用了时间
//        clientId = clientId + System.currentTimeMillis();
        initConnect();


    }

        private void initConnect() {
            //封装好的MQTTClient供操作，发送和接手等设置都用它
            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
            //设置回调
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {

                /**
                 * 连接完成回调
                 * @param reconnect true 断开重连,false 首次连接
                 * @param serverURI 服务器URI
                 */
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {

                    if (reconnect) {
                        addToHistory("Reconnected to : " + serverURI);
                        // Because Clean Session is true, we need to re-subscribe
                        subscribeToTopic();
                    } else {
                        addToHistory("Connected to: " + serverURI);
                    }
                }

                /**
                 * @desc 连接断开回调
                 * 可在这里做一些重连等操作
                 */
                @Override
                public void connectionLost(Throwable cause) {
                    addToHistory("The Connection was lost.");
                }

                /**
                 * 消息接收，如果在订阅的时候没有设置IMqttMessageListener，那么收到消息则会在这里回调。
                 * 如果设置了IMqttMessageListener，则消息回调在IMqttMessageListener中
                 * @param topic 该消息来自的订阅主题
                 * @param message 消息内容
                 */
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    addToHistory("Incoming message: " + new String(message.getPayload()));
                }

                /**
                 * 交付完成回调。在publish消息的时候会收到此回调.
                 * qos:
                 * 0 发送完则回调
                 * 1 或 2 会在对方收到时候回调
                 * @param token
                 */
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    try {
                        addToHistory("deliveryComplete,token:" + token.getMessage().toString());
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });

            //mqtt连接参数设置
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            //设置自动重连
            mqttConnectOptions.setAutomaticReconnect(true);
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录
            // 这里设置为true表示每次连接到服务器都以新的身份连接
            mqttConnectOptions.setCleanSession(false);
            //设置连接的用户名
            mqttConnectOptions.setUserName(userName);
            //设置连接的密码
            mqttConnectOptions.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            mqttConnectOptions.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            mqttConnectOptions.setKeepAliveInterval(20);
            try {
                //设置好相关参数后，开始连接
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(TAG, "onSuccess,asyncActionToken");
                        /*连接成功之后设置连接断开的缓冲配置*/
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        //开启
                        disconnectedBufferOptions.setBufferEnabled(true);
                        //离线后最多缓存100调
                        disconnectedBufferOptions.setBufferSize(100);
                        //不一直持续留存
                        disconnectedBufferOptions.setPersistBuffer(false);
                        //删除旧消息
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        //订阅主题
                        subscribeToTopic();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        addToHistory("Failed to connect to: " + serverUri);
                    }
                });


            } catch (MqttException ex) {
                ex.printStackTrace();
            }
        }

    private void addToHistory(String mainText) {
        System.out.println("LOG: " + mainText);
        mAdapter.add(mainText);
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    /**
     * 订阅主题
     */
    public void subscribeToTopic() {
        try {
            //主题、QOS、context,订阅监听，消息监听
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            }, new IMqttMessageListener() {
                /**
                 * @desc 消息到达回调
                 * @param topic
                 * @param message
                 */
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // message Arrived!
                    final String messageRecive = new String(message.getPayload());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addToHistory("recive message:" + messageRecive);
                        }
                    });
                    System.out.println("Message: " + topic + " : " + messageRecive);

                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    /**
     * 发送消息
     */
    public void publishMessage() {

        try {
            //消息封装
            MqttMessage message = new MqttMessage();
            //设置要发送的消息
            message.setPayload(publishMessage.getBytes());
            //开始发送
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published");
            if (!mqttAndroidClient.isConnected()) {
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
