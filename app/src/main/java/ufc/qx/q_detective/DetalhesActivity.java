package ufc.qx.q_detective;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

import ufc.qx.q_detective.dao.DenunciaDAO;
import ufc.qx.q_detective.dominio.Denuncia;

/**
 * Created by rodrigo on 11/12/17.
 */

public class DetalhesActivity extends Activity {

    private String urlBase = "http://maps.googleapis.com/maps/api/staticmap" +
            "?size=400x400&sensor=true&markers=color:red|%s,%s&key=AIzaSyCNgjtgzjX7cO-Qy26hSpjdgjuIBGjqE8M";

    private WebView mWebView;
    private Bundle bundle = null;
    private  int id;
    private DenunciaDAO denunciaDAO;
    private Denuncia denuncia;
    private LocationManager locationManager;
    private VideoView videoView;
    private ImageView imageView;
    private TextView descricao, categoria, usuario, data;
    private Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalhes_activity);

        descricao = findViewById(R.id.descricaoDenuncia);
        categoria = findViewById(R.id.categoriaDenuncia);
        usuario = findViewById(R.id.usuarioDenuncia);
        data = findViewById(R.id.data);
        imageView = (ImageView) findViewById(R.id.imagemDetalhes);
        mWebView = findViewById(R.id.mapa);
        denunciaDAO = new DenunciaDAO(this);
        bundle = this.getIntent().getExtras();
        videoView  = (VideoView)  findViewById(R.id.videoView);
        //videoView.setVisibility(View.INVISIBLE);


        if(bundle !=null) {
            id = bundle.getInt("id", 0);
        }
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(false);
        denuncia = denunciaDAO.buscarDenunciaPorId(id);
        categoria.setText(denuncia.getCategoria());
        usuario.setText(denuncia.getUsuario());
        descricao.setText(denuncia.getDescricao());
        data.setText(denuncia.getData().toString());

        setarFotoOuVideo();
        getLocationManager();



    }
    private void getLocationManager() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET},
                    1);
            return;
        }

        Listener listener = new Listener();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        long tempoAtualizacao = 3600;
        float distancia = 0;
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, tempoAtualizacao, distancia, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, tempoAtualizacao, distancia, listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    getLocationManager();
                } else {
                    Toast.makeText(this, "Sem permissão para uso de gps ou rede.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private class Listener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            String latitudeStr = String.valueOf(location.getLatitude());
            String longitudeStr = String.valueOf(location.getLongitude());


            String url = String.format(urlBase, latitudeStr, longitudeStr);
            mWebView.loadUrl(url);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {    }
        @Override
        public void onProviderEnabled(String provider) {  }
        @Override
        public void onProviderDisabled(String provider) { }
    }
    private File getDiretorioDeSalvamento(String nomeArquivo, int i) {
        File diretorio;
        if (nomeArquivo.contains("/")) {
            int beginIndex = nomeArquivo.lastIndexOf("/") + 1;
            nomeArquivo = nomeArquivo.substring(beginIndex);
        }
        if(nomeArquivo.contains(".jpg")){
            diretorio = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else{
            diretorio = this.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        }
        File pathDaImagem = new File(diretorio, nomeArquivo);
        return pathDaImagem;
    }
    public void setarFotoOuVideo(){
        if(denuncia.getUriMidia().contains(".mp4")) {
            Uri uri = Uri.parse(denuncia.getUriMidia());
            this.videoView.setVideoURI(uri);
            videoView.start();

            //Log.d("teste", "onCreate: " + denuncia.getLatitude());
        }else{
            videoView.setVisibility(View.INVISIBLE);
            uri = Uri.fromFile(getDiretorioDeSalvamento(denuncia.getUriMidia(), 0));
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            int bmpWidth = bitmap.getWidth();
            int bmpHeight = bitmap.getHeight();
            Matrix matrix = new Matrix();
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, true);
            imageView.setImageBitmap(resizedBitmap);

        }


    }

}
