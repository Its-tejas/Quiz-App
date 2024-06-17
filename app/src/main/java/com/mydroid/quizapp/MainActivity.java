package com.mydroid.quizapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mydroid.quizapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    List<QuizModel> quizModelList;
    int currentQue = 0;
    int correct = 0, wrong = 0, not_attempt = 10;
    CountDownTimer timer;
    long timeLeft = 10 * 60 * 1000; // 10 minutes in milliseconds
    int flag = 0;        // to overcome from repeat question
    int timeout = 0;     // to overcome from conflicting with manageButtons();

    private static final String PREFS_NAME = "QuizApp";
    private static final String KEY_CURRENT_QUESTION = "currentQuestion";
    private static final String KEY_TIME_LEFT = "timeLeft";
    private static final String KEY_CORRECT = "correct";
    private static final String KEY_WRONG = "wrong";
    private static final String KEY_NOT_ATTEMPT = "not_attempt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.startQuizBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                binding.timerTextview.setVisibility(View.VISIBLE);
                binding.quizContainer.setVisibility(View.VISIBLE);
                binding.startQuizBtn.setVisibility(View.INVISIBLE);

                // Restore saved state
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                currentQue = prefs.getInt(KEY_CURRENT_QUESTION, 0);
                timeLeft = prefs.getLong(KEY_TIME_LEFT, 10 * 60 * 1000);
                correct = prefs.getInt(KEY_CORRECT, 0);
                wrong = prefs.getInt(KEY_WRONG, 0);
                not_attempt = prefs.getInt(KEY_NOT_ATTEMPT, 10);

                loadQuestions();    // get all question from json file
                startTimer();

                setQuiz(currentQue);    // load 1st que


            }
        });

        binding.ans1btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(binding.ans1btn, quizModelList.get(currentQue).getAnswer1());

            }
        });
        binding.ans2btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(binding.ans2btn, quizModelList.get(currentQue).getAnswer2());
            }
        });

        binding.ans3btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(binding.ans3btn, quizModelList.get(currentQue).getAnswer3());
            }
        });

        binding.ans4btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(binding.ans4btn, quizModelList.get(currentQue).getAnswer4());
            }
        });


        binding.nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // logic for load next quiz
                if (currentQue < quizModelList.size()-1)
                {
                    manageButtons();
                    setBtnColor("reset");
                    currentQue++;
                    flag = 0;
                    setQuiz(currentQue);
                }else {
                    timer.cancel();
                    showResult();
                }
            }
        });

        binding.restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.restartBtn.setVisibility(View.INVISIBLE);
                binding.resultContainer.setVisibility(View.INVISIBLE);
                binding.quizContainer.setVisibility(View.VISIBLE);
                currentQue = 0;
                correct = 0;
                wrong = 0;
                not_attempt = 10;
                timeLeft = 10 * 60 * 1000;
                setQuiz(currentQue);
                startTimer();
                setBtnColor("reset");
                binding.nextBtn.setVisibility(View.INVISIBLE);
                manageButtons();
                Toast.makeText(MainActivity.this, "Quiz restarted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResult()
    {
        binding.quizContainer.setVisibility(View.INVISIBLE);
        binding.resultContainer.setVisibility(View.VISIBLE);
        binding.restartBtn.setVisibility(View.VISIBLE);

        // data binding
        binding.correctResultTV.setText(String.valueOf(correct));
        binding.wrongResultTV.setText(String.valueOf(wrong));
        binding.notAttemptResultTV.setText(String.valueOf(not_attempt));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
        if (flag == 1) currentQue++;
        saveState();
    }

    private void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_CURRENT_QUESTION, currentQue);
        editor.putLong(KEY_TIME_LEFT, timeLeft);
        editor.putInt(KEY_CORRECT, correct);
        editor.putInt(KEY_WRONG, wrong);
        editor.putInt(KEY_NOT_ATTEMPT, not_attempt);
        editor.apply();
    }

    private void startTimer()
    {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;

                long min = ((millisUntilFinished / 1000) % 3600) / 60;
                long sec = (millisUntilFinished / 1000) % 60;

                String timeFormat = String.format(Locale.getDefault(), "%02d:%02d", min, sec);
                binding.timerTextview.setText(timeFormat);
            }

            @Override
            public void onFinish() {
                binding.timerTextview.setText("00:00");
                Toast.makeText(MainActivity.this, "Timer has finished", Toast.LENGTH_SHORT).show();
                timeout = 1;     // to overcome from conflicting with manageButtons();
                showResult();
            }
        }.start();
    }

    private void setBtnColor(String req)
    {
        if (req.equals("reset")) {
                binding.ans1btn.setBackgroundColor(getResources().getColor(R.color.Grey));
                binding.ans2btn.setBackgroundColor(getResources().getColor(R.color.Grey));
                binding.ans3btn.setBackgroundColor(getResources().getColor(R.color.Grey));
                binding.ans4btn.setBackgroundColor(getResources().getColor(R.color.Grey));
        }else  {
            String correctAns = quizModelList.get(currentQue).getCorrectAns();
            if (quizModelList.get(currentQue).getAnswer1().equals(correctAns))
                binding.ans1btn.setBackgroundColor(Color.GREEN);
            else if (quizModelList.get(currentQue).getAnswer2().equals(correctAns))
                binding.ans2btn.setBackgroundColor(Color.GREEN);
            else if (quizModelList.get(currentQue).getAnswer3().equals(correctAns))
                binding.ans3btn.setBackgroundColor(Color.GREEN);
            else
                binding.ans4btn.setBackgroundColor(Color.GREEN);
        }
    }

    private void manageButtons() {
        if ( timeout == 0 && binding.ans1btn.isEnabled() && binding.nextBtn.getVisibility() == View.INVISIBLE) {
            binding.ans1btn.setEnabled(false);
            binding.ans2btn.setEnabled(false);
            binding.ans3btn.setEnabled(false);
            binding.ans4btn.setEnabled(false);
            binding.nextBtn.setVisibility(View.VISIBLE);
        }else
        {
            binding.ans1btn.setEnabled(true);
            binding.ans2btn.setEnabled(true);
            binding.ans3btn.setEnabled(true);
            binding.ans4btn.setEnabled(true);
            binding.nextBtn.setVisibility(View.INVISIBLE);
            timeout = 0;
        }
    }

    private void handleAnswer(View selectedButton, String selectedAnswer) {
        manageButtons();
        flag = 1;
        String correctAns = quizModelList.get(currentQue).getCorrectAns();
        if (selectedAnswer.equals(correctAns)) {
            correct++;
            not_attempt--;
            selectedButton.setBackgroundColor(Color.GREEN);
        } else {
            wrong++;
            not_attempt--;
            selectedButton.setBackgroundColor(Color.RED);
            setBtnColor("");
        }
    }

    private void setQuiz(int index)
    {
        binding.queTextview.setText(quizModelList.get(index).getQuestion());
        binding.ans1btn.setText(quizModelList.get(index).getAnswer1());
        binding.ans2btn.setText(quizModelList.get(index).getAnswer2());
        binding.ans3btn.setText(quizModelList.get(index).getAnswer3());
        binding.ans4btn.setText(quizModelList.get(index).getAnswer4());
    }

    private void loadQuestions()
    {
        quizModelList = new ArrayList<>();
        String json = loadJson("quiz.json");    // load all questions

        // load all data into list
        try {
            JSONArray jsonArray = new JSONArray(json);
            int maxSize = jsonArray.length();

            for (int i=0; i<maxSize; i++)
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String questionStr = jsonObject.getString("question");
                JSONArray answers = jsonObject.getJSONArray("answers");
                String answer1Str = answers.getString(0);
                String answer2Str = answers.getString(1);
                String answer3Str = answers.getString(2);
                String answer4Str = answers.getString(3);
                String correctAnswerStr = answers.getString(jsonObject.getInt("correctIndex"));

                quizModelList.add(new QuizModel(questionStr, answer1Str, answer2Str, answer3Str, answer4Str, correctAnswerStr));
            }
        }
        catch (JSONException e)
        {
            Log.e("TAG", "loadJson: " + e);
        }

    }

    private String loadJson(String fileName) {
        String json = "";
        try {
            InputStream inputStream = getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.e("TAG", "loadJson: " + e);
        }
        return json;
    }
}