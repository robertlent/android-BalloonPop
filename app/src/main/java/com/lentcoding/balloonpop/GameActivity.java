package com.lentcoding.balloonpop;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lentcoding.balloonpop.utils.HighScoreHelper;
import com.lentcoding.balloonpop.utils.SimpleAlertDialog;
import com.lentcoding.balloonpop.utils.SoundHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements Balloon.BalloonListener {
    private static final int MIN_ANIMATION_DELAY = 500;
    private static final int MAX_ANIMATION_DELAY = 1500;
    private static final int MIN_ANIMATION_DURATION = 1000;
    private static final int MAX_ANIMATION_DURATION = 8000;
    private static final int NUMBER_OF_PINS = 5;
    private static final int BALLOONS_PER_LEVEL = 10;

    private ViewGroup mContentView;
    private final int[] mBalloonColors = new int[3];
    private int mNextColor, mScreenWidth, mScreenHeight;
    private int mLevel;
    private int mScore;
    private int mPinsUsed;
    private TextView viewScore;
    private TextView viewLevel;
    private final List<ImageView> mPinImages = new ArrayList<>();
    private final List<Balloon> mBalloons = new ArrayList<>();
    private Button btnGo;
    private boolean mPlaying;
    private boolean mGameStopped = true;
    private int mBalloonsPopped;
    private SoundHelper mSoundHelper;
    private boolean mute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mBalloonColors[0] = Color.argb(255, 255, 0, 0);
        mBalloonColors[1] = Color.argb(255, 0, 255, 0);
        mBalloonColors[2] = Color.argb(255, 0, 0, 255);

        getWindow().setBackgroundDrawableResource(R.drawable.modern_background);

        mContentView = (ViewGroup) findViewById(R.id.activity_game);
        setToFullScreen();

        ViewTreeObserver viewTreeObserver = mContentView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mScreenWidth = mContentView.getWidth() - 175;
                    mScreenHeight = mContentView.getHeight();
                }
            });
        }

        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setToFullScreen();
            }
        });

        int mHiScore = HighScoreHelper.getTopScore(this);
        TextView viewHiScore = (TextView) findViewById(R.id.viewHiScore);
        viewHiScore.setText(String.valueOf(mHiScore));
        viewScore = (TextView) findViewById(R.id.viewScore);
        viewLevel = (TextView) findViewById(R.id.viewLevel);
        mPinImages.add((ImageView) findViewById(R.id.pushpin1));
        mPinImages.add((ImageView) findViewById(R.id.pushpin2));
        mPinImages.add((ImageView) findViewById(R.id.pushpin3));
        mPinImages.add((ImageView) findViewById(R.id.pushpin4));
        mPinImages.add((ImageView) findViewById(R.id.pushpin5));
        btnGo = (Button) findViewById(R.id.go_button);

        updateDisplay();

        mSoundHelper = new SoundHelper(this);
        mSoundHelper.prepareMusicPlayer(this);
        mute = getIntent().getBooleanExtra("mute", false);
    }

    private void setToFullScreen() {
        ViewGroup rootLayout = (ViewGroup) findViewById(R.id.activity_game);

        rootLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onPause() {
        if (mPlaying) {
            gameOver(true);
            finish();
            super.onPause();
        } else {
            super.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setToFullScreen();
    }

    private void startGame() {
        setToFullScreen();
        mScore = 0;
        mLevel = 0;
        mPinsUsed = 0;

        for (ImageView pin : mPinImages) {
            pin.setImageResource(R.drawable.pin);
        }

        mGameStopped = false;
        startLevel();
        if (!mute) {
            mSoundHelper.playMusic();
        }
    }

    private void startLevel() {
        mLevel++;
        updateDisplay();
        BalloonLauncher launcher = new BalloonLauncher();
        launcher.execute(mLevel);
        mPlaying = true;
        mBalloonsPopped = 0;
        btnGo.setText(R.string.stop_game);
    }

    private void finishLevel() {
        Toast.makeText(this, String.format(Locale.getDefault(), "You finished level %d", mLevel), Toast.LENGTH_SHORT).show();
        mPlaying = false;
        btnGo.setText(String.format(Locale.getDefault(), "Start level %d", mLevel + 1));
    }

    public void goButtonClickHandler(@SuppressWarnings("UnusedParameters") View view) {
        if (mPlaying) {
            gameOver(false);
        } else if (mGameStopped) {
            startGame();
        } else {
            startLevel();
        }
    }

    @Override
    public void popBalloon(Balloon balloon, boolean userTouch) {
        mBalloonsPopped++;
        if (!mute) {
            mSoundHelper.playSound();
        }
        mContentView.removeView(balloon);
        mBalloons.remove(balloon);

        if (userTouch) {
            mScore++;
        } else {
            mPinsUsed++;
            if (mPinsUsed <= mPinImages.size()) {
                mPinImages.get(mPinsUsed - 1).setImageResource(R.drawable.pin_off);
            }
            if (mPinsUsed == NUMBER_OF_PINS) {
                gameOver(true);
                return;
            } else if (mPinsUsed < 5) {
                Toast.makeText(this, "Missed that one!", Toast.LENGTH_SHORT).show();
            }
        }
        updateDisplay();

        if (mBalloonsPopped == BALLOONS_PER_LEVEL) {
            finishLevel();
        }
    }

    private void gameOver(boolean allPinsUsed) {
        Toast.makeText(this, "Game over!", Toast.LENGTH_SHORT).show();
        if (!mute) {
            mSoundHelper.pauseMusic();
        }

        for (Balloon balloon : mBalloons) {
            mContentView.removeView(balloon);
            balloon.setPopped(true);
        }
        mBalloons.clear();
        mPlaying = false;
        mGameStopped = true;
        btnGo.setText(R.string.start_game);

        if (allPinsUsed) {
            if (HighScoreHelper.isTopScore(this, mScore)) {
                HighScoreHelper.setTopScore(this, mScore);
                SimpleAlertDialog dialog = SimpleAlertDialog.newInstance("New High Score!", String.format(Locale.getDefault(), "Your new high score is %d", mScore));
                dialog.show(getSupportFragmentManager(), null);
            }
        }
    }

    private void updateDisplay() {
        viewScore.setText(String.valueOf(mScore));
        viewLevel.setText(String.valueOf(mLevel));
    }

    private class BalloonLauncher extends AsyncTask<Integer, Integer, Void> {

        @Override
        protected Void doInBackground(Integer... params) {

            if (params.length != 1) {
                throw new AssertionError("Expected 1 param for current level");
            }

            int level = params[0];
            int maxDelay = Math.max(MIN_ANIMATION_DELAY, (MAX_ANIMATION_DELAY - ((level - 1) * 500)));
            int minDelay = maxDelay / 2;

            int balloonsLaunched = 0;
            while (mPlaying && balloonsLaunched < BALLOONS_PER_LEVEL) {
                Random random = new Random(new Date().getTime());
                int xPosition = random.nextInt(mScreenWidth - 200);
                publishProgress(xPosition);
                balloonsLaunched++;

                int delay = random.nextInt(minDelay) + minDelay;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int xPosition = values[0];
            launchBalloon(xPosition);
        }
    }

    private void launchBalloon(int x) {

        Balloon balloon = new Balloon(this, mBalloonColors[mNextColor], 150);
        mBalloons.add(balloon);

        if (mNextColor + 1 == mBalloonColors.length) {
            mNextColor = 0;
        } else {
            mNextColor++;
        }

        balloon.setX(x);
        balloon.setY(mScreenHeight + balloon.getHeight());
        mContentView.addView(balloon);

        int duration = Math.max(MIN_ANIMATION_DURATION, MAX_ANIMATION_DURATION - (mLevel * 1000));
        balloon.releaseBalloon(mScreenHeight, duration);

    }
}
