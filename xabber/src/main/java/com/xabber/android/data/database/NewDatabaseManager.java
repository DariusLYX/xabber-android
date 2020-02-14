package com.xabber.android.data.database;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.log.LogManager;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import rx.Emitter;
import rx.Observable;

public class NewDatabaseManager implements OnClearListener {

    private static final int CURRENT_DATABASE_VERSION = 0;
    private static final String DATABASE_NAME = "realm_db_xabber";
    private static final String LOG_TAG = NewDatabaseManager.class.getSimpleName();

    private static NewDatabaseManager instance;
    private Observable<Realm> observableListenerInstance;
    private RealmConfiguration realmConfiguration;

    private NewDatabaseManager(){
        Realm.init(Application.getInstance().getApplicationContext());
        realmConfiguration = createRealmConfiguration();
        Realm.compactRealm(realmConfiguration);
    }

    public static NewDatabaseManager getInstance(){
        if ( instance == null) instance = new NewDatabaseManager();
        return instance;
    }

    public Observable<Realm> getObservableListener(){
        if (observableListenerInstance == null) observableListenerInstance = Observable.create(realmEmitter -> {
            final Realm observableRealm = Realm.getDefaultInstance();
            final RealmChangeListener<Realm> listener = realmEmitter::onNext;
            observableRealm.addChangeListener(listener);
            realmEmitter.onNext(observableRealm);
        }, Emitter.BackpressureMode.LATEST);
        return observableListenerInstance;
    }

    @Override
    public void onClear() { deleteRealmDatabase(); }

    private RealmConfiguration createRealmConfiguration(){
        return new RealmConfiguration.Builder()
                .name(DATABASE_NAME)
                .schemaVersion(CURRENT_DATABASE_VERSION)
                .build();
    }

    private void deleteRealmDatabase(){
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                realm.deleteRealm(realm.getConfiguration());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally { if (realm != null) realm.close(); }
        });
    }
}