package com.tareksaidee.bcconnected;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 123;


    RecyclerView mRecyclerView;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRoomsDatabaseReference;
    private ChildEventListener mRoomsEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private RoomsAdapter mRoomsAdapter;
    private String mUsername;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerOptions;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView navigationView;
    private RoundedImageView userPicView;
    private TextView usernameTextView;
    private TextView userEmailTextView;
    private View navHeaderView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsername = ANONYMOUS;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRoomsDatabaseReference = mFirebaseDatabase.getReference().child("rooms");
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description */
                R.string.navigation_drawer_close  /* "close drawer" description */
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navHeaderView = navigationView.getHeaderView(0);
        userPicView = (RoundedImageView) navHeaderView.findViewById(R.id.imageView_header);
        usernameTextView = (TextView) navHeaderView.findViewById(R.id.username_header);
        userEmailTextView = (TextView) navHeaderView.findViewById(R.id.user_email_header);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_rooms);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRoomsAdapter = new RoomsAdapter(this);
        mRecyclerView.setAdapter(mRoomsAdapter);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(MainActivity.this, "You're signed in!", Toast.LENGTH_SHORT).show();
                    initilizeSignIn(user);
                } else {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                    cleanUpOnSignout();
                }
            }
        };
    }


    void initilizeSignIn(FirebaseUser user) {
        mUsername = user.getDisplayName();
        mRoomsAdapter.setUserName(mUsername);
        attachDatabaseReadListener();
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .into(userPicView);
        }
        usernameTextView.setText(mUsername);
        userEmailTextView.setText(user.getEmail());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_new_room:
                Intent intent = new Intent(this, CreateRoom.class);
                intent.putExtra("rooms", mRoomsAdapter.getRooms());
                startActivity(intent);
                return true;
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                break;
        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void attachDatabaseReadListener() {
        if (mRoomsEventListener == null) {
            mRoomsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    mRoomsAdapter.addRoom(dataSnapshot.getValue(Room.class));
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            mRoomsDatabaseReference.addChildEventListener(mRoomsEventListener);
        }
    }

    void detachReadListener() {
        if (mRoomsEventListener != null) {
            mRoomsDatabaseReference.removeEventListener(mRoomsEventListener);
            mRoomsEventListener = null;
        }
    }

    void cleanUpOnSignout() {
        mUsername = ANONYMOUS;
        mRoomsAdapter.clear();
        mRoomsAdapter.setUserName(null);
        detachReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null)
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        cleanUpOnSignout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                //signed in
            } else {
                if (resultCode == RESULT_CANCELED) {
                    finish();
                }
            }

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}

//TODO sort rooms by whatever
//TODO my rooms
//TODO send documents and audio
//TODO long click to save
//TODO enter to send/ok
//TODO number of participants per room