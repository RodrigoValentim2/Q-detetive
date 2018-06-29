package ufc.qx.q_detective;

import android.app.Activity;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import ufc.qx.q_detective.dao.DenunciaDAO;
import ufc.qx.q_detective.dominio.Denuncia;

/**
 * Created by rodrigo on 12/12/17.
 */

public class Teste extends Activity{

    private String urlBase = "http://maps.googleapis.com/maps/api/staticmap" +
            "?size=400x400&sensor=true&markers=color:red|%s,%s&key=AIzaSyCNgjtgzjX7cO-Qy26hSpjdgjuIBGjqE8M";

    private WebView mWebView;
    private Bundle bundle = null;
    private  int id;
    private DenunciaDAO denunciaDAO;
    private Denuncia denuncia;
    private LocationManager locationManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_teste);
    }
}
