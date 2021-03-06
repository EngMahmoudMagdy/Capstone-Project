package com.magdy.mguide.UI;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.magdy.mguide.Data.Constants;
import com.magdy.mguide.Data.Contract;
import com.magdy.mguide.Information;
import com.magdy.mguide.ListInfoListener;
import com.magdy.mguide.NotiService;
import com.magdy.mguide.R;
import com.squareup.picasso.Picasso;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity implements ListInfoListener {

    public boolean mTwoPane;
    CircleImageView headerImage ;
    TextView navName, navEmail;
    FirebaseAuth mauth;
    FirebaseAuth.AuthStateListener mauthListener;
    BroadcastReceiver broadcastReceiver;
    CoordinatorLayout coordinatorLayout;
    MainActivityFragment mainActivityFragment;
    int type = 0;
    Snackbar snackbar;
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;
    private Menu menu;
    private void printKeyHash() {
        // Add code to print out the key hash
        try {
            @SuppressLint("PackageManagerGetSignatures") PackageInfo info = getPackageManager().getPackageInfo("com.magdy.mguide", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.w("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("KeyHash:", e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("KeyHash:", e.toString());
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        printKeyHash();
        Log.w("creating Main ", " here");
        coordinatorLayout = findViewById(R.id.coordinator);
        FrameLayout flPanel2 = findViewById(R.id.fl_panel2);

        mTwoPane = (null != flPanel2);

        mainActivityFragment = new MainActivityFragment();

        if (savedInstanceState == null) {
            mainActivityFragment.setListInfoListenter(this);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fl_panel1, mainActivityFragment)
                    .commit();
        } else {
            mainActivityFragment.setListInfoListenter(this);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fl_panel1, mainActivityFragment)
                    .commit();
        }


        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));

        mDrawer = findViewById(R.id.drawer_layout);//layout itself
        NavigationView nvDrawer = findViewById(R.id.nvView);
        menu = nvDrawer.getMenu();
        View header = nvDrawer.inflateHeaderView(R.layout.nav_header);
        navEmail = header.findViewById(R.id.emailtext);
        navName = header.findViewById(R.id.uname);
        headerImage = header.findViewById(R.id.imageheader);
        mDrawer.addDrawerListener(drawerToggle);


        setupDrawerContent(nvDrawer);
        drawerToggle = setupDrawerToggle();
        drawerToggle.setDrawerIndicatorEnabled(true);
        Intent mServiceIntent = new Intent(getBaseContext(), NotiService.class);
        mServiceIntent.setData(Uri.parse("data"));
        startService(mServiceIntent);
        snackbar = Snackbar
                .make(coordinatorLayout, getString(R.string.no_movies_inter), Snackbar.LENGTH_INDEFINITE)
                .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mainActivityFragment = new MainActivityFragment(type);
                        mainActivityFragment.setListInfoListenter(MainActivity.this);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fl_panel1, mainActivityFragment)
                                .commit();

                    }
                });
        snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
        installListener();


        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference().child("users");

        mauth = FirebaseAuth.getInstance();
        mauthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    menu.findItem(R.id.nav_log).setTitle(R.string.login);
                    navName.setText(R.string.app_name);
                    navEmail.setText(R.string.login);
                } else {
                    menu.findItem(R.id.nav_log).setTitle(R.string.logout);
                }
            }
        };
        mauth.addAuthStateListener(mauthListener);
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null) {
                    navName.setText(R.string.app_name);
                    navEmail.setText(R.string.login);
                } else {
                    if (mauth.getCurrentUser() != null) {
                        DataSnapshot userSnapShot = dataSnapshot.child(mauth.getCurrentUser().getUid());
                        String name = String.format(Locale.getDefault(),"%s %s",userSnapShot.child(Constants.INFO).child(Constants.FIRST_NAME).getValue(String.class),userSnapShot.child(Constants.INFO).child(Constants.LAST_NAME).getValue(String.class));
                        String email = userSnapShot.child(Constants.INFO).child(Constants.EMAIL).getValue(String.class);
                        String pic = userSnapShot.child(Constants.INFO).child(Constants.IMAGE).getValue(String.class);
                        if (pic!=null)
                            Picasso.with(getBaseContext()).load(pic).into(headerImage);
                        navName.setText(name);
                        navEmail.setText(email);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


    private void installListener() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        if (broadcastReceiver == null) {


            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    Bundle extras = intent.getExtras();

                    NetworkInfo info = extras.getParcelable("networkInfo");

                    NetworkInfo.State state;
                    if (info != null) {
                        state = info.getState();

                        Log.d("InternalBroadReceiver", info.toString() + " "
                                + state.toString());

                        if (state == NetworkInfo.State.CONNECTED) {

                            snackbar.dismiss();
                            unregisterReceiver(broadcastReceiver);

                        } else {
                            snackbar.setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    mainActivityFragment = new MainActivityFragment(type);
                                    mainActivityFragment.setListInfoListenter(MainActivity.this);
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.fl_panel1, mainActivityFragment)
                                            .commit();
                                }
                            });
                            snackbar.show();
                        }

                    }
                }
            };


            registerReceiver(broadcastReceiver, intentFilter);
        }
    }


    public void setupDrawerContent(NavigationView upDrawerContent) {
        upDrawerContent.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    private void selectDrawerItem(MenuItem menuItem) {

        // Highlight the selected item has been done by NavigationView

        MainActivityFragment mainActivityFragment;

        menuItem.setChecked(true);
        // Set action bar title
        // Close the navigation drawer
        mDrawer.closeDrawers();
        switch (menuItem.getItemId()) {
            case R.id.nav_most:
                type = 0;
                break;
            case R.id.nav_top:
                type = 1;
                break;
            case R.id.nav_fav:
                type = 2;
                break;
            case R.id.nav_log:
                if (mauth.getCurrentUser() == null) {
                    Intent intent = new Intent(getBaseContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    mauth.signOut();
                    //finish();
                }
                break;
            default:
                type = 0;
        }
        try {

            mainActivityFragment = new MainActivityFragment(type);
            mainActivityFragment.setListInfoListenter(this);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fl_panel1, mainActivityFragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        // NOTE: Make sure you pass in a valid toolbar_home reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.
        return new ActionBarDrawerToggle(this, mDrawer, toolbar,
                R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public void setSelectedList(Information info) {
        if (mTwoPane) {
            DetailActivityFragment detailActivityFragment = new DetailActivityFragment();
            Bundle extra = new Bundle();
            extra.putSerializable(Contract.Movie.TABLE_NAME, info);
            extra.putBoolean("pane", mTwoPane);
            detailActivityFragment.setArguments(extra);
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_panel2, detailActivityFragment).commit();

        } else {

            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Contract.Movie.TABLE_NAME, info);
            intent.putExtra("pane", mTwoPane);
            startActivity(intent);
        }

    }

}
