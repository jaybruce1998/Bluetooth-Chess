/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.android.bluetoothchat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Initially launched when the app is opened, manages button presses and UI changes
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices its visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {
    //I decided against making getters and setters and made variables private for my own sanity and code size
    protected static ImageView[][] views;//every square on the board
    protected static ImageView[] white, black;//captured pieces
    protected final static HashMap<String, Integer> map=new HashMap<>();//mapping of piece names to their image IDs
    protected static Chess chess;//handles most chess logic
    private ArrayList<Pair<Integer, Integer>> moves;//legal places you can move a piece to if one is selected
    //the row and column of the piece to move, where to move it, where to place the next captured piece images,
    //and inverted coordinates to use for when the board is flipped (which it is on white's turn
    protected static int r1, c1, r2, c2, wi, bi, wr1, wc1, wr2, wc2;
    protected static char me;//whose turn is it?
    private boolean canDraw;//have you offered a draw this turn? If so, you can't offer again for a while
    private AlertDialog actions;
    //when your pawn reaches the back row, you select the type of piece to promote to and this listens for your choice
    private final DialogInterface.OnClickListener actionListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int w) {
                    //promotes the piece internally and externally simultaneously
                    views[r2][c2].setImageResource(map.get(chess.promote(wr2, wc2, w)));
                    r1=8;//deselect the piece you moved
                    //tell the user if their opponent is in check, checkmate, etc.
                    Toast.makeText(MainActivity.this, chess.state(), Toast.LENGTH_SHORT).show();
                    fragment.sendMessage(""+wr1+wc1+wr2+wc2+w);//send the move over bluetooth
                }
            };
    private BluetoothChatFragment fragment;//handles bluetooth communication
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ids for every square on the UI board, doesn't follow standard chess notation!
        int[][] ids = new int[][]{
                {R.id.a1, R.id.a2, R.id.a3, R.id.a4, R.id.a5, R.id.a6, R.id.a7, R.id.a8},
                {R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8},
                {R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6, R.id.c7, R.id.c8},
                {R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5, R.id.d6, R.id.d7, R.id.d8},
                {R.id.e1, R.id.e2, R.id.e3, R.id.e4, R.id.e5, R.id.e6, R.id.e7, R.id.e8},
                {R.id.f1, R.id.f2, R.id.f3, R.id.f4, R.id.f5, R.id.f6, R.id.f7, R.id.f8},
                {R.id.g1, R.id.g2, R.id.g3, R.id.g4, R.id.g5, R.id.g6, R.id.g7, R.id.g8},
                {R.id.h1, R.id.h2, R.id.h3, R.id.h4, R.id.h5, R.id.h6, R.id.h7, R.id.h8}
        };
        //ids for captured pieces on the UI
        int[] w=new int[]{R.id.w1, R.id.w2, R.id.w3, R.id.w4, R.id.w5, R.id.w6, R.id.w7, R.id.w8,
                R.id.w9, R.id.w10, R.id.w11, R.id.w12, R.id.w13, R.id.w14, R.id.w15},
                b=new int[]{R.id.bl1, R.id.bl2, R.id.bl3, R.id.bl4, R.id.bl5, R.id.bl6, R.id.bl7, R.id.bl8,
                R.id.bl9, R.id.bl10, R.id.bl11, R.id.bl12, R.id.bl13, R.id.bl14, R.id.bl15};
        views=new ImageView[8][8];
        white=new ImageView[15];
        black=new ImageView[15];
        //make references to all UI components for easy access
        for(int i=0; i<8; i++)
            for(int j=0; j<8; j++)
                views[i][j]=findViewById(ids[i][j]);
        for(int i=0; i<15; i++)
        {
            white[i]=findViewById(w[i]);
            black[i]=findViewById(b[i]);
        }
        //map piece names to their drawable equivalents
        map.put("wrk", R.drawable.wrk);
        map.put("wkt", R.drawable.wkt);
        map.put("wbp", R.drawable.wbp);
        map.put("wkg", R.drawable.wkg);
        map.put("wqn", R.drawable.wqn);
        map.put("wpn", R.drawable.wpn);
        map.put("brk", R.drawable.brk);
        map.put("bkt", R.drawable.bkt);
        map.put("bbp", R.drawable.bbp);
        map.put("bkg", R.drawable.bkg);
        map.put("bqn", R.drawable.bqn);
        map.put("bpn", R.drawable.bpn);
        map.put("   ", R.drawable.q);//an empty square, didn't feel like typing out "empty"
        chess=new Chess();
        r1=8;
        //pawn promotion dialog box
        AlertDialog.Builder builder = new
                AlertDialog.Builder(this);
        builder.setTitle("What would you like to promote to?");
        String[] options = {"Rook", "Knight", "Bishop", "Queen"};
        builder.setItems(options, actionListener);
        actions = builder.create();
        //since this was originally a chat app, the keyboard was opened by default, so I close it here
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }
    //when a piece is deselected or moved, highlighting must be removed
    private void removeHighlights()
    {
        //if a piece was actually selected
        if(r1<8)
        {
            //un-highlight the piece in question
            //a checkerboard pattern can be maintained based on comparing evenness between the row and column
            views[r1][c1].setBackgroundResource(r1%2==c1%2?R.drawable.e:R.drawable.f);
            //board is flipped for white, remove the highlights in the right squares!
            if(me=='b')
                for(Pair<Integer, Integer> m: moves)
                    views[m.first][m.second].setBackgroundResource(m.first%2==m.second%2?R.drawable.e:R.drawable.f);
            else
                for(Pair<Integer, Integer> m: moves)
                    views[7-m.first][7-m.second].setBackgroundResource(m.first%2==m.second%2?R.drawable.e:R.drawable.f);
            r1=8;//deselect the piece internally
        }
    }
    //determine whether a move was legal
    private boolean legal(int r, int c)
    {
        //legal moves are determined when a piece is initially selected, check if the move we chose is legal
        for(Pair<Integer, Integer> m: moves)
            if(r==m.first&&c==m.second)
                return true;
        return false;
    }
    //at the beginning of a new game, the board must be reset internally and externally
    public static void resetBoard()
    {
        chess=new Chess();//reset board internally
        //reset captured pieces
        for(int i=0; i<15; i++)
        {
            white[i].setImageResource(R.drawable.q);
            black[i].setImageResource(R.drawable.q);
        }
        //fill in board with the proper perspective
        if(me=='b')
            for(int r=0; r<8; r++)
                for(int c=0; c<8; c++)
                    views[r][c].setImageResource(map.get(chess.board[r][c].toString()));
        else
            for(int r=0; r<8; r++)
                for(int c=0; c<8; c++)
                    views[r][c].setImageResource(map.get(chess.board[7-r][7-c].toString()));
        wi=0;
        bi=0;
        chess.playing=true;
    }
    //when a square is pressed, this method is called
    public void press(View view)
    {
        //since the id of a view that was set in the XML file cannot be accessed programatically, tags were used
        //and interpreted to determine which square was pressed
        char[] a=view.getTag().toString().toCharArray();
        //extract the coordinates of the square that was pressed
        int r=Character.getNumericValue(a[0]), c=Character.getNumericValue(a[1]), wr=r, wc=c;
        //accommodate for whether the board was flipped
        if(me=='w')
        {
            wr=7-r;
            wc=7-c;
        }
        //If I'm in a game, I selected one of my pieces, it's my turn and I didn't press the same piece again
        if (chess.playing&&me==chess.board[wr][wc].color&&me == chess.turn&&(r!=r1||c!=c1)) {
            removeHighlights();//I might have already selected a piece, so remove any existing highlights
            //store the selected piece long-term
            r1 = r;
            c1 = c;
            wr1=wr;
            wc1=wc;
            moves=chess.moves(wr, wc);
            //highlight all squares that the piece can move to
            if(me=='b')
                for(Pair<Integer, Integer> m: moves)
                    views[m.first][m.second].setBackgroundResource(R.drawable.b);
            else
                for(Pair<Integer, Integer> m: moves)
                    views[7-m.first][7-m.second].setBackgroundResource(R.drawable.b);
            //and the selected piece itself
            views[r][c].setBackgroundResource(R.drawable.a);
        }
        //I already selected a piece and the square I want to move to is a legal move
        else if (r1<8&&legal(wr, wc)) {
            canDraw=true;//I have completed a move, I can offer draws again!
            //store the piece to move and the piece I am capturing (or empty square I am moving to) as Strings
            //for convenience
            String p=chess.board[wr1][wc1].toString(), n=chess.board[wr][wc].toString();
            //If I captured a piece, show it at the bottom of the screen
            if(n.charAt(0)==chess.cap)
                if(chess.cap=='w')
                    white[wi++].setImageResource(map.get(n));
                else
                    black[bi++].setImageResource(map.get(n));
            chess.move(wr1, wc1, wr, wc);//move internally
            views[r1][c1].setImageResource(R.drawable.q);//change the square we are moving to into an empty square
            //if we are queening (a pawn reached the last row)
            if(p.endsWith("pn")&&(r==0||r==7))
            {
                //the square to move to only has to be stored long-term for pawn promotion
                r2=r;
                c2=c;
                wr2=wr;
                wc2=wc;
                actions.show();
            }
            else {
                //not a pawn move? We might have castled, so update the whole row out of laziness to account for this!
                if(me=='b')
                    for (int i = 0; i < 8; i++)
                        views[r1][i].setImageResource(map.get(chess.board[r1][i].toString()));
                else
                    for (int i = 0; i < 8; i++)
                        views[r1][7-i].setImageResource(map.get(chess.board[wr1][i].toString()));
                views[r][c].setImageResource(map.get(p));//put the piece on its new square in the UI
                Toast.makeText(this, chess.state(), Toast.LENGTH_SHORT).show();
                fragment.sendMessage(""+wr1+wc1+wr+wc);
            }
            removeHighlights();
        }
        //tried to make an illegal move or we wish to deselect a piece
        else {
            removeHighlights();
        }
    }
    //offer to play a game with whoever you are connected to
    public void play(View view)
    {
        if(chess.playing)
            Toast.makeText(this, "You are already in a game!", Toast.LENGTH_SHORT).show();
        else
            fragment.sendMessage("play");
    }
    //attempt to offer a draw to the other player
    public void draw(View view)
    {
        if(chess.playing)
            if(canDraw) {
                canDraw=false;
                fragment.sendMessage("draw");
            }
            else
                Toast.makeText(this, "You cannot offer a draw right now!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "You are not in a game!", Toast.LENGTH_SHORT).show();
    }
    //resign from the current game, resulting in a loss
    public void resign(View view)
    {
        if(chess.playing) {
            chess.playing=false;
            Toast.makeText(this, "You lose!", Toast.LENGTH_SHORT).show();
            fragment.sendMessage("resign");
        }
        else
            Toast.makeText(this, "You are not in a game!", Toast.LENGTH_SHORT).show();
    }
    public static final String TAG = "MainActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;

/*    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        //logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create a chain of targets that will receive log data
     */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }
}
