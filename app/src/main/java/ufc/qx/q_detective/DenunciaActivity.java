package ufc.qx.q_detective;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ufc.qx.q_detective.dao.DenunciaDAO;
import ufc.qx.q_detective.dominio.Denuncia;

/**
 * Created by mel on 07/12/17.
 */

public class DenunciaActivity extends Activity implements MenuDialogFragment.NotificarEscutadorDoMenuDialog  {
    private static final int CAPTURAR_VIDEO = 1;
    private static final int CAPTURAR_IMAGEM = 1;
    private boolean possuiCartaoSD = false;
    private boolean permisaoInternet = false;
    private final String url = "http://192.168.1.13:8080/QDetective/rest/";

    private ProgressDialog load;
    private  boolean fotoOuvideo;

    private DenunciaDAO denunciaDAO;
    private LocationManager locationManager;
    private String latitude, longitude;
    private Denuncia denuncia;
    private int id;

    private ImageView imageView;
    private VideoView videoView;
    private Uri uri;
    private Date data;
    private EditText descricaoEditText, usuarioEditText;
    private Spinner categoriaSpinner;
    private View v;
    private Bundle bundle = null;
    private String local = null;


    @Override
    protected void onResume() {
        super.onResume();
        bundle = this.getIntent().getExtras();
        if (bundle != null) {
            id = bundle.getInt("id");
            if (id > 0) {
                carregarDados(id);
            }
        }
    }

    public void sairTela(){
        Intent i = new Intent(DenunciaActivity.this, MainActivity.class);
        startActivity(i);
        this.finish();
    }

    public void carregarDados(int id) {
        denuncia = denunciaDAO.buscarDenunciaPorId(id);

        String user = denuncia.getUsuario();
        String descricao = denuncia.getDescricao();
        String categoria = denuncia.getCategoria();

        descricaoEditText.setText(descricao);
        usuarioEditText.setText(user);
        local = denuncia.getUriMidia();

    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.denuncia_layout);

