package com.example.lens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MainActivity<LoginActivity> extends AppCompatActivity {

    static final int GOOGLE_SIGN_IN = 123;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    Button btn_login, btn_logout;
    TextView text, account_name;
    ImageView image;
    ProgressBar progressBar;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_login = findViewById(R.id.login);
        btn_logout = findViewById(R.id.logout);
        text = findViewById(R.id.welcome);
        account_name = findViewById(R.id.name);
        image = findViewById(R.id.logo);
        progressBar = findViewById(R.id.progress_circular);

        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();








        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignInGoogle();
            }
        });
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logout();
            }
        });

        if (mAuth.getCurrentUser() != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            updateUI(user);

        }
    }


    public void SignInGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("TAG", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressBar.setVisibility(View.INVISIBLE);

                            Log.d("TAG", "signInWithCredential:success");

                            FirebaseUser user = mAuth.getCurrentUser();

                            MainActivity.this.updateUI(user);
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);

                            Log.w("TAG", "signInWithCredential:failure", task.getException());

                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            MainActivity.this.updateUI(null);
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("TAG", "Google sign in failed", e);
            }
        }
    }

    public void getData(){
        final FirebaseUser user = mAuth.getCurrentUser();

        final String uid = user.getUid();
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //String user_name = dataSnapshot.child(uid).child("username").getValue().toString();
                String user_name = dataSnapshot.child("Users").child(uid).child("username").getValue().toString();
                account_name.setText(user_name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void writeNewUser(String userId, String name, String email) {
        User user = new User(name, email);

        mDatabase.child("Users").child(userId).setValue(user);
    }
    private void updateUI(FirebaseUser user) {

        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            String userID =  user.getUid();


            writeNewUser(userID, name, email);

            getData();





            text.setVisibility(View.INVISIBLE);
            image.setVisibility(View.INVISIBLE);
            btn_logout.setVisibility(View.VISIBLE);
            btn_login.setVisibility(View.INVISIBLE);
        } else {
            image.setVisibility(View.VISIBLE);
            btn_logout.setVisibility(View.INVISIBLE);
            btn_login.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            account_name.setText("");
        }
    }

    private void Logout() {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        MainActivity.this.updateUI(null);
                    }
                });
    }
}