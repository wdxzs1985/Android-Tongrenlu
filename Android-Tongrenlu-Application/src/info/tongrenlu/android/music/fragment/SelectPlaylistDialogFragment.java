package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SelectPlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private SelectPlaylistDialogFragmentListener mListener = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SelectPlaylistDialogFragmentListener) {
            this.mListener = (SelectPlaylistDialogFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Cursor cursor = this.getActivity()
                                  .getContentResolver()
                                  .query(TongrenluContentProvider.PLAYLIST_URI,
                                         null,
                                         null,
                                         null,
                                         null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle(R.string.title_select_playlist)
               .setCursor(cursor, this, "title")
               .setPositiveButton(R.string.action_new_playlist, this)
               .setNegativeButton(R.string.action_cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        Bundle extras = new Bundle(this.getArguments());
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            this.mListener.onShowCreatePlaylistDialogFragment(extras);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            final AlertDialog alertDialog = (AlertDialog) dialog;
            final long playlistId = alertDialog.getListView()
                                               .getItemIdAtPosition(which);
            extras.putLong("playlistId", playlistId);
            this.mListener.onSelectPlaylist(extras);
            break;
        }

    }

    public interface SelectPlaylistDialogFragmentListener {

        void onSelectPlaylist(Bundle args);

        void onShowCreatePlaylistDialogFragment(Bundle args);
    }
}