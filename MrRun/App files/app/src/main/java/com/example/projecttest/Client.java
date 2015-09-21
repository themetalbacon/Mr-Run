package com.example.projecttest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Client extends ActionBarActivity {

    // TAG for logging
    private static final String TAG = "Client";

    // server to connect to
    protected static final int SERVER_PORT = 2000;
    protected static final String SERVER_ADDRESS = "ec2-54-149-12-88.us-west-2.compute.amazonaws.com";

    public static ArrayList<ImageView> playerList = new ArrayList<ImageView>(20);

    // networking
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;

    ImageView myPerson = null;
    boolean connected = false;
    RelativeLayout myLayout = null;
    AnimationDrawable walkingAnim = null;
    AnimatorSet animSetXY = null;
    Button push = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        myPerson = (ImageView) findViewById(R.id.p1);
        setImageResource(myPerson, login.myColour);

        myLayout = (RelativeLayout) this.findViewById(R.id.rLayout);
        push = (Button)findViewById(R.id.pushButton);

        push.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                push();
            }
        });

        if (login.username.equals("guest")) {
            push.setVisibility(View.INVISIBLE);
        }

        connect();

    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy called");
        disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_project, menu);
        return true;
    }


    /***************************************************************************/
    /********* Networking ******************************************************/
    /***************************************************************************/

    /**
     * Connects to server and sends notification
     */
    void connect() {

        new AsyncTask<Void, Void, String>() {

            String errorMsg = null;

            @Override
            protected String doInBackground(Void... args) {
                Log.i(TAG, "Connect task started");
                try {
                    connected = false;
                    socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    Log.i(TAG, "Socket created");
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream());

                    connected = true;
                    Log.i(TAG, "Input and output streams ready");

                    send("JOIN@" + login.myColour);
                    Log.i(TAG, "JOIN sent");

                } catch (UnknownHostException e1) {
                    errorMsg = e1.getMessage();
                } catch (IOException e1) {
                    errorMsg = e1.getMessage();
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
                Log.i(TAG, "Connect task finished");
                return errorMsg;
            }


            @Override
            protected void onPostExecute(String errorMsg) {
                if (errorMsg == null) {
                    Toast.makeText(getApplicationContext(),
                            "Connected to server", Toast.LENGTH_SHORT).show();

                    // start receiving
                    receive();



                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                    // can't connect: close the activity
                    finish();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Start receiving one-line messages over the TCP connection. Received lines are
     * handled in the onProgressUpdate method which runs on the UI thread.
     * This method is automatically called after a connection has been established.
     */

    void receive() {
        new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... args) {
                Log.i(TAG, "Receive task started");
                try {
                    while (connected) {

                        String msg = in.readLine();

                        if (msg == null) { // other side closed the
                            // connection
                            break;
                        }
                        publishProgress(msg);
                    }

                } catch (UnknownHostException e1) {
                    Log.i(TAG, "UnknownHostException in receive task");
                } catch (IOException e1) {
                    Log.i(TAG, "IOException in receive task");
                } finally {
                    connected = false;
                    try {
                        if (out != null)
                            out.close();
                        if (socket != null)
                            socket.close();
                    } catch (IOException e) {
                    }
                }
                Log.i(TAG, "Receive task finished");
                return null;
            }

            @Override
            protected void onProgressUpdate(String... lines) {
                // the message received from the server is
                // guaranteed to be not null
                String msg = lines[0];
                Log.i(TAG, "Message recieved!");


                if (msg.contains("INCOMING")) { // when new player joins
                    Log.i(TAG, "INCOMING MESSAGE RECIEVED");
                    ImageView person = new ImageView(getApplicationContext());

                    String cStr = msg.substring(msg.indexOf("@")+1, msg.length());
                    setImageResource(person, cStr);

                    myLayout.addView(person);

                    String idStr = msg.substring(msg.indexOf("!")+1,msg.indexOf(":"));
                    int playerId = Integer.parseInt(idStr) - 8;

                    addPlayer(playerId, person);

                    send("NEWCOORDS:" + myPerson.getX() + ","
                            + myPerson.getY() + "@" + login.myColour);
                }

                if (msg.contains("INIT")) { // initialising already existing players
                    String xStr = msg.substring(msg.indexOf(":")+1, msg.indexOf(","));
                    String yStr = msg.substring(msg.indexOf(",")+1, msg.indexOf("@"));
                    String cStr = msg.substring(msg.indexOf("@")+1, msg.length());
                    Log.i(TAG, xStr+yStr);
                    float x = Float.parseFloat(xStr);
                    float y = Float.parseFloat(yStr);

                    ImageView person = new ImageView(getApplicationContext());

                    setImageResource(person, cStr);

                    /*
                    person.setImageResource(R.drawable.run0);
                    person.setBackgroundResource(R.drawable.greyanimation);
                    */
                    myLayout.addView(person);

                    person.setX(x);
                    person.setY(y);

                    String idStr = msg.substring(msg.indexOf("!")+1,msg.indexOf(":"));
                    int playerId = Integer.parseInt(idStr) - 8;

                    addPlayer(playerId, person);

                }

                if (msg.contains("MOVED")) {
                    String idStr = msg.substring(msg.indexOf("!")+1,msg.indexOf(":"));
                    int playerId = Integer.parseInt(idStr) - 8;
                    String xStr = msg.substring(msg.indexOf(":")+1, msg.indexOf(","));
                    String yStr = msg.substring(msg.indexOf(",")+1,msg.length());
                    Log.i(TAG, xStr+yStr);
                    float x = Float.parseFloat(xStr);
                    float y = Float.parseFloat(yStr);

                    playerMoved(playerList.get(playerId), x, y);
                }

                if (msg.contains("LEFT")) {
                    String idStr = msg.substring(msg.indexOf("!")+1,msg.indexOf(":"));
                    int playerId = Integer.parseInt(idStr) - 8;

                    removePlayer(playerId);
                }

                if (msg.contains("PUSHED")) {
                    String idStr = msg.substring(msg.indexOf("!")+1,msg.indexOf(":"));
                    int playerId = Integer.parseInt(idStr) - 8;
                    String xStr = msg.substring(msg.indexOf(":")+1, msg.indexOf(","));
                    String yStr = msg.substring(msg.indexOf(",")+1,msg.length());
                    Log.i(TAG, xStr+yStr);
                    float x = Float.parseFloat(xStr);
                    float y = Float.parseFloat(yStr);

                    bePushed(x, y);
                }

                Log.i(TAG, msg);
            }



        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    void disconnect() {
        new Thread() {
            @Override
            public void run() {
                if (connected) {
                    connected = false;
                }
                // make sure that we close the output, not the input
                if (out != null) {
                    out.print("BYE");
                    out.flush();
                    out.close();
                }
                // in some rare cases, out can be null, so we need to close the socket itself
                if (socket != null)
                    try { socket.close();} catch(IOException ignored) {}

                Log.i(TAG, "Disconnect task finished");
            }
        }.start();
    }

    /**
     * Send a one-line message to the server over the TCP connection. This
     * method is safe to call from the UI thread.
     *
     * @param msg
     *            The message to be sent.
     * @return true if sending was successful, false otherwise
     */
    boolean send(String msg) {
        if (!connected) {
            Log.i(TAG, "Can't send: not connected");
            return false;
        }

        new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... msg) {
                Log.i(TAG, "sending: " + msg[0]);
                out.println(msg[0]);
                return out.checkError();
            }

            @Override
            protected void onPostExecute(Boolean error) {
                if (!error) {

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error sending message to server",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);

        return true;
    }
    /***************************************************************************/
    /***** UI related methods **************************************************/
    /**
     * ***********************************************************************
     */

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float X = event.getX();
            float Y = event.getY();
            float moveX = X - (myPerson.getWidth() / 2);
            float moveY = Y - (myPerson.getHeight() * 2);



            animateWalk (myPerson, moveX, moveY);

            send("MOVING:" + moveX + "," + moveY);
        }
        return true;
    }

    public void playerMoved (ImageView player, float X, float Y) {
        animateWalk(player, X, Y);
    }

    public void IveMoved (ImageView player, float X, float Y) {
        animateWalk(player, X, Y);
        send("MOVING:" + X + "," + Y);
    }

    public void addPlayer (int id, ImageView player) {
        if (playerList.size()<=id) {
            for (int i=playerList.size();i<=id+1;i++) {
                playerList.add(null);
            }
         }

        if (playerList.get(id) == null) {
            playerList.set(id, player);
        }
        else {
            playerList.add(player);
        }

    }

    public void animateWalk (ImageView player, Float moveX, Float moveY) {
        ObjectAnimator animX = ObjectAnimator.ofFloat(player, "x", moveX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(player, "y", moveY);

        walkingAnim = (AnimationDrawable) player.getBackground();

        animSetXY = new AnimatorSet();

        animSetXY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.i(TAG, "Animation started");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.i(TAG, "Animation ended");
                walkingAnim.stop();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                walkingAnim.stop();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

        });
        animSetXY.setDuration(2500);
        animSetXY.playTogether(animX, animY);
        walkingAnim.start();
        animSetXY.start();

    }

    public void removePlayer (int id) {
        playerList.get(id).setVisibility(View.INVISIBLE);
        myLayout.removeView(playerList.get(id));
        playerList.set(id, null);
    }

    public void setImageResource (ImageView player, String colour) {
        colour = colour.trim();
        switch (colour) {
            case "Yellow":
                player.setBackgroundResource(R.drawable.yellowanimation);
                break;
            case "Red":
                player.setBackgroundResource(R.drawable.redanimation);
                break;
            case "Green":
                player.setBackgroundResource(R.drawable.greenanimation);
                break;
            case "Blue":
                player.setBackgroundResource(R.drawable.blueanimation);
                break;
            case "Orange":
                player.setBackgroundResource(R.drawable.orangeanimation);
                break;
            case "Grey":
                player.setBackgroundResource(R.drawable.greyanimation);
                break;
        }
    }

    public void push() {
        send("PUSHING:"+myPerson.getX()+","+myPerson.getY());
    }

    public void bePushed(float X, float Y) {
        float curX = myPerson.getX();
        float curY = myPerson.getY();

        float newX = curX+100;
        float newY = curY+100;

        animateWalk (myPerson, newX, newY);
        send("MOVING:" + newX + "," + newY);


    }


}