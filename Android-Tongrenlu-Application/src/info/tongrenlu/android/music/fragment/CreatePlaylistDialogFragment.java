package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class CreatePlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private EditText mTitleView = null;

    private CreatePlaylistDialogFragmentListener mListener = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CreatePlaylistDialogFragmentListener) {
            this.mListener = (CreatePlaylistDialogFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final LayoutInflater inflater = this.getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_create_playlist,
                                           null);

        this.mTitleView = (EditText) view.findViewById(R.id.playlist_title);
        this.mTitleView.setText(this.getArguments().getString("title"));

        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle(R.string.title_new_playlist)
               .setView(view)
               .setPositiveButton(R.string.action_create, this)
               .setNegativeButton(R.string.action_cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        Bundle extras = new Bundle(this.getArguments());
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            final String title = this.mTitleView.getText().toString();
            extras.putString("title", title);
            this.mListener.onCreatePlaylist(extras);
            break;
        default:
            break;
        }

    }

    public interface CreatePlaylistDialogFragmentListener {

        void onCreatePlaylist(Bundle extras);

    }

}
