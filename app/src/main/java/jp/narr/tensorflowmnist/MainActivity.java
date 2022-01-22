/*
   Copyright 2016 Narrative Nights Inc. All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package jp.narr.tensorflowmnist;

import android.graphics.PointF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import java.util.Locale;
import java.util.Stack;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener{
    private static final String TAG = "MainActivity";
    static {
        System.loadLibrary("tensorflow_inference");
    }
    private TextToSpeech tts;
    private static final String model_file = "file:///android_asset/HW-GraphPB.pb";

    private static final String input_node = "reshape_1_input";
    private static final long[] input_shape = {1,784};
    private static final String output_node = "dense_3/Softmax";

    TensorFlowInferenceInterface inferenceInterface;

    private static final int PIXEL_WIDTH = 28;
    private TextView mResultText;
    private TextView allResultText;
    private TextView allText;
    private float mLastX;
    private float mLastY;
    private DrawModel mModel;
    private DrawView mDrawView;

    private PointF mTmpPiont = new PointF();

    private Stack<String> allwords = new Stack<String>();

    private Stack<String> kalimat = new Stack<String>();

    private ImageView suara_kata;
    private ImageView suara_kalimat;


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(),model_file);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e ("TTS", "This Language not supported");
                    }else {
                        mResultText.setEnabled(true);
                        allResultText.setEnabled(true);
                    }
                } else {
                    Log.e("TTS","Initialization Failed");
                }
            }
        });




        mModel = new DrawModel(PIXEL_WIDTH, PIXEL_WIDTH);

        mDrawView = (DrawView) findViewById(R.id.view_draw);
        mDrawView.setModel(mModel);
        mDrawView.setOnTouchListener(this);

        View detectButton = findViewById(R.id.button_detect);
        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDetectClicked();
            }
        });

        View clearButton = findViewById(R.id.button_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClicked();
            }
        });

        View allButton = findViewById(R.id.button_all);
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAllWords();
            }
        });

        View clearAllButton = findViewById(R.id.clear_all);
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickClearAllWords();
            }
        });

        View kalimatButton = findViewById(R.id.button_kalimat);
        kalimatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickKalimat();
            }
        });

        View clearKalimatButton = findViewById(R.id.clear_kalimat);
        clearKalimatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickClearKalimat();
            }
        });

        mResultText = (TextView)findViewById(R.id.text_result);
        allResultText = (TextView)findViewById(R.id.all_result);
        allText = (TextView)findViewById(R.id.all_text);
        suara_kata = (ImageView)findViewById(R.id.suara_kata);
        suara_kalimat = (ImageView)findViewById(R.id.suara_kalimat);

    }

    @Override
    protected void onResume() {
        mDrawView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mDrawView.onPause();
        super.onPause();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            processTouchDown(event);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }

        return false;
    }

    private void processTouchDown(MotionEvent event) {
        mLastX = event.getX();
        mLastY = event.getY();
        mDrawView.calcPos(mLastX, mLastY, mTmpPiont);
        float lastConvX = mTmpPiont.x;
        float lastConvY = mTmpPiont.y;
        mModel.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mDrawView.calcPos(x, y, mTmpPiont);
        float newConvX = mTmpPiont.x;
        float newConvY = mTmpPiont.y;
        mModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        mDrawView.invalidate();
    }

    private void processTouchUp() {
        mModel.endLine();
    }

    private void onDetectClicked() {
        float pixels[] = mDrawView.getPixelData();
        float[] result = fpre(pixels);
        display(result);


    }
    int index = 99;
    public void display(float[] result){
        String[] ans = {
                "0",
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7",
                "8",
                "9",
                "A",
                "B",
                "C",
                "D",
                "E",
                "F",
                "G",
                "H",
                "I",
                "J",
                "K",
                "L",
                "M",
                "N",
                "O",
                "P",
                "Q",
                "R",
                "S",
                "T",
                "U",
                "V",
                "W",
                "X",
                "Y",
                "Z",
                "a",
                "b",
                "c",
                "d",
                "e",
                "f",
                "g",
                "h",
                "i",
                "j",
                "k",
                "l",
                "m",
                "n",
                "o",
                "p",
                "q",
                "r",
                "s",
                "t",
                "u",
                "v",
                "w",
                "x",
                "y",
                "z"
        };


        int mi = 0;
        float max = 0;
        for (int i =0;i<62;i++){
            if(result[i]>max){
                max = result[i];
                mi = i;
            }
            String mes = "Kemungkinan dari "+i+": "+result[i];
            Log.d("mess",mes);
        }

        if(max>0.50f) {
            String read = ans[mi];
            String con = String.format("%.1f", max * 100);
            String dt = "Deteksi = " + read + " (" + con + "%)";
            mResultText.setText(dt);
            allwords.push(read);

//            tts.speak(read,TextToSpeech.QUEUE_FLUSH,null);
        }
        else{
            String read = ans[mi];
            String con = String.format("%.1f", max * 100);
            String dt = "Mungkin: " + read + " (" + con + "%)";
            mResultText.setText(dt);
            allwords.push(read);

//            tts.speak(read,TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    private float[] fpre(float[] pb){
        inferenceInterface.feed(input_node,pb,input_shape);
        inferenceInterface.run(new String[] {output_node});
        float[] result = new float[62];
        float[] l = new float[620];
        inferenceInterface.fetch(output_node,result);
        return result;
    }

    private void onClickAllWords() {
        suara_kata.setVisibility(View.VISIBLE);
        tts.speak(allwords.toString().replaceAll("\\[", "").replaceAll("]", "").replaceAll(",", "").replaceAll(" ", ""),TextToSpeech.QUEUE_FLUSH,null);
        allResultText.setText(allwords.toString().replaceAll("\\[", "").replaceAll("]", "").replaceAll(",", "").replaceAll(" ", ""));

    }

    private void onClearClicked() {
        mModel.clear();
        mDrawView.reset();
        mDrawView.invalidate();

        mResultText.setText("");
    }

    private void onClickClearAllWords() {
        mModel.clear();
        mDrawView.reset();
        mDrawView.invalidate();
        allwords.clear();
        allResultText.setText("");
        suara_kata.setVisibility(View.INVISIBLE);
    }

    private void onClickClearKalimat() {
        mModel.clear();
        mDrawView.reset();
        mDrawView.invalidate();
        kalimat.clear();
        allText.setText("");
        suara_kalimat.setVisibility(View.INVISIBLE);
    }

    public void onClickKalimat() {
        kalimat.push(allwords + "\b");
        allText.setText(kalimat.toString().replaceAll("\\[", "").replaceAll("]", "").replaceAll(",", "").replaceAll(" ", ""));
        suara_kalimat.setVisibility(View.VISIBLE);
        tts.speak(kalimat.toString().replaceAll("\\[", "").replaceAll("]", "").replaceAll(",", "").replaceAll(" ", ""),TextToSpeech.QUEUE_FLUSH,null);
    }
}
