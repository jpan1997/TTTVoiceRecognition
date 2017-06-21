package jpan1997.berobotvoicerecognition;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.Manifest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView text;
    private ImageView status;
    private ImageButton languageButton;
    private RadioGroup languageOption;
    private Dialog languageMenu;

    private int languageNum;
    private Locale language;

    private int currentVol;

    private Intent listenerIntent, commandIntent;
    private SpeechRecognizer sr;
    private TextToSpeech tts;
    private AudioManager audio;

    private FirebaseDatabase database;
    private Uri contentUri;
    private Context context;
    private String name;

    static final int PICTURE_VIDEO_REQUEST_CODE = 1;

    static final int ACTIVE_LISTENING = 1;
    static final int BACKGROUND_LISTENING = 0;

    static final int DEFAULT_VOLUME = 7;

    static final int DEFAULT_LANGUAGE_ID = R.id.radioEnglish;
    static final String[] KEYWORD = {"hey", "be", "bee", "robot"};
    static final Locale[] languages = {Locale.US, Locale.TRADITIONAL_CHINESE};
    static final int ENGLISH = 0;
    static final int CHINESE = 1;
    static final int DEFAULT_LANGUAGE = ENGLISH;
    static final String[][][] dialogue = {{
            /* 00 */ {")", "Hello. How can I help you?",},
            /* 01 */ {"!", "Say \"Hey BeRobot\""},
            /* 02 */ {"@", "Please try again"},
            /* 03 */ {"#", "I'm not sure what that is. Let me do a google search on"},
            /* 04 */ {"$", "Choose your language"},
            /* 05 */ {"**", "Watch your language"},
            /* 06 */ {"take a picture", "OK"},
            /* 07 */ {"take a video", "OK"},
            /* 08 */ {"louder", "OK"},
            /* 09 */ {"softer", "OK"},
            /* 10 */ {"brighter", "OK"},
            /* 11 */ {"dimmer", "OK"},
            /* 12 */ {"what time is it", "It is currently"},
            /* 13 */ {"today's date", "Today is"},
            /* 14 */ {"change to chinese", "Changing to Chinese", R.id.radioChinese + ""},
            /* 15 */ {"what is your name", "My name is", "What is yours?"},
            /* 16 */ {"how are you", "I'm very good. What about you?"},
            /* 17 */ {"what is my name", "", "Hmm I don't know. What is your name?", "Your name is "},

            {"who are you", "I am Be Robot. I can help you do many things."},
            {"i'm not good", "That's too bad"},
            {"bye bye", "See you next time"},
            {"thank", "You're welcome"}


    }, {
            /* 00 */ {")", "你好,我能幫你什麼嗎?"},
            /* 01*/ {"!", "說 \"Hey BeRobot\""},
            /* 02 */ {"@", "請再說一遍"},
            /* 03 */ {"#", "我不曉得那是什麼. 我用谷歌幫你找"},
            /* 04 */ {"$", "選語言"},
            /* 05 */ {"**", "不可以說髒話"},
            /* 06 */ {"照相", "好的"},
            /* 07 */ {"錄影", "好的"},
            /* 08 */ {"大聲一點", "好的"},
            /* 09 */ {"小聲一點", "好的"},
            /* 10 */ {"亮一點", "好的"},
            /* 11 */ {"暗一點", "好的"},
            /* 12 */ {"現在幾點", "現在是"},
            /* 13 */ {"幾月幾號", "今天是"},
            /* 14 */ {"成英文", "語言換成英文", R.id.radioEnglish + ""},
            /* 15 */ {"你叫什麼名字", "我叫", "你呢?"},
            /* 16 */ {"你好嗎", "我很好．你呢?"},
            /* 17 */ {"我叫什麼名字", "", "我不知道. 你叫什麼名字?", "你叫"},

            {"你是誰", "我是極趣公司的機器人.我可以幫你做很多事"},
            {"我不好", "太可惜了"},
            {"拜拜", "再見"},
            {"謝謝", "不客氣"}
    }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize context (used by anonymous inner classes)
        context = this;

        // initialize all page elements
        text = (TextView) findViewById(R.id.text);
        status = (ImageView) findViewById(R.id.status);
        languageButton = (ImageButton) findViewById(R.id.language_button);

        languageMenu = new Dialog(this);
        languageMenu.requestWindowFeature(Window.FEATURE_NO_TITLE);
        languageMenu.setContentView(R.layout.language_menu);
        languageMenu.setCancelable(true);


        // initialize language, language radio button event listener to set language to desired language
        language = languages[DEFAULT_LANGUAGE];
        languageNum = DEFAULT_LANGUAGE;
        languageOption = (RadioGroup) languageMenu.findViewById(R.id.radioLang);
        languageOption.check(DEFAULT_LANGUAGE_ID);
        languageOption.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioChinese) {
                    language = languages[1];
                    languageNum = CHINESE;
                    tts.setLanguage(languages[1]);
                } else {
                    language = languages[0];
                    languageNum = ENGLISH;
                    tts.setLanguage(languages[0]);
                }
            }
        });

        // initialize text to speech service
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    int result = tts.setLanguage(language);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization Failed!");
                }
            }
        });

        // initialize language settings button listener
        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLanguageMenu();
            }
        });

        // initialize audio
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        muteAudio();

        // check permissions were granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA},
                    10);
        }

        // start listening
        getSpeechInput();
    }

    // called when current page is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroySpeechRecognizer();
        tts.shutdown();
    }

    // reset everything and start background speech recognizer
    public void getSpeechInput() {
        status.setImageResource(R.drawable.sleeping);
        resetListenerIntent();
        destroySpeechRecognizer();
        createSpeechRecognizer(BACKGROUND_LISTENING);
        startSpeechRecognizer(BACKGROUND_LISTENING);
        text.setText(dialogue[languageNum][1][1]);
        text.setTextColor(Color.GRAY);
    }

    // receive command from user (not using background service)
    private void receiveCommand() {
        status.setImageResource(R.drawable.awake);
        resetCommandIntent();
        destroySpeechRecognizer();
        createSpeechRecognizer(ACTIVE_LISTENING);
        startSpeechRecognizer(ACTIVE_LISTENING);
    }

    // reset listenerIntent to be used by background speech recognizer
    private void resetListenerIntent() {
        listenerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // english is used to detect "hey be robot"
        listenerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        listenerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, Locale.US.toString());
    }

    // reset commandIntent to be used to receive user commands
    private void resetCommandIntent() {
        commandIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        commandIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        commandIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toString());
        commandIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language.toString());
    }

    // create speech recognizer
    private void createSpeechRecognizer(final int type) {
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {

            final private String TAG = "Voice Recognition";
            // used for timing purposes
            private long start, stop;
            private long speechLength;

            public void onReadyForSpeech(Bundle params) { Log.d(TAG, "Ready for Speech"); }

            public void onBeginningOfSpeech() {
                Log.d(TAG, "Beginning Of Speech");
                start = System.nanoTime();
            }

            public void onRmsChanged(float rmsdB) {}

            public void onBufferReceived(byte[] buffer) { Log.d(TAG, "Buffer Received"); }

            public void onEndOfSpeech() {
                Log.d(TAG, "End of Speech");
                if (type == ACTIVE_LISTENING) {
                    status.setImageResource(R.drawable.thinking);
                }
                stop = System.nanoTime();
                speechLength = (stop-start)/1000000;
                start = System.nanoTime();
            }

            public void onError(int error) {
                Log.d(TAG, "Error " + error);

                // if speech detected but unable to parse, prompt user again
                if (error == SpeechRecognizer.ERROR_NO_MATCH && type == ACTIVE_LISTENING) {
                    speak(dialogue[languageNum][2][1]);
                    receiveCommand();

                // error during sleeping mode, stay in sleep mode
                } else {
                    getSpeechInput();
                }
            }

            public void onResults(Bundle results) {
                Log.d(TAG, "Results " + results);

                stop = System.nanoTime();
                Log.d("Time", "Length " + speechLength + "ms | Processing " + (stop-start)/1000000 + "ms");

                // get first result
                ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("RESULT", result.get(0));
                // background listening
                if (type == BACKGROUND_LISTENING) {
                    // check if result has the keyword
                    if (keywordDetected(result.get(0))) {
                        speak(dialogue[languageNum][0][1]);
                        receiveCommand();

                    // keep listening if keyword not heard
                    } else {
                        getSpeechInput();
                    }

                // active listening
                } else {
                    // if keyword detected, restart active listening
                    if (keywordDetected(result.get(0))) {
                        speak(dialogue[languageNum][0][1]);
                        receiveCommand();

                    // else process user command
                    } else {
                        text.setText(result.get(0));
                        text.setTextColor(ContextCompat.getColor(context, R.color.beRobotBlue));
                        processCommand(result.get(0));
                    }
                }
            }

            public void onPartialResults(Bundle partialResults) { Log.d(TAG, "Partial Results"); }

            public void onEvent(int eventType, Bundle params) { Log.d(TAG, "onEvent " + eventType); }
        });
    }

    // called when command listener receives speech from user
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // picture / video
            case PICTURE_VIDEO_REQUEST_CODE :
                // call media scanner to put pic/video in the android gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                getSpeechInput();
        }
    }

    // check for keyword
    private boolean keywordDetected(String command) {
        command = command.toLowerCase();
        boolean result = false;
        for(int i = 0; i < KEYWORD.length; i++) {
            if(command.contains(KEYWORD[i])) {
                text.setText("");
                return true;
            }
        }

        return false;
    }

    // process commands from user
    private void processCommand(String command) {
        int convoLogic = 0; // 0 = go back to sleep, 1 = receive another command, 2 = google/firebase (pause)
        int index = -1;
        command = command.toLowerCase();

        long start = System.nanoTime();

        // search for matching command in table
        for (int i = 0; i < dialogue[languageNum].length; i++) {
            if (command.contains(dialogue[languageNum][i][0])) {
                long stop = System.nanoTime();
                Log.d("Time", "Lookup Table " + (stop-start)/1000000 + " ms");
                speak(dialogue[languageNum][i][1]);
                index = i;
            }
        }

        // special commands
        switch (index) {
            case 5:    // curse
                convoLogic = 1;
                break;
            case 6:    // take photo
                takePictureVideo(0);
                convoLogic = 2;
                break;
            case 7:
                takePictureVideo(1);
                convoLogic = 2;
                break;
            case 8:
                currentVol += 5;
                break;
            case 9:
                currentVol -= 5;
                break;
            case 10:
                adjustBrightness(1);
                break;
            case 11:
                adjustBrightness(0);
                break;
            case 12:    // time
                speak(getCurrentTime());
                break;
            case 13:    // date
                speak(getDate());
                break;
            case 14:     // change language
                languageOption.check(Integer.valueOf(dialogue[languageNum][index][2]));
                break;
            case 15:     // what is your name
                Locale currLang = languages[languageNum];
                tts.setLanguage(Locale.US);
                speak("Be Robot");
                tts.setLanguage(currLang);
                speak(dialogue[languageNum][index][2]);
                convoLogic = 1;
                break;
            case 16:     // how are you
                convoLogic = 1;
                break;
            case 17:    // what is my name
                if(name == null) {
                    speak(dialogue[languageNum][index][2]);
                    convoLogic = 1;
                } else {
                    speak(dialogue[languageNum][index][3] + name);
                }
                break;
            case -1:
                if (!parseIntroduction(command)) {
                    convoLogic = 2;

                    final long start2 = System.nanoTime();

                    command = command.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "");

                    // look up command in firebase database
                    database = FirebaseDatabase.getInstance();
                    final DatabaseReference ref = database.getReference(languageNum + "/" + command);
                    final String googleQuery = command;
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            long stop = System.nanoTime();
                            Log.d("Time", "Firebase " + (stop-start2)/1000000 + "ms");
                            // command not found in firebase, conduct google search
                            if (snapshot.getValue() == null) {
                                googleSearch(googleQuery);
                            // command found in firebase, say response
                            } else {
                                speak(snapshot.getValue().toString());
                            }
                            ref.goOffline();
                            database.goOffline();
                            // to back to sleep mode
                            getSpeechInput();
                        }
                        @Override
                        public void onCancelled(DatabaseError e) {
                            Log.e("Firebase", "Unable to retrieve data");
                        }
                    });
                }
        }

        // choose to sleep, listen or pause
        if(convoLogic == 0) {
            getSpeechInput();
        } else if(convoLogic == 1) {
            receiveCommand();
        }

        // if user curses, change emoji to angry, recieve another command
        if(index == 14) {
            status.setImageResource(R.drawable.angry);
        }
    }

    // start speech recognizer with listener intent
    private void startSpeechRecognizer(int type) {
        if (sr != null) {
            sr.startListening((type == BACKGROUND_LISTENING) ? listenerIntent : commandIntent);
        }
    }

    // completely stop and destroy speech recognizer
    private void destroySpeechRecognizer() {
        if (sr != null) {
            sr.destroy();
        }
    }

    // speak desired text, handle all audio adjustments
    private void speak(String str) {
        status.setImageResource(R.drawable.awake);
        restoreAudio();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(str, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
        }

        while(tts.isSpeaking());
        muteAudio();
    }

    // mute audio
    private void muteAudio() {
        currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        // make sure currentVol is never 0
        if (currentVol == 0) {
            currentVol = DEFAULT_VOLUME;
        }
    }

    // restore audio
    private void restoreAudio() {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    // process self introduction from user
    private boolean parseIntroduction(String command) {
        // English
        if (languageNum == ENGLISH) {
            if (command.contains("my name is")) {
                speak("Nice to meet you" + command.substring(command.lastIndexOf(" ")));
                name = command.substring(command.lastIndexOf(" "));
                return true;
            }
            // Chinese
        } else {
            if (command.contains("我") && (command.contains("叫") || command.contains("名字"))) {
                int nameIndex = (command.contains("叫")) ? command.indexOf("叫") : command.indexOf("是");
                speak("很高興認識你," + command.substring(nameIndex + 1));
                name = command.substring(nameIndex + 1);
                return true;
            }
        }

        return false;
    }

    // start google search activity
    private void googleSearch(String command) {
        speak(dialogue[languageNum][3][1]+command);
        Uri uri = Uri.parse("http://www.google.com/#q=" + command);
        Intent search = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(search);
    }

    // get current time string to feed into tts
    private String getCurrentTime() {
        String time = new SimpleDateFormat("hh mm aa").format(new Date());
        if(languageNum == CHINESE) {
            time = (time.contains("AM")? "上午" : "下午" ) + time.substring(0, 2) + "點" + time.substring(3, 5) + "分";
        }
        return time;
    }

     // get today's date in speakable string format
    private String getDate() {
        String date;
        if(languageNum == ENGLISH) {
            date = new SimpleDateFormat("EEEE MMMM dd, yyyy").format(new Date());
        } else {
            date = new SimpleDateFormat("yyyy年 MM月 dd號").format(new Date());
            String dayOfTheWeek = new SimpleDateFormat("E").format(new Date());
            switch(dayOfTheWeek) {
                case "Mon":
                    dayOfTheWeek = "1";
                    break;
                case "Tue":
                    dayOfTheWeek = "2";
                    break;
                case "Wed":
                    dayOfTheWeek = "3";
                    break;
                case "Thu":
                    dayOfTheWeek = "4";
                    break;
                case "Fri":
                    dayOfTheWeek = "5";
                    break;
                case "Sat":
                    dayOfTheWeek = "6";
                    break;
                case "Sun":
                    dayOfTheWeek = "天";
                    break;
            }
            date = "星期" + dayOfTheWeek + ", " + date;
        }
        return date;
    }

    // take picture/video 0 = pic, 1 = vid
    public void takePictureVideo(int type) {
        String action, dir, subdir, ext;
        // initialize based on type of media
        if(type == 0) {
            action = MediaStore.ACTION_IMAGE_CAPTURE;
            dir = Environment.DIRECTORY_DCIM;
            subdir = "Camera";
            ext = ".jpg";
        } else {
            action = MediaStore.ACTION_VIDEO_CAPTURE;
            dir = Environment.DIRECTORY_MOVIES;
            subdir = "";
            ext = ".mp4";
        }

        Intent intent = new Intent(action);
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(dir), subdir);

        // make directory if it doesn't exist
        if (! mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }

        contentUri = Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator
                + new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date()) + ext));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
        // start the media capture Intent
        startActivityForResult(intent, PICTURE_VIDEO_REQUEST_CODE);
    }

    // adjusts brightness up or down
    public void adjustBrightness(int direction) {
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        int brightness = 255;
        try {
            brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch(Exception e) {}

        if(direction == 0) {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness - 100);
        } else {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness + 100);
        }
    }

    // open language menu in dialog box
    public void openLanguageMenu() {
        TextView title = (TextView) languageMenu.findViewById(R.id.title);
        title.setText(dialogue[languageNum][4][1]);
        // show dialog box
        languageMenu.show();
    }

}
