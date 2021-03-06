package com.callerid.sampleapp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements ServiceCallbacks{

    private static TextView lbStatus;

    // UDP listen requirements
    private UDPListen mService;
    private boolean mBound = false;
    private String inString = "Waiting for Calls.";
    private ArrayList<String> previousReceptions;

    // Notifications
    static final int uniqueID = 1122334455;
    private String notificationStringL1 = "L01 : Idle";
    private String notificationStringL2 = "L02 : Idle";
    private String notificationStringL3 = "L03 : Idle";
    private String notificationStringL4 = "L04 : Idle";

    // Required objects
    static ImageView callPhonePics[] = new ImageView[5];
    static TextView callTimes[] = new TextView[5];
    static TextView callNumbers[] = new TextView[5];
    static TextView callNames[] = new TextView[5];
    static TableRow callPanels[] = new TableRow[5];

    // Memory of program when minimized
    static int memPhoneStatus[] = new int[5];
    static String memCallTimes[] = new String[5];
    static String memCallNumbers[] = new String[5];
    static String memCallNames[] = new String[5];
    static int memCallPanels[] = new int[5];

    NotificationManager nm;

    // Required Variables
    static int lightBlue = Color.rgb(223, 246, 247);
    static int lightGreen = Color.rgb(211,245,211);
    static int lightestGreen = Color.rgb(232, 250, 233);
    static int lightGrey = Color.rgb(227,229,230);

    static int imgRing;
    static int imgOffHook;
    static int imgOnHook;

    // Required for memory capturing during app in background
    private boolean isInFront;

    @Override
    public void onResume() {
        super.onResume();
        isInFront = true;
        showMemory();
    }

    @Override
    public void onPause() {
        super.onPause();
        isInFront = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // end listener Service
        Intent intent = new Intent(this, UDPListen.class);
        stopService(intent);
        unbindService(mConnection);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // bind to Service
        Intent intent = new Intent(this, UDPListen.class);
        mBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Notifications
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Associations
        lbStatus = (TextView)findViewById(R.id.lbStatus);

        callPhonePics[1] = (ImageView)findViewById(R.id.picLine1);
        callPhonePics[2] = (ImageView)findViewById(R.id.picLine2);
        callPhonePics[3] = (ImageView)findViewById(R.id.picLine3);
        callPhonePics[4] = (ImageView)findViewById(R.id.picLine4);

        callTimes[1] = (TextView)findViewById(R.id.lbTime1);
        callTimes[2] = (TextView)findViewById(R.id.lbTime2);
        callTimes[3] = (TextView)findViewById(R.id.lbTime3);
        callTimes[4] = (TextView)findViewById(R.id.lbTime4);

        callNumbers[1] = (TextView)findViewById(R.id.lbNumber1);
        callNumbers[2] = (TextView)findViewById(R.id.lbNumber2);
        callNumbers[3] = (TextView)findViewById(R.id.lbNumber3);
        callNumbers[4] = (TextView)findViewById(R.id.lbNumber4);

        callNames[1] = (TextView)findViewById(R.id.lbName1);
        callNames[2] = (TextView)findViewById(R.id.lbName2);
        callNames[3] = (TextView)findViewById(R.id.lbName3);
        callNames[4] = (TextView)findViewById(R.id.lbName4);

        callPanels[1] = (TableRow)findViewById(R.id.panLine1);
        callPanels[2] = (TableRow)findViewById(R.id.panLine2);
        callPanels[3] = (TableRow)findViewById(R.id.panLine3);
        callPanels[4] = (TableRow)findViewById(R.id.panLine4);

        // Image references
        imgRing = getResources().getIdentifier("phonering" , "mipmap", getPackageName());
        imgOffHook = getResources().getIdentifier("phoneoffhook" , "mipmap", getPackageName());
        imgOnHook = getResources().getIdentifier("phoneonhook" , "mipmap", getPackageName());

        // Place previous call information back on screen
        showMemory();

    }

    // Setup connection/binder to service
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            UDPListen.LocalBinder binder = (UDPListen.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
            mService.setCallbacks(MainActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }

    };

    // Link Display to Update so the UI gets updated through interface
    @Override
    public void display(String rString){

        inString = rString;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateUI(inString,isInFront);
            }
        });

    }

    // ----------------------------------------------------------------------

    private void removeReceptionFromBuffer(String reception){

        ArrayList<Integer> indexes = new ArrayList<>();
        int cnt = 0;

        for(String pReception : previousReceptions) {

            if(pReception.contains(reception.substring(reception.length()-20))){
                indexes.add(cnt);
            }
            cnt++;
        }

        for(int i = indexes.size()-1; i >= 0; i--){
            int remove = indexes.get(i);
            previousReceptions.remove(remove);
        }
    }

    public void updateUI(String inData, boolean visible){

        // Code to ignore duplicates
        if(previousReceptions.contains(inData)) {
            // If duplicate, ignore
            return;
        }
        else{
            // If not duplicate add to check buffer
            if(previousReceptions.size()>30) {
                // If check buffer is full, add one to the end and remove oldest
                previousReceptions.add(inData);
                previousReceptions.remove(0);
            }
            else{
                // If check buffer not full, simply add to end
                previousReceptions.add(inData);
            }
        }

        // Setup variables for use
        String myData = inData;

        String command;
        Integer myLine=0;
        String myType="";
        String myIndicator="";

        // Unused in this app but available for other custom apps
        String myDuration="";
        String myCheckSum="";
        String myRings="";
        //------------------------------------------------------

        String myDateTime="";
        String myNumber="";
        String myName="";

        // Check if matches a call record
        Pattern myPattern = Pattern.compile(".*(\\d\\d) ([IO]) ([ES]) (\\d{4}) ([GB]) (.)(\\d) (\\d\\d/\\d\\d \\d\\d:\\d\\d [AP]M) (.{8,15})(.*)");
        Matcher matcher = myPattern.matcher(myData);

        if(matcher.find()){

            myLine = Integer.parseInt(matcher.group(1));
            myType = matcher.group(2);

            if(myType.equals("I")||myType.equals("O")){

                myIndicator = matcher.group(3);

                // Unused in this app but available for other custom apps
                myDuration = matcher.group(4);
                myCheckSum = matcher.group(5);
                myRings = matcher.group(6);
                //------------------------------------------------------

                myDateTime = matcher.group(8);
                myNumber = matcher.group(9);
                myName = matcher.group(10);

            }

            if(myIndicator.equals("E")){
                removeReceptionFromBuffer(inData);
            }

        }

        // Check to see if call information is from a DETAILED record
        Pattern myPatternDetailed = Pattern.compile(".*(\\d\\d) ([NFR]) {13}(\\d\\d/\\d\\d \\d\\d:\\d\\d:\\d\\d)");
        Matcher matcherDetailed = myPatternDetailed.matcher(myData);

        if(matcherDetailed.find()){

            myLine = Integer.parseInt(matcherDetailed.group(1));
            myType = matcherDetailed.group(2);

            if(myType.equals("N")||myType.equals("F")||myType.equals("R")){
                myDateTime = matcherDetailed.group(3);
            }

        }

        // Combine type and indicator to allow for 'findCommandValue()' function
        command = myType + myIndicator;

        // Get unique value for commands
        int commandUnique = findCommandValue(command);

        // Store call information and if app is in foreground show changes
        if(!visible){

            // Application is NOT in foreground so remember all activity
            // for later display when app is resumed.
            switch(commandUnique){

                case 1: // 'R' for Ring

                    // Incoming Call ------------------
                    // Remember all call information
                    memPhoneStatus[myLine] = imgRing;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    break;

                case 2: // 'IS' for Inbound Start

                    // Ring answered ---------------------

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOffHook;
                    memCallPanels[myLine] = lightestGreen;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Send notification to lock screen
                    sendNotifications(true,myLine,myName,myNumber);

                    break;

                case 3: // 'F' for Off Hook

                    // Phone pickup --------------------

                    /// Remember needed call information
                    memPhoneStatus[myLine] = imgOffHook;

                    break;

                case 4: // 'N' for On Hook

                    // Remember needed call information
                    memCallPanels[myLine] = lightGrey;
                    memPhoneStatus[myLine] = imgOnHook;

                    break;

                case 5: // 'IE' for Inbound End

                    // Remember all call information
                    memCallPanels[myLine] = lightGrey;
                    memPhoneStatus[myLine] = imgOnHook;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Send notification to lock screen
                    //sendNotifications(false,myLine,myName,myNumber);

                    break;

                case 6: // 'OS' for Outbound Start

                    // Outgoing Call -------------------

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOffHook;
                    memCallPanels[myLine] = lightBlue;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    break;

                case 7: // 'OE' for Outbound End

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOnHook;
                    memCallPanels[myLine] = lightGrey;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    break;
            }

        }else{

            // App. IS in foreground so display all changes
            switch(commandUnique){

                case 1: // 'R' for Ring

                    // Remember all call information
                    memPhoneStatus[myLine] = imgRing;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Incoming Call ------------------

                    // Change picture of phone to ringing
                    callPhonePics[myLine].setImageResource(imgRing);

                    // Light up panel green for incoming call
                    callPanels[myLine].setBackgroundColor(lightGreen);

                    // Show time then clear name & number
                    callTimes[myLine].setText(myDateTime);
                    callNumbers[myLine].setText(myNumber);
                    callNames[myLine].setText(myName);

                    break;

                case 2: // 'IS' for Inbound Start

                    // Ring answered ---------------------

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOffHook;
                    memCallPanels[myLine] = lightestGreen;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Light-up panel green for incoming call
                    callPanels[myLine].setBackgroundColor(lightestGreen);

                    // Show time on panel
                    callTimes[myLine].setText(myDateTime);

                    // Show name and number on panel
                    callNumbers[myLine].setText(myNumber);
                    callNames[myLine].setText(myName);

                    // Send notification about new call
                    sendNotifications(true,myLine,myName,myNumber);

                    break;

                case 3: // 'F' for Off Hook

                    // Phone pickup --------------------

                    // Remember needed call information
                    memPhoneStatus[myLine] = imgOffHook;

                    // Change picture of phone to off hook
                    callPhonePics[myLine].setImageResource(imgOffHook);

                    break;

                case 4: // 'N' for On Hook

                    // Remember needed call information
                    memCallPanels[myLine] = lightGrey;
                    memPhoneStatus[myLine] = imgOnHook;

                    // Phone hangup
                    callPanels[myLine].setBackgroundColor(lightGrey);

                    // Change picture of phone to not-ringing
                    callPhonePics[myLine].setImageResource(imgOnHook);

                    break;

                case 5: // 'IE' for Inbound End

                    // Remember all call information
                    memCallPanels[myLine] = lightGrey;
                    memPhoneStatus[myLine] = imgOnHook;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Phone hangup
                    callPanels[myLine].setBackgroundColor(lightGrey);

                    // Change picture of phone to not-ringing
                    callPhonePics[myLine].setImageResource(imgOnHook);

                    // Show time on panel
                    callTimes[myLine].setText(myDateTime);

                    // Show name and number on panel
                    callNumbers[myLine].setText(myNumber);
                    callNames[myLine].setText(myName);

                    //sendNotifications(false,myLine,myName,myNumber);

                    break;
                case 6: // 'OS' for Outbound Start

                    // Outgoing Call -------------------

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOffHook;
                    memCallPanels[myLine] = lightBlue;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Change picture of phone to off hook
                    callPhonePics[myLine].setImageResource(imgOffHook);

                    callPanels[myLine].setBackgroundColor(lightBlue);

                    // Show time on panel
                    callTimes[myLine].setText(myDateTime);

                    // Show name and number on panel
                    callNumbers[myLine].setText(myNumber);
                    callNames[myLine].setText(myName);

                    break;

                case 7: // 'OE' for Outbound End

                    // Remember all call information
                    memPhoneStatus[myLine] = imgOnHook;
                    memCallPanels[myLine] = lightGrey;
                    memCallTimes[myLine] = myDateTime;
                    memCallNumbers[myLine] = myNumber;
                    memCallNames[myLine] = myName;

                    // Phone hangup
                    callPanels[myLine].setBackgroundColor(lightGrey);

                    // Change picture of phone to not-ringing
                    callPhonePics[myLine].setImageResource(imgOnHook);

                    break;
            }

        }

    }

    private void showMemory(){

        // Update all lines with stored memory
        for(int i=1;i<5;i++){

            callPhonePics[i].setImageResource(memPhoneStatus[i]);
            callPanels[i].setBackgroundColor(memCallPanels[i]);
            callTimes[i].setText(memCallTimes[i]);
            callNumbers[i].setText(memCallNumbers[i]);
            callNames[i].setText(memCallNames[i]);

        }

    }

    private void sendNotifications(Boolean start,int line, String name, String number){

        // Fix lengths for name and number
        while(name.length()<16){
            name = name + " ";
        }
        while(number.length()<16){
            number = number + " ";
        }

        // Insert call activity into the notifications string
        if(start){

            switch(line){

                case 1:
                    notificationStringL1 = "L01 : " + name + "    " + number;
                    break;

                case 2:
                    notificationStringL2 = "L02 : " + name + "    " + number;
                    break;

                case 3:
                    notificationStringL3 = "L03 : " + name + "    " + number;
                    break;

                case 4:
                    notificationStringL4 = "L04 : " + name + "    " + number;
                    break;

            }

        }else{

            switch(line){

                case 1:
                    notificationStringL1 = "L01 : Idle";
                    break;

                case 2:
                    notificationStringL2 = "L02 : Idle";
                    break;

                case 3:
                    notificationStringL3 = "L03 : Idle";
                    break;

                case 4:
                    notificationStringL4 = "L04 : Idle";
                    break;

            }

        }

        // Check which icon to show
        int myIcon;
        if(notificationStringL1.contains("Idle") && notificationStringL2.contains("Idle") && notificationStringL3.contains("Idle") && notificationStringL4.contains("Idle")) {
            myIcon = imgOnHook;
        }else{
            myIcon = imgOffHook;
        }

        // Update notification
        String notifyString = notificationStringL1 + "\r\n" + notificationStringL2 + "\r\n" + notificationStringL3 + "\r\n" + notificationStringL4;

        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);

        Notification n = new Notification.Builder(this)
                .setContentTitle("Call Activity")
                .setContentText(notifyString)
                .setSmallIcon(myIcon)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(notifyString))
                .setContentIntent(pi)
                .build();

        nm.notify(uniqueID, n);

    }

    private int findCommandValue(String str){

       // Since the SWITCH command does not allow for strings
       // we return the correct INT value by looking at the string
        if(str.equals("R"))return 1;
        if(str.equals("IS"))return 2;
        if(str.equals("F"))return 3;
        if(str.equals("N"))return 4;
        if(str.equals("IE"))return 5;
        if(str.equals("OS"))return 6;
        if(str.equals("OE"))return 7;

        return 0;

    }

}
