package www.faehse.de.todo;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();


    public GoogleMap mMap;
    private LocationManager manager;
    private LocationListener listener;
    private Location location;
    private String provider;
    LatLng latlng, daten;
    MarkerOptions options;
    StringBuilder temp, stringlatLng;
    // Testzaehler für den LocationListener Aufruf
    int zaehler = 0;
    // Für den Aufruf der Umkreissuche von POI(s). Einmal für den Stringbuilder und auch für Circle-Methode
    float RADIUS = 1500;
    // Parameter für den Location-Update Aufruf
    int TIME = 1000;
    int UMKREIS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // LocationManager-Instanz ermitteln
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //latlng = new LatLng(location.getLatitude(), location.getLongitude());

        // Liste mit Namen aller Provider erfragen
        List<String> providers = manager.getAllProviders();
        for (String name : providers) {
            LocationProvider lp = manager.getProvider(name);
            Log.d(TAG,
                    lp.getName() + " --- isProviderEnabled(): "
                            + manager.isProviderEnabled(name));
            Log.d(TAG, "requiresCell(): " + lp.requiresCell());
            Log.d(TAG, "requiresNetwork(): " + lp.requiresNetwork());
            Log.d(TAG, "requiresSatellite(): " + lp.requiresSatellite());
        }
        // Provider mit grober Auflösung
        // und niedrigen Energieverbrauch
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        provider = manager.getBestProvider(criteria, true);
        Log.d(TAG, provider);
        // LocationListener-Objekt erzeugen
        listener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                Log.d(TAG, "onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider) {

                Log.d(TAG, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider) {

                Log.d(TAG, "onProviderDisabled()");
            }

            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "-------------> onLocationChanged()");
                if (location != null) {
                    mMap.clear();
                    latlng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                    .title("Aktueller Standort : "+zaehler)
                    .position(latlng));

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latlng).zoom(14).build();

                    mMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(cameraPosition));

                    mMap.addCircle(new CircleOptions()
                            .center(latlng)
                            .radius(RADIUS)
                            .fillColor(0x5500ff00)
                            .strokeWidth(0.0f));
                    new PoiErfragen().execute();
                    zaehler++;

                }
            }


        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        manager.requestLocationUpdates(provider, TIME, UMKREIS,
                listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        manager.removeUpdates(listener);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Verändern der Googlekarten Ansicht
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Deaktiviren der Google Buttons für GoogleMaps und Google Routing Funktionen
        mMap.getUiSettings().setMapToolbarEnabled(true);

    }

    public String buildURL() {
        /* Hier wird über einen StringBuilder die https Adresse zur aktuellen POI Abfrage über eine json-Datei realisert.
        * Die http-Suchanfrage hat die Form: https://maps.googleapis.com/maps/api/place/nearbysearch/json
        * ?location=STRING LATITUDE, STRING LONGITUDE&radius=1000&name=rewe&key=YOUR_GOOGLE_KEY
        * Feste Latitude und Longitude Werte um String Builder zu testen.
        * final String latitude = "50.06226155";
        * final String longitude = "8.21733695";
        */
        location = manager.getLastKnownLocation(provider);
        double receivelatitude = location.getLatitude();
        double receivelongitude = location.getLongitude();
        final String latitude = String.valueOf(receivelatitude);
        final String longitude = String.valueOf(receivelongitude);
        //Log.d("---------->", ""+latitude);
        //Log.d("---------->", ""+longitude);
        stringlatLng = new StringBuilder(latitude+","+longitude);
        String latLngRequest = stringlatLng.toString();

        final String GOOGLE_KEY = "AIzaSyBOy5KtLlnqgdSvDn7QFlZGJv_NA02GrP8 ";

        temp = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?location=");
        temp.append(latLngRequest);
        temp.append("&radius="+RADIUS);
        temp.append("&types=grocery_or_supermarket");
        //temp.append("&name=rewe");
        temp.append("&sensor=true");
        temp.append("&key=");
        temp.append(GOOGLE_KEY);
        String urlResult = temp.toString();
        //Log.d("------------>: ", urlResult);
        return urlResult;

    }

    private class PoiErfragen extends AsyncTask<Void, Void, String> {

        private static final String LOG_TAG = "TestApp";
        HttpURLConnection connection = null;


        @Override
        protected String doInBackground(Void... args) {

            final StringBuilder json = new StringBuilder();

            try {
                // Verbindung zum WebService
                URL url = new URL(buildURL());

                connection = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(connection.getInputStream());

                // json Daten lesen und in den  StringBuilder schreiben
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    json.append(buff, 0, read);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to service", e);
                //throw new IOException("Error connecting to service", e); //uncaught
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return json.toString();
        }

        // Wird nach der kompletten Ausführung der Methode protected String doInBackground(Void... args) ausgeführt
        @Override
        protected void onPostExecute(String json) {

            try {
                // De - Serialisierung des JSON-String in ein Array mit Geometrie-Objekten.
                JSONObject mainObj = new JSONObject(json);
                // Das Array in dem die Geometrie-Objekte enthalten sind, heisst results.
                JSONArray jArray = mainObj.optJSONArray("results");
                /*
                 *
                 * Wenn die json Abfrage erledigt wurde, wird geschaut ob POI(s) von Typ grocery_or_supermarket gefunden
                 * wurden. Daraufin wird bei jArray != null eine Testweise Handlungsempfehlung Bananen kaufen ausgegeben.
                 *
                */
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject temp = jArray.optJSONObject(i);
                    JSONObject loc = temp.optJSONObject("geometry").optJSONObject("location");
                    String stillOpen = jArray.getJSONObject(i).getJSONObject("opening_hours").getString("open_now");
                    if (stillOpen.equals(" ")){
                        stillOpen = "false";
                    }
                    double lat = Double.parseDouble(loc.getString("lat"));
                    double lng = Double.parseDouble(loc.getString("lng"));
                    LatLng latLng = new LatLng(lat, lng);
                    String resultFilter = jArray.getJSONObject(i).optString("name");

                    /*
                     * Wenn die json Abfrage erledigt wurde, wird geschaut ob POI(s) mit dem Namen Rewe oder Aldi Süd vorhanden sind
                     * Daraufin wird durch den Aufruf von toast.show eine Handlungsempfehlung ausgegeben.
                     *
                    */
                    if (resultFilter.equalsIgnoreCase("REWE") | resultFilter.equalsIgnoreCase("REWE City") && stillOpen.equals("true")) {
                        // Todo: Bei einem Treffer die Farbe der dazugehörigen POI-Marker ändern.
                        Context context = getApplicationContext();
                        String geschaeft = jArray.getJSONObject(i).optString("name");
                        String adresse = jArray.getJSONObject(i).optString("vicinity");
                        CharSequence text = "Eine eingetragene To-do kann hier " + ":" + geschaeft + ", " + adresse + " " + "erledigt werden.";
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(context, text, duration);
                        // Ausgabe der Handlungsempfehlung
                        toast.show();
                    }

                    // Für jeden POI wird auf der Karte ein Marker erzeugt.
                    JSONObject attr = temp.optJSONObject("geometry");
                    mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title(jArray.getJSONObject(i).optString("name"))
                            // Todo Eventuell noch anzeigen, ob Geschaeft noch offen aus String stillOpen
                            .snippet(jArray.getJSONObject(i).optString("vicinity"))// +"Geschäft geöffnet?: "+stillOpen))
                            .position(latLng));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }

        }
    }
}

