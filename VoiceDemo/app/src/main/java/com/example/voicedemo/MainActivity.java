package com.example.voicedemo;

import java.util.Random;
import java.util.TimerTask;
import java.util.Timer;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.EditText;

import com.modules.Common;
import com.modules.SinVoicePlayer;
import com.modules.SinVoiceRecognition;
import com.modules.GenBinaryText;

public class MainActivity extends Activity implements SinVoiceRecognition.Listener, SinVoicePlayer.Listener {
    private final static String TAG = "MainActivity";

    private String initText = null;
    private String binaryText = null;

    private final static int Text = 1;
    private final static int Start = 2;
    private final static int End = 3;
    private final static int handIn = 1;
    private final static int randIn = 2;
    private int inputMode = 0;

    private Handler myHanlder;
    private SinVoicePlayer mySinVoicePlayer;
    private SinVoiceRecognition myRecognition;
    private GenBinaryText myGenBinaryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySinVoicePlayer = new SinVoicePlayer();
        mySinVoicePlayer.setListener(this);
        myRecognition = new SinVoiceRecognition();
        myRecognition.setListener(this);

        final TextView playTextView = findViewById(R.id.playText);
        final TextView recgTextView = findViewById(R.id.regText);
        final TextView realTextView = findViewById(R.id.realData);
        final EditText inputEditText = findViewById(R.id.inputText);
        myHanlder = new RegHandler(recgTextView);

        Button playStart = this.findViewById(R.id.startPlay);
        playStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                myRecognition.stop();
                Common.ACK = 0;
                Common.SEND = 1;

                myGenBinaryText = new GenBinaryText();
                initText = null;
                binaryText = null;

                if (inputMode == handIn) {
                    initText = inputEditText.getText().toString();
                    binaryText = myGenBinaryText.convertTextToBinary(initText);
                }
                if (inputMode == randIn) {
                    StringBuilder text_string = new StringBuilder();
                    int count = Common.defaultCount;

                    while (count > 0) {
                        Random rand = new Random();
                        int number_int = rand.nextInt(Common.maxRange) + 33;
                        char number_char=(char)number_int;

                        text_string.append(number_char);
                        count--;
                    }

                    initText = text_string.toString();
                    binaryText = myGenBinaryText.convertTextToBinary(initText);
                }

                playTextView.setText(initText);
                realTextView.setText(binaryText);
                mySinVoicePlayer.play(binaryText);
            }
        });

        Button playStop = this.findViewById(R.id.stopPlay);
        playStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mySinVoicePlayer.stop();
                myRecognition.stop();
                Common.ACK = 1;
                Common.SEND = 0;
            }
        });

        Button recgStart = this.findViewById(R.id.startReg);
        recgStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                myRecognition.start();
            }
        });

        Button recgStop = this.findViewById(R.id.stopReg);
        recgStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                myRecognition.stop();
            }
        });

        RadioGroup modeRadioGroup = findViewById(R.id.radioGroup);
        modeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton RB = findViewById(i);
                Log.d(TAG,"Your choice is：" + RB.getText());

                final RadioButton handButton = findViewById(R.id.handIn);
                final RadioButton randButton = findViewById(R.id.randIn);

                if (handButton.isChecked()) {
                    playTextView.setVisibility(View.INVISIBLE);
                    inputEditText.setVisibility(View.VISIBLE);

                    inputMode = handIn;
                }
                if (randButton.isChecked()) {
                    playTextView.setVisibility(View.VISIBLE);
                    inputEditText.setVisibility(View.INVISIBLE);

                    inputMode = randIn;
                }
            }
        });
    }


    private static class RegHandler extends Handler {
        private StringBuilder myStringBuilder = new StringBuilder();
        private TextView myTextView;

        private RegHandler(TextView textView) {
            myTextView = textView;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Start:
                    myStringBuilder.delete(0, myStringBuilder.length());
                    Log.d(TAG,"recognition start");
                    break;

                case Text:
                    char ch = (char) msg.arg1;
                    myStringBuilder.append(ch);

                    if (myTextView != null) {
                        myTextView.setText(myStringBuilder.toString());
                    }
                    break;

                case End:
                    myStringBuilder.delete(0, myStringBuilder.length());
                    Log.d(TAG,"recognition end");
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onPlayStart() {
        Log.d(TAG, "start play");
    }
    @Override
    public void onPlayEnd() {
        Log.d(TAG, "stop play");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (Common.ACK == 0 && Common.SEND == 1){
                    myRecognition.stop();
                    mySinVoicePlayer.play(binaryText);
                }
            }
        };
        Timer myTimer = new Timer();

        myRecognition.start();

        myTimer.schedule(task, 8000);
    }

    @Override
    public void onRecognitionStart() {
        myHanlder.sendEmptyMessage(Start);
    }
    @Override
    public void onRecognition(char ch) {
        myHanlder.sendMessage(myHanlder.obtainMessage(Text, ch, 0));
    }
    @Override
    public void onRecognitionEnd() {
        myHanlder.sendEmptyMessage(End);
    }
}
