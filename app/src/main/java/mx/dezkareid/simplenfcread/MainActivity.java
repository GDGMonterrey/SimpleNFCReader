package mx.dezkareid.simplenfcread;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private NfcAdapter adapter;
    private Tag tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = NfcAdapter.getDefaultAdapter(this);
        Button enviarMensaje = (Button) findViewById(R.id.send_button);
        enviarMensaje.setOnClickListener(this);
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
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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
                TextView textView = (TextView) findViewById(R.id.nfc_text);
                textView.setText(content);

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
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    private void writeNFC(String message){
        NdefMessage ndefMessage = createMessage(message);
        if(ndefMessage == null){
            return;
        }
        writeMessage(tag, ndefMessage);
    }

    private NdefMessage createMessage(String message){
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], textToByteArray(message));
        return new NdefMessage(new NdefRecord[]{ ndefRecord});;
    }

    private byte[] textToByteArray(String message){
        byte[] data = null;
        try{
            byte[] language = Locale.getDefault().getLanguage().getBytes("UTF-8");
            Charset encoding = Charset.forName("UTF-8");
            final byte[] text = message.getBytes(encoding);
            int utfBit = 0;
            char status = (char)(utfBit + language.length);
            data = new byte[1 + language.length + text.length];
            data[0] = (byte)status;
            System.arraycopy(language, 0, data, 1, language.length);
            System.arraycopy(text, 0, data, 1 + language.length, text.length);


        }catch (Exception e){
            Log.d("ErrorTextoByteArray",e.getMessage());
        }
        return data;
    }

    private void formatTag(Tag tag, NdefMessage message){
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable == null){
                return;
            }
            ndefFormatable.connect();
            ndefFormatable.format(message);
            ndefFormatable.close();
            showMessage("Tag grabada");
        }catch (Exception e){
            Log.d("ExceptionFormat", e.getMessage());
        }

    }
    private void writeMessage(Tag tag, NdefMessage message){
        try {
            if(tag == null){
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null){
                formatTag(tag,message);
            }else{
                ndef.connect();
                ndef.writeNdefMessage(message);
                ndef.close();
                showMessage("Tag grabada");
            }
        }catch (Exception e){

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.send_button: {
                EditText text = (EditText) findViewById(R.id.message);
                writeNFC(text.getText().toString());
            }
            default:{

            }
        }
    }
}
