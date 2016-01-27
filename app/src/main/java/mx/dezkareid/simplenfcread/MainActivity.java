package mx.dezkareid.simplenfcread;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

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
        for(Parcelable p : parcelables){
            readFromParcelable(p);
        }
    }

    private void readFromParcelable(Parcelable parcelable) {
        NdefMessage ndefMessage = (NdefMessage) parcelable;
        NdefRecord[] ndefRecords = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : ndefRecords){
            readFromNDEFRecord(ndefRecord);
        }

    }

    private void readFromNDEFRecord(NdefRecord ndefRecord){
        short tnf = ndefRecord.getTnf();
        if (tnf == NdefRecord.TNF_WELL_KNOWN){
            if(Arrays.equals(NdefRecord.RTD_TEXT, ndefRecord.getType())){
                String content = getTextFromRecord(ndefRecord);
                Log.d("PARCELABLE", content);
                showMessage(content);

            }

        }
    }

    private String getTextFromRecord(NdefRecord ndefRecord){
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload,languageSize+1,
                    payload.length -languageSize -1,textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("TextFromRecord", e.getMessage(), e);
        }
        return tagContent;
    }

    private void showMessage(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG);
    }
}
