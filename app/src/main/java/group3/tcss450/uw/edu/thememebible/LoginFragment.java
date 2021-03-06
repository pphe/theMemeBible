package group3.tcss450.uw.edu.thememebible;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Allows user to login to use the application.
 *
 * @author Peter Phe
 * @version 1.0
 */
public class LoginFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "LoginFragment";
    private OnFragmentInteractionListener mListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        // add listener for OK button
        Button b = (Button) v.findViewById(R.id.btnOK);
        b.setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            if (view.getId() == R.id.btnOK) {

                // get edit text references
                EditText etUsername = (EditText) getActivity().findViewById(R.id.editTextUsername);
                EditText etPassword = (EditText) getActivity().findViewById(R.id.editTextPassword);

                // grab strings from the text fields
                String strUsername = etUsername.getText().toString();
                String strPassword = etPassword.getText().toString();

                // check if either are empty
                if (TextUtils.isEmpty(strUsername)) {
                    etUsername.setError("Please enter your username");
                    return;
                } else if (TextUtils.isEmpty(strPassword)) {
                    etPassword.setError("Please enter your password");
                    return;
                }

                // edittext ok to clear now
                etPassword.setText("");

                // initiate login service
                new LoginWebServiceTask(view.getId()).execute(MainActivity.PARTIAL_URL,
                        strUsername, strPassword);
            }
        }
    }

    /**
     * This class contains the service required for logging into the application.
     */
    private class LoginWebServiceTask extends AsyncTask<String, Void, String> {

        private final String SERVICE = "login.php";
        private final int mButtonID;

        public LoginWebServiceTask(int theButtonID) {
            mButtonID = theButtonID;
        }

        @Override
        protected void onPreExecute() {
            // disable button since task is running
            getActivity().findViewById(mButtonID).setEnabled(false);
        }

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length != 3) { // URL, login_name, login_pass
                throw new IllegalArgumentException("Two String arguments required.");
            }

            String response = "";
            HttpURLConnection urlConnection = null;
            String url = strings[0];

            try {
                // build URL
                URL urlObject = new URL(url + SERVICE);

                // connect with URL (returns instance of HttpURLConnection)
                urlConnection = (HttpURLConnection) urlObject.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);

                // prep JSON object to send login info
                JSONObject json = new JSONObject();
                json.put("login_name", strings[1]);
                json.put("login_pass", strings[2]);

                // prep for POST
                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                String data = URLEncoder.encode("json", "UTF-8")
                        + "=" + URLEncoder.encode(json.toString(), "UTF-8");

                // send data to stream
                wr.write(data);
                wr.flush();

                // prep for response from server
                InputStream content = urlConnection.getInputStream();
                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String chunk = "";

                // consume data to build response string
                while ((chunk = buffer.readLine()) != null) {
                    response += chunk;
                }
            } catch (Exception e) {
                response = "Unable to connect, Reason: " + e.getMessage();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            // reenable button
            getActivity().findViewById(mButtonID).setEnabled(true);
            Log.i(TAG, "Response from server: " + response);

            // check if bad response (from exception thrown in doInBackground())
            if (response.startsWith("Unable to")) {
                Toast.makeText(getActivity(), response, Toast.LENGTH_LONG).show();
                return;
            }

            // parse response (a JSON string) and check status code
            try {
                JSONObject json = new JSONObject(response);
                if (json.getInt("status") == 200) // login successful
                {
                    Bundle args = new Bundle();
                    args.putString(getString(R.string.response_key_displayfragment),
                            json.getString("message"));

                    // give MainActivity the args bundle and make callback
                    mListener.getLogin(args);
                    mListener.onFragmentInteraction(mButtonID);
                } else { // login failed (could get specific with either username or pw)
                    String message = json.getString("message");
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonID);
        void getLogin(Bundle args);
    }
}
