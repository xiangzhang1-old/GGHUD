package com.example.hud;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
Basic Activity. Controls MCards.
Customization:
 Voice/Touch Menu
*/
public class MainActivity extends Activity {
    private static final int ADDCARD_SPEECH_REQUEST = 135;

    private MCards mCards;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        mCards = new MCards(this);
        this.setContentView(mCards.mCardScrollView);
    }

    // voice / touch menu
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // user tapped touchpad, do something
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keycode, event);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.add_card:
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    startActivityForResult(intent, ADDCARD_SPEECH_REQUEST);
                    break;
                case R.id.remove_card:
                    mCards.removeCard();
                    break;
                case R.id.wifi_settings:
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    break;
                default:
                    return true;
            }
            return true;
        }

        return super.onMenuItemSelected(featureId, item);   // good practice
    }

    // menu -> Voice Recognition -> addCard
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADDCARD_SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            mCards.addCard(this, spokenText);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

/*
mCards
 create a Card Scroller from XML file, setView
Usage:
 new mCards(context, textArrayList)
 this.setContentView(mCards.mCardScrollView)    // to populate this with mCards
 mCards.mAdapter.notifyDataSetChanged()         // to redraw mCards

 mCards.removeCard()                            // remove current card
 mCards.addCard(context, text)                  // add TEXT card with text
Customization:
 syncs json with firebase

Note:
 textArrayList (textArrayListRef)
    [
        { 'text': text }      // ascii only.
                              // MENU.footnote is blocked by "okay glass"
                              // change code to use TEXT card with footnote and timestamp if u want.
    ]
 substitutionHashMap  (substitutionHashMapRef)
    {
        'lunch': 'lunch. never walk, lest you lose entire afternoon.'
    }
 ideally 1singledefinition, but cards dont have getText, so textArrayList it is.
 removeCard and addCard ought to act on textArrayList, then pass to UI (notifyOnDataSetChanged). But for beauty. a hack.
*/
class MCards {

    private class MCardScrollAdapter extends CardScrollAdapter {
        @Override
        public int getPosition(Object item) {
            return mCardBuilders.indexOf(item);
        }
        @Override
        public int getCount() {
            return mCardBuilders.size();
        }
        @Override
        public Object getItem(int position) {
            return mCardBuilders.get(position);
        }
        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }
        @Override
        public int getItemViewType(int position){
            return mCardBuilders.get(position).getItemViewType();
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCardBuilders.get(position).getView(convertView, parent);
        }
    }

    private ArrayList<HashMap<String, String>> textArrayList = new ArrayList<HashMap<String, String>>();
    private Firebase textArrayListRef;

    private HashMap<String, String> substitutionHashMap = new HashMap();
    private Firebase substitutionHashMapRef;

    private ArrayList<CardBuilder> mCardBuilders;
    public CardScrollView mCardScrollView;
    public MCardScrollAdapter mAdapter;

    private Context context;

    public MCards(final Context context) {
        this.context = context;

        mCardBuilders = new ArrayList<CardBuilder>();

        mAdapter = new MCardScrollAdapter();

        mCardScrollView = new CardScrollView(context);
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();

        textArrayListRef = new Firebase("https://hudfirebaseproject-5b25e-d409d.firebaseio.com/").child("textArrayList");
        // download
        textArrayListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList _ = dataSnapshot.getValue(ArrayList.class);

                if (_ != null) {
                    textArrayList = _;

                    mCardBuilders.clear();
                    for (int i = 0; i < textArrayList.size(); i++) {
                        mCardBuilders.add(new CardBuilder(context, CardBuilder.Layout.TEXT)
                                .setText(textArrayList.get(i).get("text")));
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                System.out.println("onCancelled error:" + error.getMessage());
            }
        });

        substitutionHashMapRef = new Firebase("https://hudfirebaseproject-5b25e-d409d.firebaseio.com/").child("substitutionHashMapRef");
        substitutionHashMapRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap _ = dataSnapshot.getValue(HashMap.class);
                if (_ != null)
                    substitutionHashMap = _;
            }

            @Override
            public void onCancelled(FirebaseError error) {
                System.out.println("onCancelled error:" + error.getMessage());
            }
        });
    }

    public void removeCard() {
        int i = mCardScrollView.getSelectedItemPosition();

        textArrayList.remove(i);

        // or, simply, setmCardBuilder; notifyDataSet changed should be called.
        mCardBuilders.remove(i);
        mCardScrollView.animate(i, CardScrollView.Animation.DELETION);

        // upload
        textArrayListRef.setValue(textArrayList);
    }

    public void addCard(Context context, String text) {
        if (substitutionHashMap.containsKey(text)) {
            text = substitutionHashMap.get(text);
        }

        HashMap<String, String> _ = new HashMap<String, String>();
        _.put("text", text);
        textArrayList.add(0, _);

        mCardBuilders.add(0, new CardBuilder(context, CardBuilder.Layout.TEXT).setText(text));
        mCardScrollView.animate(0, CardScrollView.Animation.INSERTION);

        // upload
        textArrayListRef.setValue(textArrayList);
    }

}