        imageView = (ImageView) findViewById(R.id.imagemView1);
        videoView = (VideoView) findViewById(R.id.videoView3);
        imageView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.INVISIBLE);
        possuiCartaoSD = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);


        this.denunciaDAO = new DenunciaDAO(this);
        id = 0;
        categoriaSpinner = findViewById(R.id.categoria);
        usuarioEditText = findViewById(R.id.usuarioEditText);
        descricaoEditText =(EditText) findViewById(R.id.decricaoEditText);


    }

    public void salvarDenuncia(View view) {

        if (TextUtils.isEmpty(usuarioEditText.getText().toString().trim())) {
           usuarioEditText.setError("Campo obrigatório.");
            return;
        }

        if (TextUtils.isEmpty(descricaoEditText.getText().toString().trim())) {
            descricaoEditText.setError("Campo obrigatório.");
            return;
        }

        String categoria = categoriaSpinner.getSelectedItem().toString();
        String usuario = usuarioEditText.getText().toString();
        String descricao = descricaoEditText.getText().toString();
        this.data = new Date();

        String midia = null;
        Log.d("teste", "salvarDenuncia: "+getDiretorioDeSalvamentoFoto(uri.toString()).toString());

        if (getDiretorioDeSalvamentoFoto(uri.toString()).toString().contains(".mp4")) {
            midia = getDiretorioDeSalvamentoVideo(uri.toString()).toString();

        } else {
            midia = getDiretorioDeSalvamentoFoto(uri.toString()).toString();
        }


        if (id > 0) {
            denuncia = new Denuncia(id, descricao, data, Double.parseDouble(latitude),Double.parseDouble(longitude), midia, usuario, categoria);
            denunciaDAO.atualizarDenuncia(denuncia);
            Toast.makeText(this, "Denuncia Cadastrada com sucesso.", Toast.LENGTH_LONG).show();
        }
        else {
            denuncia = new Denuncia(descricao, data, Double.parseDouble(latitude),Double.parseDouble(longitude), midia, usuario, categoria);
            denunciaDAO.inserirDenuncia(denuncia);
            Toast.makeText(this, "Denuncia Cadastrada com sucesso.", Toast.LENGTH_LONG).show();
        }

        finish();
    }

    public void escloherOpc(View v){
        MenuDialogFragment fragmento = new MenuDialogFragment();
        Bundle bundle = new Bundle();
        fragmento.setArguments(bundle);
        fragmento.show(this.getFragmentManager(), "menuDialog");
        getLocationManager();

    }

    @Override
    public void onDialogVideoClick(int posicao) {
        imageView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.VISIBLE);
       capturarVideo(v);


    }

    @Override
    public void onDialogFotoClick(int posicao) {
        imageView.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.INVISIBLE);
        Log.d("verifica2", "onDialogVideoClick: "+ fotoOuvideo);
        capturarFoto(v);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURAR_VIDEO || requestCode == CAPTURAR_IMAGEM) {
            if (resultCode == RESULT_OK) {
                String msg = "Salva com sucesso! ";
               Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Não foi Salvo!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void visualizarVideo(View v) {
        videoView.setVideoURI(uri);
        MediaController mc = new MediaController(this);
        videoView.setMediaController(mc);
        videoView.start();
    }

    public void capturarVideo(View v) {
        getPermissoesVideo();
    }

    private void capturarFoto(View v) {
        getPermissoesFoto();
    }




    private void getPermissoesVideo() {
        String CAMERA = Manifest.permission.CAMERA;
        String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
      /*  String FINE =  Manifest.permission.ACCESS_FINE_LOCATION;
        String COARSE =   Manifest.permission.ACCESS_COARSE_LOCATION;
        String INTERNET =       Manifest.permission.INTERNET;*/
        int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;

        boolean permissaoCamera = ActivityCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED;
        boolean permissaoEscrita = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        boolean permissaoLeitura = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED;

        if (permissaoCamera && permissaoEscrita && permissaoLeitura) {
            iniciarGravacaoDeVideo();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void getPermissoesFoto() {
        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean leitura = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean escrita = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (camera && leitura && escrita) {
            iniciarCapturaDeFotos();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED
                       ){
                    iniciarGravacaoDeVideo();
                } else {
                    Toast.makeText(this, "Sem permissão para uso de câmera.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case 2: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if (isOnline()) {
                        permisaoInternet = true;
                        return;
                    } else {
                        permisaoInternet = false;
                        Toast.makeText(this, "Sem conexão de Internet.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    permisaoInternet = false;
                    Toast.makeText(this, "Sem permissão para uso de Internet.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void iniciarGravacaoDeVideo() {
        try {
            setArquivoVideo();
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
            startActivityForResult(intent, CAPTURAR_VIDEO);
        } catch (Exception e) {

        }
    }

    private void iniciarCapturaDeFotos() {
        try {

            setArquivoImagem();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, CAPTURAR_IMAGEM);
        } catch (Exception e) {

        }
    }


    private void setArquivoVideo() {

        File diretorio = this.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File pathVideo = new File(diretorio + "/" + System.currentTimeMillis() + ".mp4");

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            String authority = this.getApplicationContext().getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(this, authority, pathVideo);
        } else {
            uri = Uri.fromFile(pathVideo);
        }
    }

    private void setArquivoImagem() {
        File diretorio = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File pathFoto = new File(diretorio + "/" + System.currentTimeMillis() + ".jpg");

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            String authority = this.getApplicationContext().getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(this, authority, pathFoto);
        } else {
            uri = Uri.fromFile(pathFoto);
        }
    }

    private File getDiretorioDeSalvamentoVideo(String nomeArquivo) {
        if (nomeArquivo.contains("/")) {
            int beginIndex = nomeArquivo.lastIndexOf("/") + 1;
            nomeArquivo = nomeArquivo.substring(beginIndex);
        }
        File diretorio = this.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File pathDaImagem = new File(diretorio, nomeArquivo);
        return pathDaImagem;
    }

    private File getDiretorioDeSalvamentoFoto(String nomeArquivo) {
        if (nomeArquivo.contains("/")) {
            int beginIndex = nomeArquivo.lastIndexOf("/") + 1;
            nomeArquivo = nomeArquivo.substring(beginIndex);
        }
        File diretorio = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File pathDaImagem = new File(diretorio, nomeArquivo);
        return pathDaImagem;
    }



    private void getPermissaoDaInternet() {
        boolean internet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        boolean redeStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
        if (internet && redeStatus) {
            if (isOnline()) {
                permisaoInternet = true;
                return;
            } else {
                permisaoInternet = false;
                Toast.makeText(this, "Sem conexão de Internet.", Toast.LENGTH_LONG).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE},
                    1);
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    class UploadDenuncia extends AsyncTask<Denuncia, Void, WebServiceUtils> {

        @Override
        protected void onPreExecute() {
            load = ProgressDialog.show(DenunciaActivity.this, "Por favor Aguarde ...", "Recuperando Informações do Servidor...");
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected WebServiceUtils doInBackground(Denuncia... denuncias) {
            WebServiceUtils webService = new WebServiceUtils();
            Denuncia denuncia = denuncias[0];
            String urlDados = url + "denuncias";
            if (webService.sendDenunciaJson(urlDados, denuncia)) {
                urlDados = url + "arquivos/postFotoBase64";
                webService.uploadImagemBase64(urlDados, new File(denuncia.getUriMidia()));
            }
            return webService;
        }
        @Override
        protected void onPostExecute(WebServiceUtils webService) {
            Toast.makeText(getApplicationContext(),
                    webService.getRespostaServidor(),
                    Toast.LENGTH_LONG).show();
            load.dismiss();
        }
    }

    public void realizarUpload(View view) {

        if (TextUtils.isEmpty(descricaoEditText.getText().toString().trim())) {
            descricaoEditText.setError("Campo obrigatório.");
            return;
        }

        if (TextUtils.isEmpty(usuarioEditText.getText().toString().trim())) {
            usuarioEditText.setError("Campo obrigatório.");
            return;
        }

        getPermissaoDaInternet();
        if (permisaoInternet) {
            Denuncia denuncia = new Denuncia();
            denuncia.setId(0);
            denuncia.setData(Calendar.getInstance().getTime());
            denuncia.setDescricao(descricaoEditText.getText().toString());
            denuncia.setUsuario(usuarioEditText.getText().toString());
            denuncia.setLatitude(Double.parseDouble(latitude));
            denuncia.setLongitude(Double.parseDouble(longitude));
            denuncia.setCategoria(categoriaSpinner.getSelectedItem().toString());
            if(fotoOuvideo) {
                denuncia.setUriMidia(getDiretorioDeSalvamentoFoto(uri.getPath()).getPath());
            }
            denuncia.setUriMidia(getDiretorioDeSalvamentoVideo(uri.getPath()).getPath());
            UploadDenuncia upload = new UploadDenuncia();
            upload.execute(denuncia);

        }
    }

    private void getLocationManager() {
        Listener listener = new Listener();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        long tempoAtualizacao = 90;
        float distancia = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET},
                    1);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, tempoAtualizacao, distancia, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, tempoAtualizacao, distancia, listener);
    }

    private class Listener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());


        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }
}
