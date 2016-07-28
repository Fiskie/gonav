package fiskie.gonav.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import fiskie.gonav.AppSettings;
import fiskie.gonav.R;
import fiskie.gonav.auth.CredentialValidator;
import fiskie.gonav.auth.PTCCredentialsPair;
import okhttp3.OkHttpClient;

public class SetupPTCActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_ptc);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Authenticate with PTC");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void launchGoogleSSOIntent(View v) {
        startActivity(new Intent(this, SetupGoogleActivity.class));
    }

    public void launchPTCAccountCreationIntent(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://club.pokemon.com/us/pokemon-trainer-club/sign-up/"));
        startActivity(intent);
    }

    public void onVerify(final View v) {
        String username = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        v.setEnabled(false);

        final PTCCredentialsPair pair = new PTCCredentialsPair(username, password);
        final Activity self = this;
        final ProgressDialog progressDialog = new ProgressDialog(this);

        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        new AsyncTask<PTCCredentialsPair, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(PTCCredentialsPair... ptcCredentialsPairs) {
                OkHttpClient client = new OkHttpClient();

                try {
                    PTCCredentialsPair pair = ptcCredentialsPairs[0];
                    PtcCredentialProvider provider = new PtcCredentialProvider(client, pair.getUsername(), pair.getPassword());
                    new CredentialValidator(provider).test();
                } catch (final RemoteServerException | LoginFailedException e) {
                    e.printStackTrace();

                    self.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(self, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            v.setEnabled(true);
                        }
                    });

                    return false;
                } finally {
                    self.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                        }
                    });
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    (new AppSettings(getSharedPreferences("gonav", MODE_PRIVATE), getAssets())).setPTCCredentialsPair(pair);
                    startActivity(new Intent(self, MainActivity.class));
                }
            }
        }.execute(pair);
    }
}