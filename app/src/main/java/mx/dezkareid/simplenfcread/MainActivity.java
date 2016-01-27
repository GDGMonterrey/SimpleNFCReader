package mx.dezkareid.simplenfcread;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!adapterWorks()){
            return;
        }
        handler(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!adapterWorks())
            return;
        enableSystemForegroundNFC();
    }

    public boolean adapterWorks(){
        if(adapter == null){
            return false;
        }
        return adapter.isEnabled();
    }

    private void enableSystemForegroundNFC(){
        Intent intent = new Intent(this,this.getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        IntentFilter[] intentFilter = new IntentFilter[]{new IntentFilter(adapter.ACTION_TAG_DISCOVERED) };
        adapter.enableForegroundDispatch(this,pendingIntent,intentFilter,null);
    }
    private void handler(Intent intent) {
        String action = intent.getAction();
        if ( NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            extractParcelableInfo(intent.getParcelableArrayExtra(adapter.EXTRA_NDEF_MESSAGES));
        }
    }

    private void extractParcelableInfo( Parcelable [] parcelables){
        Log.d("PARCELABLE", parcelables.length + "");
    }
}
