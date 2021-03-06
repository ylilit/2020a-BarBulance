package android.technion.com;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FacebookActivity extends AppCompatActivity
        implements ForceUpdateChecker.OnUpdateNeededListener{

    private Button signInButton;
    private Button signInAddEventButton;
    private LoginButton loginButton;

    private static final String TAG = "FacebookLogin";
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private Database db;
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager;

    /////////////////////////////////////////////////
    //          handler FB Access Tokens           //
    /////////////////////////////////////////////////

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            final String uid = user.getUid();

                            FirebaseFirestore db2;
                            db2 = FirebaseFirestore.getInstance();
                            DocumentReference usersRef = db2.collection("Users").document(uid);
                            usersRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            //update only token
                                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                            String email = "";
                                            if(document.get("userEmail")!=null) {
                                                email = document.get("userEmail").toString();
                                            }
                                            String name = document.get("userName").toString();
                                            String phone = document.get("userPhoneNumber").toString();
                                            //String email = document.get("fcmtoken").toString();
                                            User user = new User(name,email,phone,uid,"");
                                            db.addUserToDatabase(user);

                                        } else {

                                            //add new user
                                            Log.d(TAG, "No such document");
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            String name = user.getDisplayName();
                                            String email = user.getEmail();
                                            String phone = user.getPhoneNumber();
                                            if(phone == null){ phone = ""; }
                                            User barbulanceUser = new User(name, email, phone, uid, "");
                                            db.addUserToDatabase(barbulanceUser);
                                        }
                                    } else {
                                        Log.d(TAG, "get failed with ", task.getException());
                                    }
                                }
                            });

                            Intent next = new Intent(FacebookActivity.this, MainActivity.class);
                            finish();
                            startActivity(next);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(FacebookActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    ///////////////////////////////////////
    //          onUpdateNeeded           //
    ///////////////////////////////////////

    @Override
    public void onUpdateNeeded(final String updateUrl) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New version available")
                .setMessage("Please, update app to new version to continue reposting.")
                .setPositiveButton("Update",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                redirectStore(updateUrl);
                            }
                        }).setNegativeButton("No, thanks",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();
        dialog.show();
    }

    private void redirectStore(String updateUrl) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //////////////////////////////////
    //          on Create           //
    //////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook);

        //forceUpdateConfig
        final FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // set in-app defaults
        Map<String, Object> remoteConfigDefaults = new HashMap();
        remoteConfigDefaults.put(ForceUpdateChecker.KEY_UPDATE_REQUIRED, false);
        remoteConfigDefaults.put(ForceUpdateChecker.KEY_CURRENT_VERSION, "1.0");
        remoteConfigDefaults.put(ForceUpdateChecker.KEY_UPDATE_URL,
                "https://play.google.com/store/apps/details?id=com.technion.android");

        firebaseRemoteConfig.setDefaults(remoteConfigDefaults);
        firebaseRemoteConfig.fetch(60) // fetch every minutes
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "remote config is fetched.");
                            firebaseRemoteConfig.activateFetched();
                        }
                    }
                });
        //ForceUpdateConfigEnd

        ForceUpdateChecker.with(this).onUpdateNeeded(this).check();

        db = new Database();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();

        //check if user is already signed in
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        FirebaseUser current = mAuth.getCurrentUser();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired() && current != null;
        if(isLoggedIn) {
            Intent intent = new Intent(FacebookActivity.this, MainActivity.class);
            finish();
            startActivity(intent);
        }

        //singing in
        loginButton = findViewById(R.id.login_button);

        loginButton.setReadPermissions(Arrays.asList("email", "public_profile"));
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
                loginButton.setVisibility(View.GONE);
                signInAddEventButton.setVisibility(View.GONE);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });

        // Add an event - unregistered user
        signInAddEventButton = findViewById(R.id.signInAddEventButton);
        signInAddEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FacebookActivity.this, AddEventActivity.class);
                startActivity(intent);
            }
        });

//        /* Temporary bypassing facebook login *////////////////////////////////////////
//        signInButton = findViewById(R.id.signInButton);
//        signInButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(FacebookActivity.this, MainActivity.class);
//                finish();
//                startActivity(intent);
//            }
//        });
//        //////////////////////////////////////////////////////////////////////////////

    }

    ////////////////////////////////////////////
    //          on Activity Result            //
    ////////////////////////////////////////////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //////////////////////////////////
    //          on Start            //
    //////////////////////////////////

    @Override
    public void onStart() {
        super.onStart();

        //check if user is already signed in
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        FirebaseUser current = mAuth.getCurrentUser();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired() && current!=null;
        if(isLoggedIn) {
            Intent intent = new Intent(FacebookActivity.this, MainActivity.class);
            finish();
            startActivity(intent);
        }
    }
}
