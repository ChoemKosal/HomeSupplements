package com.kh.homesupplement;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import dmax.dialog.SpotsDialog;

public class WelcomeActivity extends AppCompatActivity {

    private static final int APP_REQUEST_CODE = 1000;
    Button btn_supplierLogin;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Suppliers");
        users.setValue("");

        btn_supplierLogin = findViewById(R.id.btn_supplierLogin);
        btn_supplierLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithPhone();
            }
        });



        if (AccountKit.getCurrentAccessToken() != null){
//            final SpotsDialog waittingDialog = new SpotsDialog(WelcomeActivity.this);
//            waittingDialog.show();
//            waittingDialog.setMessage("Please Waiting..");
//            waittingDialog.setCancelable(false);

            AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                @Override
                public void onSuccess(final Account account) {
                    Suppleirs user = new Suppleirs();
                    final String userPhone = account.getPhoneNumber().toString();
                    users.child(userPhone)
                            .setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    //login
                                    users.child(userPhone)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    Intent homeIntent = new Intent(WelcomeActivity.this,MainActivity.class);
                                                    startActivity(homeIntent);

                                                    //waittingDialog.dismiss();
                                                    finish();

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(WelcomeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }

                @Override
                public void onError(AccountKitError accountKitError) {

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE){
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            if (result.getError() != null){
                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }else if (result.wasCancelled()){
                Toast.makeText(this, "Cancel login", Toast.LENGTH_SHORT).show();
                return;
            }else {
                if (result.getAccessToken()!=null){
                    final SpotsDialog waittingDialog = new SpotsDialog(WelcomeActivity.this);
                    waittingDialog.show();
                    waittingDialog.setMessage("Please Waiting...");
                    waittingDialog.setCancelable(false);

                    //get current phone
                   AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                       @Override
                       public void onSuccess(Account account) {
                           final String userPhone = account.getPhoneNumber().toString();
                           //check if exists account
                           users.orderByKey().equalTo(userPhone)
                                   .addListenerForSingleValueEvent(new ValueEventListener() {
                                       @Override
                                       public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                           if (!dataSnapshot.child(userPhone).exists()){
                                               Suppleirs user = new Suppleirs();

                                               user.setPhone(userPhone);
                                               //user.setName(userPhone);
                                               //user.setAvatarUrl("");
                                               //user.setRates("0.0");

                                               //register to fire base
                                               users.child(user.getPhone())
                                                       .setValue(user)
                                                       .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                           @Override
                                                           public void onSuccess(Void aVoid) {

                                                               //login
                                                               users.child(userPhone)
                                                                       .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                           @Override
                                                                           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                               Intent homeIntent = new Intent(WelcomeActivity.this,MainActivity.class);
                                                                               startActivity(homeIntent);

                                                                               waittingDialog.dismiss();
                                                                               finish();

                                                                           }

                                                                           @Override
                                                                           public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                           }
                                                                       });

                                                           }
                                                       }).addOnFailureListener(new OnFailureListener() {
                                                   @Override
                                                   public void onFailure(@NonNull Exception e) {
                                                       Toast.makeText(WelcomeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                   }
                                               });

                                           }
                                           else {

                                               Intent homeIntent = new Intent(WelcomeActivity.this,MainActivity.class);
                                               startActivity(homeIntent);

                                               waittingDialog.dismiss();
                                               finish();
                                           }
                                       }

                                       @Override
                                       public void onCancelled(@NonNull DatabaseError databaseError) {

                                       }
                                   });
                       }

                       @Override
                       public void onError(AccountKitError accountKitError) {
                           Toast.makeText(WelcomeActivity.this, ""+accountKitError.getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                       }
                   });
                }
            }
        }
    }

    private void signInWithPhone() {

        final Intent intent = new Intent(this, AccountKitActivity.class);
        final AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }
}
