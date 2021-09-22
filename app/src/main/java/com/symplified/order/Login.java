package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.services.AlertService;

import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends AppCompatActivity {

    private Button login;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private Dialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);
//        getSupportActionBar().hide();
        progressDialog = new Dialog(this);
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        Log.d("TAG", "onCreate: "+sharedPreferences.getAll().toString());

        login = findViewById(R.id.btn_login);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Retrofit retrofit = new Retrofit.Builder().baseUrl(App.BASE_URL+App.USER_SERVICE_URL)
                        .addConverterFactory(GsonConverterFactory.create()).build();

                LoginApi loginApiService = retrofit.create(LoginApi.class);

                Call<LoginResponse> loginResponse = loginApiService.login("application/json",
                        new LoginRequest(email.getEditText().getText().toString(), password.getEditText().getText().toString()));

                progressDialog.show();

                loginResponse.clone().enqueue(new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                        if(response.isSuccessful())
                        {
                            LoginData res = response.body().data;
                            Log.d("TAG", "Login Response : "+ response.body().data.toString());
                            Toast.makeText(getApplicationContext(), "Logged In Successfully !", Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if(!sharedPreferences.contains("isLoggedIn") || sharedPreferences.getInt("isLoggedIn", -1) == 0)
                            {
                                editor.putString("email", res.session.username);
                                editor.putString("accessToken", res.session.accessToken);
                                editor.putString("refreshToken", res.session.refreshToken);
                                editor.putString("ownerId", res.session.ownerId);
                                editor.putString("expiry", res.session.expiry.toGMTString());
                                editor.putInt("isLoggedIn", 1);
                                editor.apply();
                            }
                            Toast.makeText(getApplicationContext(), "ownerID : "+sharedPreferences.getString("ownerId", null), Toast.LENGTH_SHORT).show();
                            progressDialog.hide();
                            Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
                            startActivity(intent);
                        }
                        else {
                            Log.d("TAG", "Login response : Not successful");
                            progressDialog.hide();
                            Toast.makeText(getApplicationContext(), "Unsuccessful, Please try again", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        Log.e("TAG", "onFailure: ", t.getCause());
                        Toast.makeText(getApplicationContext(), "Check your internet connection !", Toast.LENGTH_SHORT).show();
                        progressDialog.hide();
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        if(sharedPreferences.getInt("isLoggedIn",-1) == 1 && !sharedPreferences.contains("storeId")) {
            Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
            startActivity(intent);
            finish();
        }
        else if(sharedPreferences.getInt("isLoggedIn",-1) == 1 && sharedPreferences.contains("storeId")){
            Intent intent = new Intent(getApplicationContext(), Orders.class);
            startActivity(intent);
            finish();
        }

        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if(sharedPreferences.getInt("isLoggedIn", -1) == 1)
        {
            this.finishAffinity();
        }
        else
            super.onBackPressed();
    }
}