package ufc.qx.q_detective;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class MenuDialogFragment extends DialogFragment  {
    private NotificarEscutadorDoMenuDialog escutador;


    public interface NotificarEscutadorDoMenuDialog {
        public void onDialogVideoClick(int posicao);

        public void onDialogFotoClick(int posicao);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CharSequence[] items = {"Vídeo", "Foto"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Opções").setItems(items, itemClick);
        escutador = (NotificarEscutadorDoMenuDialog) getActivity();
        return builder.create();
    }

    DialogInterface.OnClickListener itemClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int item) {
            int pos = MenuDialogFragment.this.getArguments().getInt("posicao");
            switch (item) {
                case 0:
                    escutador.onDialogVideoClick(pos);
                    break;
                case 1:
                    escutador.onDialogFotoClick(pos);
                    break;
            }
        }
    };
}
