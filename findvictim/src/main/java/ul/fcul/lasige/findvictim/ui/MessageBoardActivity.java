package ul.fcul.lasige.findvictim.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.PostMessage;
import ul.fcul.lasige.findvictim.data.DatabaseHelper;

public class MessageBoardActivity extends AppCompatActivity {

    private String message = "";

    static ListView mMessageList;
    static SimpleCursorAdapter mAdapter;
    static SQLiteDatabase mDb;

    static Context Context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_message_board);
        Context = MessageBoardActivity.this;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        /*
      The {@link android.support.v4.view.PagerAdapter} that will provide
      fragments for each of the sections. We use a
      {@link FragmentPagerAdapter} derivative, which will keep every
      loaded fragment in memory. If this becomes too memory intensive, it
      may be best to switch to a
      {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        /*
      The {@link ViewPager} that will host the section contents.
     */
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        assert mViewPager != null;
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(mViewPager);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        mDb = dbHelper.getWritableDatabase();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.msg);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageBoardActivity.this);
                builder.setTitle("Post Message");

                final EditText input = new EditText(MessageBoardActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(160)});
                builder.setView(input);

                builder.setPositiveButton("Post", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        message = input.getText().toString();
                        String check = message.trim();

                        if (!check.isEmpty()) {
                            PostMessage newMsg = new PostMessage();
                            newMsg.sender = "Me Myself and I";
                            newMsg.sender_type = "Victim";
                            newMsg.content = message;

                            long currentTime = System.currentTimeMillis() / 1000L;
                            newMsg.timeSent = currentTime;
                            newMsg.timeReceived = currentTime;

                            PostMessage.Store.addMessage(mDb, newMsg);
                        }

                        refreshCursor();
                        mMessageList.smoothScrollToPosition(0);
                        Toast.makeText(MessageBoardActivity.this, "Posted", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    private void refreshCursor() {
        Cursor newCursor = PostMessage.Store.fetchAllMessages(mDb);
        mAdapter.changeCursor(newCursor);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_board, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            SwipeRefreshLayout refresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
            assert refresh != null;
            refresh.setRefreshing(true);
            return true;
        }
        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "ALL";
                case 1:
                    return "VICTIMS";
                case 2:
                    return "RESCUERS";
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_message_board, container, false);

            final Cursor cursor;
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1){
                cursor = PostMessage.Store.fetchAllMessages(mDb);
            }
            else if (getArguments().getInt(ARG_SECTION_NUMBER) == 2){
                cursor = PostMessage.Store.fetchVictimMessages(mDb);
            }
            else if (getArguments().getInt(ARG_SECTION_NUMBER) == 3) {
                cursor = PostMessage.Store.fetchRescuerMessages(mDb);
            }
            else {
                return rootView;
            }

            String[] cols = new String[]{
                    PostMessage.Store.COLUMN_CONTENT,
                    PostMessage.Store.COLUMN_SENDER
            };
            int[] to = new int[]{
                    R.id.messageContent, R.id.messageMeta
            };

            // configure how to populate the list view through an adapter
            mAdapter = new SimpleCursorAdapter(MessageBoardActivity.Context, R.layout.message, cursor, cols, to, 0);
            mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
                    TextView view = (TextView) v;
                    String content = cursor.getString(columnIndex);

                    if (cursor.getColumnName(columnIndex).equals(PostMessage.Store.COLUMN_SENDER)) {
                        String sender_type = cursor.getString(cursor.getColumnIndex(PostMessage.Store.COLUMN_SENDER_TYPE));
                        int timeColIndex = cursor.getColumnIndex(PostMessage.Store.COLUMN_TIME_SENT);
                        int flags = DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_SHOW_YEAR
                                | DateUtils.FORMAT_NUMERIC_DATE;

                        String formatted = DateUtils.formatDateTime(
                                MessageBoardActivity.Context, 1000 * cursor.getLong(timeColIndex), flags);
                        content = sender_type + " - " +content + " at " + formatted;
                    }

                    view.setText(content);
                    return true;
                }
            });

            mMessageList = (ListView) rootView.findViewById(R.id.messagesList);
            mMessageList.setAdapter(mAdapter);

            final SwipeRefreshLayout refresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);
            refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Cursor newCursor = null;
                    if (getArguments().getInt(ARG_SECTION_NUMBER) == 1){
                        newCursor = PostMessage.Store.fetchAllMessages(mDb);
                    }
                    else if (getArguments().getInt(ARG_SECTION_NUMBER) == 2){
                        newCursor = PostMessage.Store.fetchVictimMessages(mDb);
                    }
                    else if (getArguments().getInt(ARG_SECTION_NUMBER) == 3) {
                        newCursor = PostMessage.Store.fetchRescuerMessages(mDb);
                    }
                    mAdapter.changeCursor(newCursor);
                    refresh.setRefreshing(false);
                }
            });

            return rootView;
        }
    }
}
