package org.exasperation.NumberSwap;

import java.util.ArrayList;
import java.util.List;
import java.net.Socket;

import android.app.Activity;
import android.os.Bundle;

import android.content.ContentValues;
import android.content.ContentProviderOperation;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.content.ContentUris;
import android.telephony.TelephonyManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class NearbyList extends Activity implements MessageListener
{
    public static final String TAG = "org.exasperation.NumberSwap";
    Context c;
    Client client;
    WaitTask waitTask;
    ListView userListView;
    ArrayList<User> nameList;
    Button refreshButton;
    String mPhoneNumber;

    LocationProvider networkLocationProvider;
    LocationProvider GPSLocationProvider;
    LocationManager locationManager;
    LocationListener locationListener;

    Location currentLocation;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        c = this;
        setTitle("People Nearby");
        client = new Client(this, "numberswap.chickenkiller.com", 38495);
        setContentView(R.layout.browser);
        userListView = (ListView) findViewById(R.id.user_list);
        refreshButton = (Button) findViewById(R.id.refresh_button);

        nameList = new ArrayList<User>();
            
        TelephonyManager tMgr =(TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneNumber = tMgr.getLine1Number();

        userListView.setAdapter(new UserAdapter(this, R.layout.user_row, nameList));
    }
    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart()");

        super.onStart();

        client.connect();

        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                client.sendMessage("init "+currentLocation.getLatitude()+" "
                                          +currentLocation.getLongitude()+" "
                                          +"Junseok"+" "
                                          +"4545454545");
            }
        });

        
        userListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView lv, View v, int position, long id)
            {
                client.sendMessage("req "+nameList.get(position).getName().replace(' ','_'));
                //waitTask = new WaitTask();
                //waitTask.execute(nameList.toArray(new User[0]));
            }
        });
        //currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (location != null)
                    currentLocation = location;
                if (location.getProvider() == LocationManager.GPS_PROVIDER)
                    locationManager.removeUpdates(locationListener);

                if (currentLocation == null)
                    client.sendMessage("test");
                else
                    client.sendMessage("init "+location.getLatitude()+" "
                                              +location.getLongitude()+" "
                                              +"Junseok"+" "
                                              +"4545454545");
                    //client.sendMessage(""+currentLocation.getLatitude());
                Log.d(TAG, "location changed");
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "status changed: "+provider + " " + status);
            }

            public void onProviderEnabled(String provider) {
                Log.d(TAG, "provider disabled:" + provider);
            }
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "provider disabled:" + provider);
            }
        };
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume()");

        super.onResume();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause()");

        super.onPause();
        client.disconnect();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop()");

        super.onStop();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    public void onMessageReceived(String cmd, final ArrayList<String> params)
    {
        Log.d(TAG, "onMessageReceived()");
        Log.d(TAG, cmd +" on...");
        for (String s : params) {
            Log.d(TAG, "===="+s);
        }
        if (cmd.equals("names")) {
            nameList.clear();
            //final ArrayList<User> newUsers = new ArrayList<User>();
            for (String name : params)
            {
                nameList.add(new User(name));
            }
            Runnable update = new Runnable() {
                public void run(){
                    userListView.setAdapter(new UserAdapter(c, R.layout.user_row, nameList));
                    userListView.invalidate();
                }
            };
            userListView.post(update);
        }
        if (cmd.equals("req"))
        {
            Runnable promptUser = new Runnable() {
                public void run(){
                    AlertDialog.Builder builder = new AlertDialog.Builder(c);
                    builder.setTitle("Confirm swap")
                           .setMessage("Swap numbers with "+params.get(0).replace('_',' ')+"?")
                           .setCancelable(true)
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                   Log.d(TAG, "THE CONTACTS HAVE BEEN SWAPPED REALLY");
                                   client.sendMessage("add " + params.get(0));
                               }
                           })
                           .setNegativeButton("No", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                   dialog.cancel();
                               }
                           });
                    builder.create().show();
                }
            };
            runOnUiThread(promptUser);
            Log.d(TAG, "REQUEST RECEIVED AND PROCESSED");
        }
        if (cmd.equals("add"))
        {
            String nameToAdd = params.get(0);
            String numberToAdd = params.get(1);
            addContact(nameToAdd, numberToAdd);
            Runnable toastUser = new Runnable() {
                public void run(){
                    Toast.makeText(c, "Numbers swapped!", Toast.LENGTH_SHORT).show();
                }
            };
        }

    }

    private class UserAdapter extends ArrayAdapter<User> 
    {
        private List<User> users;
        public UserAdapter(Context context, int textViewResourceId, List<User> users)
        {
            super(context, textViewResourceId, users);
            this.users = users;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.user_row, null);
            }
            TextView userName = (TextView) v.findViewById(R.id.user_name);

            User u = users.get(position);
            if (u != null) {
                userName.setText(u.getName());
            }
            return v;
        }
    }
    
    public void addContact(String name, String phone) {
        String DisplayName = name;
        String MobileNumber = phone;
        String HomeNumber = "1111";
        String WorkNumber = "2222";
        String emailID = "email@nomail.com";
        String company = "bad";
        String jobTitle = "abcd";
        
        ArrayList<ContentProviderOperation> ops = 
            new ArrayList<ContentProviderOperation>();
        
        ops.add(ContentProviderOperation.newInsert(
            ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
        );
        
        //------------------------------------------------------ Names
        if(DisplayName != null)
        {           
            ops.add(ContentProviderOperation.newInsert(
                ContactsContract.Data.CONTENT_URI)              
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,     
                    DisplayName).build()
            );
        } 
        
        //------------------------------------------------------ Mobile Number                      
        if(MobileNumber != null)
        {
            ops.add(ContentProviderOperation.
                newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, MobileNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, 
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
            );
        }

        // Asking the Contact provider to create a new contact                  
        try 
        {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } 
        catch (Exception e) 
        {               
            e.printStackTrace();
            Toast.makeText(c, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /*
        ContentValues values = new ContentValues();
        values.put(Data.DISPLAY_NAME, name);
        Uri rawContactUri = getContentResolver().insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        long contactId = getContactId(c, rawContactId);
        System.out.println("rawContactId = " + rawContactId);
        System.out.println("contactId = " + contactId);

        values.clear();
        values.put(Phone.NUMBER, phone);
        values.put(Phone.TYPE, Phone.TYPE_OTHER);
        values.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        getContentResolver().insert(Data.CONTENT_URI, values);

        values.clear();
        values.put(Data.MIMETYPE, Data.CONTENT_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        getContentResolver().insert(Data.CONTENT_URI, values);

        values.clear();
        values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        getContentResolver().insert(Data.CONTENT_URI, values);
    }
    
    public static long getContactId(Context context, long rawContactId) {
        Cursor cur = null;
        try {
            cur = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[] { ContactsContract.RawContacts.CONTACT_ID }, ContactsContract.RawContacts._ID + "=" + rawContactId, null, null);
            if (cur.moveToFirst()) {
                return cur.getLong(cur.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return -1;
    }*/


    private class WaitTask extends AsyncTask<User, Integer, Boolean>
    {
        Context c;
        ProgressDialog dialog;
        public WaitTask(Context context){
            c = context;
        }
        protected void onPreExecute() {
            dialog = new ProgressDialog(c);
            dialog.setTitle("Waiting");
            dialog.setMessage("Waiting for a response...");
            dialog.setIndeterminate(true);
            dialog.show();
        }
        protected Boolean doInBackground(User... otherUser) {
            for (int i = 10; i > 0; i--)
            {
                Log.d(TAG, "DOING THINGS");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                if (isCancelled()) return true;
            }
            return false;
        }

        protected void onPostExecute(Boolean cancelled) {
            dialog.dismiss();
            if (!cancelled)
            {
                Toast t = Toast.makeText(c, "Request timed out!", Toast.LENGTH_SHORT);
                t.show();
            }
            else
            {
                //ADD TO CONTACTS
                Log.d(TAG, "ADDING TO CONTACTS FOSHO BRU");
            }
        }
    }

}
